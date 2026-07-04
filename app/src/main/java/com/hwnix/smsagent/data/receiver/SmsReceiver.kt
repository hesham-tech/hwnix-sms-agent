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

            // تجميع أجزاء الرسالة الطويلة (multipart) حسب المُرسِل
            val grouped = messages.groupBy { it.displayOriginatingAddress ?: "unknown" }

            val smsDao = com.hwnix.smsagent.core.di.ServiceLocator.database.smsDao()
            val syncEngine = SyncEngine(com.hwnix.smsagent.core.di.ServiceLocator.appContext)

            for ((sender, parts) in grouped) {
                // دمج جميع الأجزاء في نص واحد
                val fullBody = parts.joinToString("") { it.displayMessageBody ?: "" }
                val timestamp = parts.first().timestampMillis

                Log.i(TAG, "SMS from: $sender | SIM: $subscriptionId | Parts: ${parts.size} | Chars: ${fullBody.length}")

                val smsEntity = SmsEntity(
                    phoneNumber = sender,
                    messageBody = fullBody,
                    direction = "incoming",
                    status = "pending_upload",
                    messageRef = UUID.randomUUID().toString(),
                    subscriptionId = subscriptionId,
                    sentAt = timestamp
                )

                GlobalScope.launch {
                    try {
                        smsDao.insert(smsEntity)
                        syncEngine.performFullSync()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing incoming SMS: ${e.message}")
                    }
                }
            }
        }
    }
}
