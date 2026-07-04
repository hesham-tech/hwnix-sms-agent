package com.hwnix.smsagent.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hwnix.smsagent.data.local.SmsEntity
import com.hwnix.smsagent.data.local.AppDatabase
import com.hwnix.smsagent.data.local.SyncEngine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.UUID

// مستقبل الرسائل الواردة — يجمع أجزاء الرسالة الطويلة في رسالة واحدة قبل الحفظ
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val subscriptionId = intent.getIntExtra("subscription", -1).toString()
            val grouped = messages.groupBy { it.displayOriginatingAddress ?: "unknown" }

            val pendingResult = goAsync()

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val isUnlocked = androidx.core.os.UserManagerCompat.isUserUnlocked(context)

                    for ((sender, parts) in grouped) {
                        val fullBody = parts.joinToString("") { it.displayMessageBody ?: "" }
                        val timestamp = parts.first().timestampMillis

                        Log.i(TAG, "Incoming SMS from: $sender | SIM: $subscriptionId | Unlocked: $isUnlocked")

                        if (isUnlocked) {
                            try {
                                val smsDao = com.hwnix.smsagent.core.di.ServiceLocator.database.smsDao()
                                val syncEngine = SyncEngine(com.hwnix.smsagent.core.di.ServiceLocator.appContext)

                                val smsEntity = SmsEntity(
                                    phoneNumber = sender,
                                    messageBody = fullBody,
                                    direction = "incoming",
                                    status = "pending_upload",
                                    messageRef = UUID.randomUUID().toString(),
                                    subscriptionId = subscriptionId,
                                    sentAt = timestamp
                                )
                                smsDao.insert(smsEntity)
                                syncEngine.performFullSync()
                                Log.i(TAG, "Directly saved incoming SMS to database.")
                            } catch (dbEx: Exception) {
                                Log.e(TAG, "Failed database write under unlocked state, falling back to file storage: ${dbEx.message}")
                                saveToDeviceProtectedStorage(context, sender, fullBody, subscriptionId, timestamp)
                            }
                        } else {
                            // وضع Direct Boot (قاعدة البيانات المشفرة والـ Keystore غير متاحين)
                            // نحفظ الرسالة مؤقتاً كملف JSON في مساحة التخزين المحمية للجهاز (Device Protected Storage)
                            saveToDeviceProtectedStorage(context, sender, fullBody, subscriptionId, timestamp)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming SMS receiver: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun saveToDeviceProtectedStorage(
        context: Context,
        sender: String,
        body: String,
        subscriptionId: String,
        timestamp: Long
    ) {
        try {
            val deviceProtectedContext = context.createDeviceProtectedStorageContext()
            val directory = java.io.File(deviceProtectedContext.filesDir, "direct_boot_sms")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = java.io.File(directory, "sms_${System.currentTimeMillis()}_${UUID.randomUUID()}.json")
            val json = com.google.gson.JsonObject().apply {
                addProperty("phoneNumber", sender)
                addProperty("messageBody", body)
                addProperty("subscriptionId", subscriptionId)
                addProperty("sentAt", timestamp)
            }.toString()

            file.writeText(json)
            Log.i(TAG, "Successfully queued SMS in device protected files: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write SMS payload to device protected storage: ${e.message}", e)
        }
    }
}
