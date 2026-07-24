package com.hwnix.smsagent.data.local

import android.content.Context
import android.content.Intent
import android.os.Build
import com.hwnix.smsagent.data.service.AgentForegroundService
import android.telephony.SmsManager
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hwnix.smsagent.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.UUID

class SyncEngine(private val context: Context) {

    private val sessionManager by lazy { com.hwnix.smsagent.core.di.ServiceLocator.sessionManager }
    private val database by lazy { com.hwnix.smsagent.core.di.ServiceLocator.database }
    private val smsDao by lazy { database.smsDao() }
    private val apiService by lazy { com.hwnix.smsagent.core.di.ServiceLocator.apiService }
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "SyncEngine"
    }

    /**
     * تشغيل المزامنة الشاملة (الرسائل المعلقة والأوامر والنبضات) بشكل خيطي آمن.
     */
    private fun logAndTrace(stage: String) {
        Log.i(TAG, stage)
        BootTracker.updateStage(context, stage)
        sendRemoteLog("TRACE_SYNC", stage)
    }

    suspend fun performFullSync() {
        if (sessionManager.getAuthToken() == null) {
            logAndTrace("TRACE_SYNC: Aborted (no auth token)")
            return
        }
        
        syncMutex.withLock {
            logAndTrace("TRACE_SYNC_01: Enter Sync()")
            
            try {
                // التأكد من تسجيل الجهاز على السيرفر أولاً
                if (sessionManager.getDeviceId() == -1L) {
                    logAndTrace("TRACE_SYNC_02: Registering device...")
                    val registered = registerDeviceSync()
                    if (!registered) {
                        Log.e(TAG, "Sync aborted: Device not registered on server.")
                        logAndTrace("TRACE_SYNC_ABORT: Device registration failed")
                        return
                    }
                }
                logAndTrace("TRACE_SYNC_02: Device registration verified")
                
                // 0. تفريغ أي رسائل واردة مسجلة في وضع Direct Boot أثناء قفل الشاشة
                flushDirectBootSmsQueue()

                // 1. فحص صندوق الوارد بالنظام لمزامنة الرسائل الواردة المتراكمة (أثناء توقف التطبيق أو انقطاع الشبكة)
                logAndTrace("TRACE_SYNC_03: Scanning inbox...")
                val scanSuccess = runWithBackoff { scanSystemInboxForNewMessages() } ?: false
                logAndTrace("TRACE_SYNC_04: Inbox scan finished ($scanSuccess)")

                // 2. رفع الرسائل الواردة المتراكمة محلياً
                logAndTrace("TRACE_SYNC_05: Uploading pending incoming SMS...")
                val uploadSuccess = runWithBackoff { uploadPendingIncomingSms() } ?: false
                logAndTrace("TRACE_SYNC_06: Upload finished ($uploadSuccess)")
                
                // 2. سحب ومعالجة الأوامر المعلقة من السيرفر
                logAndTrace("TRACE_SYNC_07: Pulling server commands...")
                val pullSuccess = runWithBackoff { pullAndProcessCommands() } ?: false
                logAndTrace("TRACE_SYNC_08: Commands processed ($pullSuccess)")
                
                // 3. إرسال نبضة قلب لتحديث التواجد وسحب الإعدادات
                logAndTrace("TRACE_SYNC_09: Sending heartbeat...")
                val heartbeatSuccess = runWithBackoff { sendHeartbeat() } ?: false
                
                if (heartbeatSuccess) {
                    sessionManager.saveLastSyncSuccessTime(System.currentTimeMillis())
                    logAndTrace("TRACE_SYNC_10: Sync finished successfully")
                } else {
                    logAndTrace("TRACE_SYNC_10: Heartbeat failed, sync incomplete")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
                BootTracker.logException(context, "SyncEngine.performFullSync", e)
                sendRemoteLog("TRACE_SYNC_ERROR", "Sync error: ${e.message}")
            }
        }
    }

    private fun hasActiveSession(): Boolean {
        return sessionManager.getAuthToken() != null
    }

    /**
     * مزامنة أرقام الشرائح (SIM lines) مع السيرفر — يُستدعى من الـ UI بعد إدخال المستخدم للأرقام.
     * كل عنصر في القائمة يحتوي على: slot_index, subscription_id, carrier, phone_number
     */
    suspend fun syncSimLines(lines: List<Map<String, String>>): String? = withContext(Dispatchers.IO) {
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) {
            Log.e(TAG, "syncSimLines: no deviceId saved, aborting.")
            return@withContext "لم يتم العثور على معرف الجهاز محلياً. يرجى تسجيل الخروج والولوج مجدداً."
        }

        val simsArray = JsonArray()
        lines.forEach { line ->
            val obj = JsonObject().apply {
                addProperty("slot_index", line["slot_index"]?.toIntOrNull() ?: 0)
                addProperty("subscription_id", line["subscription_id"] ?: "-1")
                addProperty("carrier", line["carrier"] ?: "Unknown")
                addProperty("phone_number", line["phone_number"] ?: "")
                addProperty("mcc", line["mcc"] ?: "")
                addProperty("mnc", line["mnc"] ?: "")
            }
            simsArray.add(obj)
        }

        val payload = JsonObject().apply {
            addProperty("device_id", deviceId)
            add("sims", simsArray)
        }

        Log.d(TAG, "syncSimLines payload: $payload")

        return@withContext try {
            val response = apiService.syncLines(
                idempotencyKey = "SIM_SYNC_${deviceId}_${UUID.randomUUID()}",
                body = payload
            )
            Log.d(TAG, "syncSimLines response: ${response.code()} - ${response.body()}")
            if (response.isSuccessful) {
                null
            } else {
                val errorBody = response.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    try {
                        val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                        json.get("message")?.asString ?: "خطأ غير معروف من السيرفر: ${response.code()}"
                    } catch (e: Exception) {
                        "خطأ من السيرفر (${response.code()}): $errorBody"
                    }
                } else {
                    "خطأ من السيرفر (${response.code()})"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncSimLines failed: ${e.message}", e)
            "فشل الاتصال: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * جلب أرقام الخطوط المسجلة حالياً على السيرفر لهذا الجهاز.
     * يعود بـ Map يربط slot_index بزوج يحتوي على (اسم المشغل، رقم الهاتف).
     */
    suspend fun getSavedSimLines(): Map<Int, Pair<String, String>> = withContext(Dispatchers.IO) {
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) return@withContext emptyMap()

        try {
            val response = apiService.getDeviceLines(deviceId)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                if (responseBody.get("status")?.asBoolean == true) {
                    val data = responseBody.getAsJsonArray("data")
                    val result = mutableMapOf<Int, Pair<String, String>>()
                    data.forEach { element ->
                        val lineObj = element.asJsonObject
                        val slotIndex = lineObj.get("slot_index").asInt
                        val carrierElem = lineObj.get("carrier")
                        val phoneElem = lineObj.get("phone_number")
                        val carrier = if (carrierElem != null && !carrierElem.isJsonNull) carrierElem.asString else ""
                        val phoneNumber = if (phoneElem != null && !phoneElem.isJsonNull) phoneElem.asString else ""
                        result[slotIndex] = Pair(carrier, phoneNumber)
                    }
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSavedSimLines failed: ${e.message}")
        }
        return@withContext emptyMap()
    }

    /**
     * إرسال نبضة قلب الهاتف وتلقي أي تغييرات للإعدادات وسياسات التحديث.
     */
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) return@withContext false

        val batteryLevel = try {
            val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) { 100 }

        val networkType = try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cellular"
                else -> "unknown"
            }
        } catch (e: Exception) { "unknown" }

        val payload = JsonObject().apply {
            addProperty("device_id", deviceId)
            addProperty("network_type", networkType)
            addProperty("battery_level", batteryLevel)
            addProperty("is_internet_available", networkType != "unknown")
            addProperty("free_memory_bytes", Runtime.getRuntime().freeMemory())
            addProperty("free_storage_bytes", context.filesDir.usableSpace)
            addProperty("app_version", "1.0.11")
            addProperty("configuration_version", sessionManager.getConfigVersion())
        }

        try {
            val response = apiService.sendHeartbeat(payload)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                if (responseBody.get("status")?.asBoolean == true) {
                    val data = responseBody.getAsJsonObject("data")
                    
                    // تحديث الإعدادات إذا تم الإشعار بذلك
                    if (data.get("settings_updated")?.asBoolean == true) {
                        val config = data.getAsJsonObject("config")
                        sessionManager.saveConfigVersion(config.get("configuration_version").asInt)
                        sessionManager.savePollingInterval(config.get("polling_interval_seconds").asInt)
                        sessionManager.saveLoggingLevel(config.get("logging_level").asString)
                        sessionManager.saveMaxRetry(config.get("max_retry_count").asInt)
                        Log.i(TAG, "Device settings updated to version: ${sessionManager.getConfigVersion()}")
                    }
                    return@withContext true
                }
            } else {
                if (handleDeviceVerification(response.code())) {
                    return@withContext sendHeartbeat()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat failed: ${e.message}")
        }
        return@withContext false
    }

    /**
     * تفريغ أي رسائل واردة سُجلت كملفات JSON في Device Protected Storage أثناء قفل الشاشة
     * ونقلها لقاعدة البيانات المحلية بوضع pending_upload لرفعها للسيرفر فوراً.
     */
    private suspend fun flushDirectBootSmsQueue(): Unit = withContext(Dispatchers.IO) {
        val deviceProtectedContext: Context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        try {
            val directory = java.io.File(deviceProtectedContext.filesDir, "direct_boot_sms")
            if (!directory.exists() || !directory.isDirectory) return@withContext
            val files = directory.listFiles() ?: return@withContext
            if (files.isEmpty()) return@withContext

            Log.i(TAG, "Found ${files.size} queued SMS from Direct Boot mode. Importing to Room DB...")
            files.forEach { file ->
                try {
                    val content = file.readText()
                    val json = com.google.gson.JsonParser.parseString(content).asJsonObject
                    val phoneNumber = json.get("phoneNumber").asString
                    val messageBody = json.get("messageBody").asString
                    val sentAt = json.get("sentAt").asLong
                    val subscriptionId = json.get("subscriptionId").asString
                    val androidSmsId = if (json.has("androidSmsId")) json.get("androidSmsId").asString else null

                    val request = com.hwnix.smsagent.data.local.SmsImportRequest(
                        phoneNumber = phoneNumber,
                        messageBody = messageBody,
                        subscriptionId = subscriptionId,
                        sentAt = sentAt,
                        source = "DirectBootQueue",
                        androidSmsId = androidSmsId
                    )

                    com.hwnix.smsagent.data.local.SmsImportManager.importMessage(context, request)
                    file.delete()
                    Log.i(TAG, "Processed Direct Boot SMS file: ${file.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing Direct Boot SMS file: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing Direct Boot SMS queue: ${e.message}")
        }
    }

    /**
     * فحص صندوق الوارد بالنظام (System Inbox) ورفع أي رسائل واردة جديدة لم تسجل بعد.
     * هذا يضمن عدم ضياع أي رسالة واردة في حال إيقاف التطبيق الإجباري (Force Stop) أو انقطاع الإنترنت.
     */
    private suspend fun scanSystemInboxForNewMessages(): Boolean = withContext(Dispatchers.IO) {
        val lastCheckTime = sessionManager.getLastIncomingSmsCheckTime()
        val currentTime = System.currentTimeMillis()
        
        // إذا كانت أول مرة، نفحص آخر ساعتين فقط لمنع رفع تاريخ الرسائل بالكامل
        val sinceTime = if (lastCheckTime == 0L) currentTime - (2 * 3600 * 1000) else lastCheckTime
        
        Log.i(TAG, "Scanning system inbox for new incoming SMS since: $sinceTime")
        sendRemoteLog("TRACE_INBOX_SCAN", "Scanning system inbox since: $sinceTime")
        
        try {
            val inboxUri = android.net.Uri.parse("content://sms/inbox")
            context.contentResolver.query(
                inboxUri,
                null, // استخدام null لتجنب استثناء العمود غير الصالح على الهواتف المختلفة
                "date > ?",
                arrayOf(sinceTime.toString()),
                "date ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")
                val dateSentIdx = cursor.getColumnIndex("date_sent")
                val subIdIdx = cursor.getColumnIndex("sub_id")
                val simIdIdx = cursor.getColumnIndex("sim_id")
                
                var foundCount = 0
                var insertedCount = 0

                if (addressIdx >= 0 && bodyIdx >= 0 && dateIdx >= 0) {
                    while (cursor.moveToNext()) {
                        val address = cursor.getString(addressIdx) ?: "unknown"
                        val body = cursor.getString(bodyIdx) ?: ""
                        val date = cursor.getLong(dateIdx)
                        val dateSent = if (dateSentIdx >= 0) cursor.getLong(dateSentIdx) else null
                        val smsId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        
                        val subId = if (subIdIdx >= 0) {
                            cursor.getString(subIdIdx) ?: "-1"
                        } else if (simIdIdx >= 0) {
                            cursor.getString(simIdIdx) ?: "-1"
                        } else {
                            "-1"
                        }
                        
                        foundCount++

                        val request = com.hwnix.smsagent.data.local.SmsImportRequest(
                            phoneNumber = address,
                            messageBody = body,
                            subscriptionId = subId,
                            sentAt = date,
                            source = "InboxScan",
                            androidSmsId = smsId,
                            dateSentTimestamp = dateSent,
                            dateTimestamp = date
                        )

                        val imported = com.hwnix.smsagent.data.local.SmsImportManager.importMessage(context, request)
                        if (imported) {
                            insertedCount++
                        }
                    }
                }
                Log.i(TAG, "Inbox scan finished. Found: $foundCount, Unsynced: $insertedCount")
                sendRemoteLog("TRACE_INBOX_SCAN", "Inbox scan finished. Found: $foundCount, Unsynced: $insertedCount")
                BootTracker.updateStage(context, "TRACE_SYNC: Inbox scan details: found $foundCount, unsynced $insertedCount")
            }
            sessionManager.saveLastIncomingSmsCheckTime(currentTime)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan system inbox: ${e.message}", e)
            sendRemoteLog("TRACE_INBOX_SCAN", "Failed to scan system inbox: ${e.message}")
            return@withContext false
        }
    }

    /**
     * رفع الرسائل الواردة المخزنة محلياً بنظام الدفعات والمطابقة (Batch upload).
     */
    private suspend fun uploadPendingIncomingSms(): Boolean = withContext(Dispatchers.IO) {
        val pending = smsDao.getPendingUploads()
        if (pending.isEmpty()) {
            Log.i(TAG, "No pending SMS to upload.")
            BootTracker.updateStage(context, "TRACE_SYNC: No pending SMS to upload")
            return@withContext true
        }

        Log.i(TAG, "Found ${pending.size} pending SMS to upload.")
        BootTracker.updateStage(context, "TRACE_SYNC: Found ${pending.size} pending SMS to upload")

        val deviceId = sessionManager.getDeviceId()
        val array = JsonArray()

        pending.forEach { sms ->
            // تحويل الـ timestamp من Long millis إلى ISO 8601
            val sentAtIso = try {
                val date = java.util.Date(sms.sentAt)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.format(date)
            } catch (e: Exception) {
                null
            }

            val obj = JsonObject().apply {
                addProperty("subscription_id", sms.subscriptionId)
                addProperty("phone_number", sms.phoneNumber)
                addProperty("message_body", sms.messageBody)
                addProperty("message_ref", sms.messageRef)
                if (sentAtIso != null) addProperty("sent_at", sentAtIso)
            }

            Log.d(TAG, "Preparing SMS for upload: ref=${sms.messageRef}, from=${sms.phoneNumber}, sub=${sms.subscriptionId}, sentAt=$sentAtIso")
            array.add(obj)
        }

        val payload = JsonObject().apply {
            addProperty("device_id", deviceId)
            add("messages", array)
        }

        Log.d(TAG, "Sending batchSync payload: $payload")

        // استخدام UUID فريد للدورة كـ Idempotency-Key
        val idempotencyKey = "BATCH_SYNC_" + System.currentTimeMillis()

        try {
            val response = apiService.batchSyncSms(idempotencyKey, payload)
            Log.d(TAG, "batchSync response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "batchSync response body: $body")
                if (body.get("status")?.asBoolean == true) {
                    // تحديث الرسائل محلياً إلى uploaded
                    pending.forEach { sms ->
                        smsDao.update(sms.copy(status = "uploaded"))
                    }
                    Log.i(TAG, "Synced ${pending.size} incoming messages successfully.")
                    return@withContext true
                }
            } else {
                if (handleDeviceVerification(response.code())) {
                    return@withContext uploadPendingIncomingSms()
                }
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "batchSync failed - code: ${response.code()}, error: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch sync upload failed: ${e.message}", e)
        }
        return@withContext false
    }

    /**
     * جلب ومعالجة الأوامر المعلقة (كإرسال الرسائل).
     */
    suspend fun pullAndProcessCommands(): Boolean = withContext(Dispatchers.IO) {
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) return@withContext false

        try {
            val response = apiService.getPendingCommands(deviceId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    val commands = body.getAsJsonArray("data")
                    val cmdIds = commands.map { it.asJsonObject.get("id").asLong }
                    Log.i(TAG, "TRACE 1: Poll response received at ${System.currentTimeMillis()}, count=${commands.size()}, ids=$cmdIds")
                    sendRemoteLog("TRACE 1", "Poll response received, count=${commands.size()}, ids=$cmdIds")

                    commands.forEach { element ->
                        val cmdObj = element.asJsonObject
                        val commandId = cmdObj.get("id").asLong
                        val type = cmdObj.get("command_type").asString
                        val payload = cmdObj.getAsJsonObject("payload")

                        Log.i(TAG, "TRACE 2: PROCESS commandId=$commandId of type: $type")
                        sendRemoteLog("TRACE 2", "PROCESS commandId=$commandId of type: $type", commandId)
                        
                        if (type == "SEND_SMS") {
                            executeSmsSendCommand(commandId, payload)
                        } else {
                            // تنفيذ أوامر إدارية أخرى (تحديث الإعدادات، إلخ)
                            reportCommandStatus(commandId, "executed", JsonObject())
                        }
                    }
                    return@withContext true
                }
            } else {
                if (handleDeviceVerification(response.code())) {
                    return@withContext pullAndProcessCommands()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull commands: ${e.message}")
        }
        return@withContext false
    }

    /**
     * تنظيف رقم الهاتف: حذف مفتاح الدولة (+20) وعلامة + للحصول على الصيغة المحلية.
     */
    private fun cleanPhoneNumber(phone: String): String {
        var cleaned = phone.trim()
        if (cleaned.startsWith("+20")) cleaned = "0" + cleaned.removePrefix("+20")
        else if (cleaned.startsWith("+")) cleaned = "0" + cleaned.removePrefix("+")
        return cleaned
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
            if (subManager != null) {
                val infoList = subManager.activeSubscriptionInfoList
                if (infoList != null) {
                    for (info in infoList) {
                        if (info.simSlotIndex == slotIndex) {
                            return info.subscriptionId
                        }
                    }
                }
            }
        }
        return -1
    }

    /**
     * تنفيذ أمر إرسال رسالة SMS مع تتبع حالة التسليم الفعلي.
     */
    private fun executeSmsSendCommand(commandId: Long, payload: JsonObject) {
        val messageId = payload.get("message_id").asLong
        val rawPhone = payload.get("phone_number").asString
        val phoneNumber = cleanPhoneNumber(rawPhone)
        val messageBody = payload.get("message_body").asString
        val slotIndex = payload.get("slot_index").asInt
        val subId = getSubscriptionIdForSlot(slotIndex)

        try {
            val smsManager = if (subId != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            // إعداد SentIntent لمعرفة نتيجة الإرسال للشبكة
            Log.i(TAG, "TRACE 1.1: BEFORE creating PendingIntent for SMS_SENT, cmd: $commandId, msg: $messageId")
            sendRemoteLog("TRACE 1.1", "BEFORE creating PendingIntent for SMS_SENT, cmd: $commandId, msg: $messageId", commandId, messageId)
            val sentIntent = android.app.PendingIntent.getBroadcast(
                context,
                commandId.toInt(),
                android.content.Intent("com.hwnix.smsagent.SMS_SENT").apply {
                    setPackage(context.packageName)
                    putExtra("command_id", commandId)
                    putExtra("message_id", messageId)
                    putExtra("phone_number", phoneNumber)
                    putExtra("message_body", messageBody)
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            Log.i(TAG, "TRACE 1.2: AFTER creating PendingIntent for SMS_SENT, cmd: $commandId, msg: $messageId")
            sendRemoteLog("TRACE 1.2", "AFTER creating PendingIntent for SMS_SENT, cmd: $commandId, msg: $messageId", commandId, messageId)

            // إعداد DeliveryIntent لمعرفة التسليم الفعلي للمستقبِل
            val deliveryIntent = android.app.PendingIntent.getBroadcast(
                context,
                (commandId + 10000).toInt(),
                android.content.Intent("com.hwnix.smsagent.SMS_DELIVERED").apply {
                    setPackage(context.packageName)
                    putExtra("command_id", commandId)
                    putExtra("message_id", messageId)
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            // إرسال الرسالة مع الـ intents
            Log.i(TAG, "TRACE 1.3: BEFORE calling sendTextMessage(), cmd: $commandId, msg: $messageId, phone: $phoneNumber")
            sendRemoteLog("TRACE 1.3", "BEFORE calling sendTextMessage(), cmd: $commandId, msg: $messageId, phone: $phoneNumber", commandId, messageId)
            smsManager.sendTextMessage(phoneNumber, null, messageBody, sentIntent, deliveryIntent)
            Log.i(TAG, "TRACE 1.4: AFTER sendTextMessage() returned successfully (no exception), cmd: $commandId, msg: $messageId")
            sendRemoteLog("TRACE 1.4", "AFTER sendTextMessage() returned successfully (no exception), cmd: $commandId, msg: $messageId", commandId, messageId)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            val responseObj = JsonObject().apply {
                addProperty("message_id", messageId)
                addProperty("error", e.message ?: "Unknown hardware sending error")
            }
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                reportCommandStatus(commandId, "failed", responseObj)
            }
        }
    }

    /**
     * إبلاغ السيرفر بنتيجة تنفيذ الأمر مع الـ Idempotency-Key.
     */
    private suspend fun reportCommandStatus(commandId: Long, status: String, responsePayload: JsonObject) {
        val payload = JsonObject().apply {
            addProperty("device_id", sessionManager.getDeviceId())
            addProperty("status", status)
            add("response_payload", responsePayload)
        }

        val idempotencyKey = "CMD_EXEC_REP_" + commandId + "_" + status
        try {
            apiService.executeCommand(commandId, idempotencyKey, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report command status: ${e.message}")
        }
    }

    /**
     * محرك الـ Exponential Backoff لإعادة المحاولات المتدرجة الفاشلة.
     */
    private suspend fun <T> runWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 2000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelayMs
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed: ${e.message}. Retrying in ${currentDelay}ms...")
                if (attempt == maxAttempts) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong() + (Math.random() * 500).toLong() // إضافة Jitter عشوائي
            }
        }
        return null
    }

    private suspend fun registerDeviceSync(): Boolean {
        return try {
            val customName = sessionManager.getGatewayName()
            val devName = if (customName.isNotBlank()) customName else Build.MODEL

            val rawId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            val hardwareId = if (rawId.isNullOrBlank() || rawId == "9774d56d682e549c") {
                sessionManager.getDeviceUuid()
            } else {
                rawId
            }

            val payload = JsonObject().apply {
                addProperty("android_id", hardwareId)
                addProperty("uuid", sessionManager.getDeviceUuid())
                addProperty("device_name", devName)
                addProperty("brand", Build.BRAND)
                addProperty("model", Build.MODEL)
                addProperty("android_version", Build.VERSION.RELEASE)
                addProperty("app_version", com.hwnix.smsagent.BuildConfig.VERSION_NAME)
            }

            val key = UUID.randomUUID().toString()
            val response = apiService.registerDevice(key, payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    val data = body.getAsJsonObject("data")
                    val deviceId = data.get("device_id").asLong
                    sessionManager.saveDeviceId(deviceId)
                    Log.i(TAG, "Device auto-re-registered successfully. New ID: $deviceId")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-re-register device: ${e.message}")
            false
        }
    }

    private suspend fun handleDeviceVerification(responseCode: Int): Boolean {
        if (responseCode == 403) {
            Log.e(TAG, "Device explicitly decoupled by user from admin panel. Logging out...")
            sessionManager.clearSession()
            try {
                val serviceIntent = Intent(context, AgentForegroundService::class.java)
                context.stopService(serviceIntent)
            } catch (e: Exception) { /* ignore */ }
            return false
        }
        if (responseCode == 422 || responseCode == 404) {
            Log.w(TAG, "Device ID not found or invalid on server. Re-registering...")
            sessionManager.saveDeviceId(-1L)
            return registerDeviceSync()
        }
        return false
    }

    private fun sendRemoteLog(tag: String, message: String, commandId: Long = -1L, messageId: Long = -1L) {
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JsonObject().apply {
                    addProperty("device_id", deviceId)
                    addProperty("tag", tag)
                    addProperty("message", message)
                    val detailsObj = JsonObject().apply {
                        addProperty("client_timestamp", System.currentTimeMillis())
                        addProperty("thread", Thread.currentThread().name)
                        if (commandId != -1L) addProperty("command_id", commandId)
                        if (messageId != -1L) addProperty("message_id", messageId)
                    }
                    add("details", detailsObj)
                }
                apiService.logDiagnostic(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send remote log: ${e.message}")
            }
        }
    }
}
