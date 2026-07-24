package com.hwnix.smsagent.data.local

import android.content.Context
import android.util.Log
import com.hwnix.smsagent.core.di.ServiceLocator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID

data class SmsImportRequest(
    val phoneNumber: String,
    val messageBody: String,
    val subscriptionId: String,
    val sentAt: Long,
    val source: String, // "SmsReceiver", "InboxScan", "DirectBootQueue", "RecoveryScan"
    val androidSmsId: String? = null,
    val pduTimestamp: Long? = null,
    val dateSentTimestamp: Long? = null,
    val dateTimestamp: Long? = null
)

/* تعليق عربي مختصر: مدير استيراد الرسائل الموحد لمنع تكرار الرسائل وتتبع مصادرها بدقة بالغة مع قفل التزامن و Trace ID */
object SmsImportManager {

    private const val TAG = "SmsImportManager"
    private val importMutex = Mutex()

    fun determineSentAt(pduTimestamp: Long?, dateSent: Long?, date: Long?, fallbackSentAt: Long): Long {
        val selected = when {
            pduTimestamp != null && pduTimestamp > 0 -> pduTimestamp
            dateSent != null && dateSent > 0 -> dateSent
            date != null && date > 0 -> date
            else -> fallbackSentAt
        }
        return selected
    }

    fun generateIdempotencyKey(phoneNumber: String, sentAt: Long, subscriptionId: String, messageBody: String): String {
        val normalizedAddress = phoneNumber.replace("[^0-9]".toRegex(), "")
            .let { if (it.length > 9) it.takeLast(9) else it }
        // تجميع نافذة زمنية مرنة بمقدار 3 ثوانٍ لمعادلة أدنى فروق أجزاء الثانية بين الـ PDU ونظام أندرويد
        val timeBucket = (sentAt / 3000) * 3000
        val bodyHash = sha256(messageBody.trim())
        return "key_${normalizedAddress}_${timeBucket}_${subscriptionId}_$bodyHash"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(4).joinToString("") { "%02x".format(it) }
    }

    suspend fun importMessage(context: Context, request: SmsImportRequest): Boolean = importMutex.withLock {
        val traceId = "TRC-${UUID.randomUUID().toString().take(6).uppercase()}"

        val resolvedSentAt = determineSentAt(
            request.pduTimestamp,
            request.dateSentTimestamp,
            request.dateTimestamp,
            request.sentAt
        )
        val finalRequest = request.copy(sentAt = resolvedSentAt)

        val idempotencyKey = generateIdempotencyKey(
            finalRequest.phoneNumber,
            finalRequest.sentAt,
            finalRequest.subscriptionId,
            finalRequest.messageBody
        )
        val bodyHash = sha256(finalRequest.messageBody)

        val attemptLog = "[$traceId] IMPORT_ATTEMPT | source=${finalRequest.source} | sysId=${finalRequest.androidSmsId ?: "N/A"} | address=${finalRequest.phoneNumber} | sentAt=${finalRequest.sentAt}"
        Log.i(TAG, attemptLog)
        BootTracker.updateStage(context, attemptLog)

        val keyLog = "[$traceId] KEY_GENERATED | key=$idempotencyKey | hash=$bodyHash"
        Log.i(TAG, keyLog)

        val isUnlocked = androidx.core.os.UserManagerCompat.isUserUnlocked(context)

        if (!isUnlocked) {
            val dbLog = "[$traceId] DIRECT_BOOT_LOCKED | Saving to Device Protected Storage"
            Log.w(TAG, dbLog)
            BootTracker.updateStage(context, dbLog)
            saveToDeviceProtectedStorage(context, finalRequest, idempotencyKey, traceId)
            return false
        }

        try {
            val smsDao = ServiceLocator.database.smsDao()

            val exists = smsDao.existsByIdempotencyKey(idempotencyKey)
            val lookupLog = "[$traceId] DB_LOOKUP | exists=$exists"
            Log.i(TAG, lookupLog)

            if (exists) {
                val skipMessage = "[$traceId] DUPLICATE_SKIPPED | source=${finalRequest.source} | key=$idempotencyKey"
                Log.i(TAG, skipMessage)
                BootTracker.updateStage(context, skipMessage)
                return false
            }

            val smsEntity = SmsEntity(
                phoneNumber = finalRequest.phoneNumber,
                messageBody = finalRequest.messageBody,
                direction = "incoming",
                status = "pending_upload",
                messageRef = UUID.randomUUID().toString(),
                subscriptionId = finalRequest.subscriptionId,
                sentAt = finalRequest.sentAt,
                idempotencyKey = idempotencyKey
            )

            val insertedRowId = smsDao.insert(smsEntity)
            if (insertedRowId > 0) {
                val successMessage = "[$traceId] INSERTED | rowId=$insertedRowId | source=${finalRequest.source} | key=$idempotencyKey"
                Log.i(TAG, successMessage)
                BootTracker.updateStage(context, successMessage)
                return true
            } else {
                val conflictMessage = "[$traceId] DUPLICATE_SKIPPED (DB Conflict) | source=${finalRequest.source} | key=$idempotencyKey"
                Log.w(TAG, conflictMessage)
                BootTracker.updateStage(context, conflictMessage)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$traceId] Failed database write under unlocked state, falling back to file storage: ${e.message}")
            saveToDeviceProtectedStorage(context, finalRequest, idempotencyKey, traceId)
            return false
        }
    }

    private fun saveToDeviceProtectedStorage(context: Context, request: SmsImportRequest, idempotencyKey: String, traceId: String) {
        try {
            val deviceProtectedContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
            val directory = java.io.File(deviceProtectedContext.filesDir, "direct_boot_sms")
            if (!directory.exists()) directory.mkdirs()

            val fileName = "sms_${idempotencyKey}.json"
            val file = java.io.File(directory, fileName)
            if (file.exists()) {
                Log.i(TAG, "[$traceId] File $fileName already exists in Direct Boot storage. Skipping duplicate write.")
                return
            }

            val json = com.google.gson.JsonObject().apply {
                addProperty("phoneNumber", request.phoneNumber)
                addProperty("messageBody", request.messageBody)
                addProperty("subscriptionId", request.subscriptionId)
                addProperty("sentAt", request.sentAt)
                addProperty("idempotencyKey", idempotencyKey)
                addProperty("source", request.source)
                addProperty("androidSmsId", request.androidSmsId ?: "")
                addProperty("traceId", traceId)
            }

            file.writeText(json.toString())
            Log.i(TAG, "[$traceId] Saved SMS to Direct Boot storage: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "[$traceId] Failed to save SMS to Direct Boot storage: ${e.message}")
        }
    }
}
