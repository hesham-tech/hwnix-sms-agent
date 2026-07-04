package com.hwnix.smsagent.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.local.SyncEngine
import com.hwnix.smsagent.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// خدمة الخلفية الدائمة — تستخدم WakeLock لضمان عمل الـ polling على Android 9 وما دونه
class AgentForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var syncEngine: SyncEngine
    private lateinit var sessionManager: SessionManager
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "AgentService"
        private const val CHANNEL_ID = "hwnix_agent_foreground_channel"
        private const val NOTIFICATION_ID = 2026
    }

    override fun onCreate() {
        super.onCreate()
        syncEngine = SyncEngine(applicationContext)
        sessionManager = SessionManager(applicationContext)
        createNotificationChannel()

        // إعداد WakeLock لمنع تجميد الـ CPU أثناء دورة الـ polling
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HWNix:AgentWakeLock"
        ).apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting Agent Foreground Service...")

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("بوابة الرسائل HWNix")
            .setContentText("تطبيق بوابة الرسائل يعمل بالخلفية لمزامنة الخطوط والرسائل.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startPeriodicSyncLoop()

        return START_STICKY
    }

    private fun startPeriodicSyncLoop() {
        serviceScope.launch {
            while (true) {
                // تنشيط WakeLock أثناء دورة المزامنة لضمان اكتمالها على Android 9
                try {
                    wakeLock?.acquire(60_000L) // timeout 60 ثانية كحد أقصى
                } catch (e: Exception) {
                    Log.w(TAG, "WakeLock acquire failed: ${e.message}")
                }

                try {
                    Log.d(TAG, "Periodic sync triggered...")
                    syncEngine.performFullSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sync loop: ${e.message}")
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) wakeLock?.release()
                    } catch (e: Exception) { /* ignore */ }
                }

                val intervalSeconds = sessionManager.getPollingInterval()
                Log.d(TAG, "Next sync in ${intervalSeconds}s")
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
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) { /* ignore */ }
        serviceJob.cancel()
        Log.i(TAG, "Agent Foreground Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
