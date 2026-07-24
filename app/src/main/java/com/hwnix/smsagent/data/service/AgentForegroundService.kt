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
import com.hwnix.smsagent.data.local.BootTracker
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

    enum class AgentServiceState {
        CREATED,
        FOREGROUND_PROMOTED,
        WAITING_FOR_UNLOCK,
        SESSION_INITIALIZED,
        SYNCING,
        STOPPED
    }

    private var currentState: AgentServiceState = AgentServiceState.STOPPED

    private fun updateServiceState(newState: AgentServiceState, detail: String? = null) {
        currentState = newState
        val logMsg = "SERVICE_STATE -> ${newState.name}${if (detail != null) " ($detail)" else ""}"
        Log.i(TAG, logMsg)
        BootTracker.updateStage(applicationContext, logMsg)
    }

    private fun isUserUnlocked(): Boolean {
        return androidx.core.os.UserManagerCompat.isUserUnlocked(applicationContext)
    }

    private fun ensureDependencies(): Boolean {
        if (!isUserUnlocked()) return false
        try {
            var newlyInitialized = false
            if (!::syncEngine.isInitialized) {
                syncEngine = SyncEngine(applicationContext)
                newlyInitialized = true
            }
            if (!::sessionManager.isInitialized) {
                sessionManager = SessionManager(applicationContext)
                newlyInitialized = true
            }
            if (newlyInitialized) {
                updateServiceState(AgentServiceState.SESSION_INITIALIZED, "Encrypted storage ready")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted dependencies: ${e.message}")
            BootTracker.logException(applicationContext, "AgentForegroundService.ensureDependencies", e)
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            updateServiceState(AgentServiceState.CREATED)

            // إعداد WakeLock لمنع تجميد الـ CPU أثناء دورة الـ polling
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HWNix:AgentWakeLock"
            ).apply { setReferenceCounted(false) }

            com.hwnix.smsagent.data.local.ServiceHealthMonitor.updateHealth(
                isServiceRunning = true,
                isForegroundActive = true,
                reason = "تخلّق الخدمة",
                context = applicationContext
            )

            promoteToForeground()
            ensureDependencies()
        } catch (e: Exception) {
            Log.e(TAG, "Failed in onCreate: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.onCreate", e)
        }
    }

    private var isForegroundPromoted = false

    private fun updateLiveNotification() {
        try {
            createNotificationChannel()
            val health = com.hwnix.smsagent.data.local.ServiceHealthMonitor.getHealth()

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, pendingIntentFlags
            )

            val notificationTitle = "${health.overallHealth.icon} ${health.overallHealth.label}"
            val notificationText = health.statusMessage

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setColor(health.overallHealth.colorHex)
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            val notification = builder.build()

            if (!isForegroundPromoted) {
                startForeground(NOTIFICATION_ID, notification)
                isForegroundPromoted = true
                updateServiceState(AgentServiceState.FOREGROUND_PROMOTED)
                BootTracker.updateStage(applicationContext, "START_FOREGROUND_DONE")
            } else {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed in updateLiveNotification: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.updateLiveNotification", e)
        }
    }

    private fun promoteToForeground() {
        if (isForegroundPromoted) {
            Log.d(TAG, "Foreground already active. Refreshing notification status.")
            updateLiveNotification()
            return
        }
        BootTracker.updateStage(applicationContext, "CALLING_START_FOREGROUND")
        updateLiveNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val source = intent?.getStringExtra("launcher_source") ?: "UNKNOWN"
            Log.i(TAG, "Starting Agent Foreground Service (Source: $source)...")
            BootTracker.updateStage(applicationContext, "SERVICE_ON_START_COMMAND (Source: $source)")
            promoteToForeground()
            startPeriodicSyncLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed in onStartCommand: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.onStartCommand", e)
        }
        return START_STICKY
    }

    private var syncJob: Job? = null
    private var wasLockedWaitingLogged = false

    private fun startPeriodicSyncLoop() {
        if (syncJob?.isActive == true) {
            Log.i(TAG, "Sync loop is already running. Skipping duplicate loop trigger.")
            return
        }

        updateServiceState(AgentServiceState.SYNCING, "Loop started")
        com.hwnix.smsagent.data.local.ServiceHealthMonitor.updateHealth(
            isSyncLoopRunning = true,
            reason = "بدء دورة المزامنة الفعالة",
            context = applicationContext
        )

        syncJob = serviceScope.launch {
            while (true) {
                if (!ensureDependencies()) {
                    if (!wasLockedWaitingLogged) {
                        updateServiceState(AgentServiceState.WAITING_FOR_UNLOCK, "Direct Boot Locked")
                        com.hwnix.smsagent.data.local.ServiceHealthMonitor.updateHealth(
                            isSyncLoopRunning = true,
                            reason = "انتظار فك قفل الشاشة (Direct Boot)",
                            context = applicationContext
                        )
                        updateLiveNotification()
                        wasLockedWaitingLogged = true
                    }
                    delay(5_000L)
                    continue
                }

                if (wasLockedWaitingLogged) {
                    BootTracker.updateStage(applicationContext, "USER_UNLOCKED_DETECTED: Phone unlocked, resuming full sync")
                    wasLockedWaitingLogged = false
                }

                // تنشيط WakeLock أثناء دورة المزامنة لضمان اكتمالها على Android 9
                try {
                    wakeLock?.acquire(60_000L) // timeout 60 ثانية كحد أقصى
                } catch (e: Exception) {
                    Log.w(TAG, "WakeLock acquire failed: ${e.message}")
                }

                try {
                    Log.d(TAG, "Periodic sync triggered...")
                    syncEngine.performFullSync()
                    com.hwnix.smsagent.data.local.ServiceHealthMonitor.recordSuccessfulSync(applicationContext)
                    com.hwnix.smsagent.data.local.ServiceHealthMonitor.recordHeartbeat(applicationContext)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e(TAG, "Error in sync loop: ${e.message}")
                    com.hwnix.smsagent.data.local.ServiceHealthMonitor.recordFailure(e.message ?: "خطأ بالدورة", applicationContext)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) wakeLock?.release()
                    } catch (e: Exception) { /* ignore */ }
                    updateLiveNotification()
                }

                val intervalSeconds: Long = try { (sessionManager.getPollingInterval() as Number).toLong() } catch (_: Exception) { 10L }
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
        isForegroundPromoted = false
        updateServiceState(AgentServiceState.STOPPED)
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) { /* ignore */ }
        serviceJob.cancel()
        Log.i(TAG, "Agent Foreground Service destroyed.")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "Task removed. Scheduling service restart...")
        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
            putExtra("launcher_source", "ALARM_MANAGER_RESTART")
        }
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT else PendingIntent.FLAG_ONE_SHOT
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        try {
            alarmService.set(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart alarm: ${e.message}")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

