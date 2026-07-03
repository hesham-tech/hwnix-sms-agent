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

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: continue
                val body = sms.displayMessageBody ?: ""
                val timestamp = sms.timestampMillis
                
                // للحصول على شريحة الـ SIM المستلمة (إذا كانت متوفرة)
                val subscriptionId = intent.getIntExtra("subscription", -1).toString()

                Log.i(TAG, "Incoming SMS intercepted from: $sender on SIM Slot ID: $subscriptionId")

                // معرف محلي فريد للرسالة لمنع التكرار
                val messageRef = UUID.randomUUID().toString()

                val smsEntity = SmsEntity(
                    phoneNumber = sender,
                    messageBody = body,
                    direction = "incoming",
                    status = "pending_upload",
                    messageRef = messageRef,
                    subscriptionId = subscriptionId,
                    sentAt = timestamp
                )

                // تخزين ومزامنة فورية بالخلفية
                val smsDao = AppDatabase.getDatabase(context).smsDao()
                val syncEngine = SyncEngine(context)

                GlobalScope.launch {
                    try {
                        // 1. التخزين المحلي
                        smsDao.insert(smsEntity)
                        
                        // 2. تفعيل محرك المزامنة فوراً لرفعها
                        syncEngine.performFullSync()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing incoming SMS: ${e.message}")
                    }
                }
            }
        }
    }
}
