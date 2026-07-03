package com.hwnix.smsagent.data.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hwnix.smsagent.data.local.SyncEngine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FcmListenerService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmListenerService"
    }

    /**
     * تُستدعى عند استقبال إشعار FCM (Data Message).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i(TAG, "Silent FCM notification received from: ${remoteMessage.from}")

        // قراءة البيانات المرسلة كـ Data Payload
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val command = data["command"] ?: ""
            Log.d(TAG, "FCM data payload command: $command")

            // تفعيل محرك المزامنة لإيقاظ التطبيق وسحب الأوامر فوراً
            val syncEngine = SyncEngine(applicationContext)
            GlobalScope.launch {
                try {
                    syncEngine.performFullSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to execute sync on FCM wakeup: ${e.message}")
                }
            }
        }
    }

    /**
     * تُستدعى عند توليد أو تجديد توكن FCM للهاتف.
     * يجب رفع هذا التوكن مستقبلاً للسيرفر في سجلات الجهاز لإتمام الإشعار.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token generated: $token")
        
        // حفظ توكن FCM محلياً لتمريره مع طلب تسجيل الهاتف اللاحق
        val sharedPreferences = getSharedPreferences("hwnix_fcm_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }
}
