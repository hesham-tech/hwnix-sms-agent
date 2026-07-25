package com.hwnix.cash.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hwnix.cash.data.service.AgentRestartWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

// مستقبل الرسائل الواردة — يجمع أجزاء الرسالة الطويلة في رسالة واحدة قبل الحفظ ومصمم ببدائل تعافي فائقة
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

                        // إيقاظ الخدمة الخلفية لضمان استمراريتها ومزامنة الرسالة الصادرة/الواردة
                        try {
                            val serviceIntent = Intent(context, com.hwnix.cash.data.service.AgentForegroundService::class.java).apply {
                                putExtra("launcher_source", "SMS_RECEIVER")
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (sEx: Exception) {
                            Log.w(TAG, "Could not start foreground service from SmsReceiver, triggering WorkManager fallback: ${sEx.message}")
                            try {
                                AgentRestartWorker.enqueue(context)
                            } catch (_: Exception) {}
                        }

                        val request = com.hwnix.cash.data.local.SmsImportRequest(
                            phoneNumber = sender,
                            messageBody = fullBody,
                            subscriptionId = subscriptionId,
                            sentAt = timestamp,
                            source = "SmsReceiver",
                            pduTimestamp = timestamp
                        )

                        val imported = com.hwnix.cash.data.local.SmsImportManager.importMessage(context, request)
                        if (imported) {
                            try {
                                com.hwnix.cash.core.di.ServiceLocator.syncEngine.performFullSync()
                            } catch (e: Exception) {
                                Log.w(TAG, "Trigger full sync after SMS import failed: ${e.message}")
                            }
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
}
