package com.hwnix.cash.data.service

import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hwnix.cash.data.local.SyncEngine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/* تعليق عربي مختصر: خدمة استقبال نبضات FCM عالية الأولوية لإيقاظ التطبيق والخدمة تلقائياً بنمط تطبيقات WhatsApp و Messenger دون الحاجة لتفعيل التشغيل التلقائي اليدوي */
class FcmListenerService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmListenerService"
    }

    private fun ensureForegroundServiceActive() {
        try {
            val serviceIntent = Intent(applicationContext, AgentForegroundService::class.java).apply {
                putExtra("launcher_source", "FCM_HIGH_PRIORITY_WAKEUP")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
            Log.i(TAG, "Foreground service started/promoted via FCM high-priority push wakeup.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service from FCM, enqueuing worker: ${e.message}")
            AgentRestartWorker.enqueue(applicationContext)
        }
    }

    /**
     * تُستدعى عند استقبال إشعار FCM (Data Message عالية الأولوية مثل واتساب وماسنجر).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i(TAG, "Silent FCM high-priority notification received from: ${remoteMessage.from}")

        // إيقاظ وتشغيل الخدمة الأمامية فوراً
        ensureForegroundServiceActive()

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
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token generated: $token")
        
        ensureForegroundServiceActive()

        // حفظ توكن FCM محلياً لتمريره مع طلب تسجيل الهاتف اللاحق
        val sharedPreferences = getSharedPreferences("hwnix_fcm_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }
}
