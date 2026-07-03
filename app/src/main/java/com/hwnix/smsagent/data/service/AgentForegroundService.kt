package com.hwnix.smsagent.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hwnix.smsagent.data.local.SyncEngine
import com.hwnix.smsagent.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var syncEngine: SyncEngine

    companion object {
        private const val TAG = "AgentService"
        private const val CHANNEL_ID = "hwnix_agent_foreground_channel"
        private const val NOTIFICATION_ID = 2026
    }

    override fun onCreate() {
        super.onCreate()
        syncEngine = SyncEngine(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting Agent Foreground Service...")

        // إعداد واجهة فتح التطبيق عند النقر على الإشعار
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        // بناء الإشعار الأمامي
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("بوابة الرسائل HWNix")
            .setContentText("تطبيق بوابة الرسائل يعمل بالخلفية لمزامنة الخطوط والرسائل.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // بدء دورة النبض والمزامنة الدورية
        startPeriodicSyncLoop()

        return START_STICKY
    }

    private fun startPeriodicSyncLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    Log.d(TAG, "Periodic sync triggered inside foreground loop...")
                    syncEngine.performFullSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic sync loop: ${e.message}")
                }
                
                // جلب مهلة النبضات التشغيلية من الإعدادات الافتراضية
                val intervalSeconds = syncEngine.sendHeartbeat().let {
                    val sharedPreferences = getSharedPreferences("hwnix_secured_session", MODE_PRIVATE)
                    sharedPreferences.getInt("polling_interval", 60)
                }
                
                delay(intervalSeconds * 1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "HWNix Gateway Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.i(TAG, "Agent Foreground Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
