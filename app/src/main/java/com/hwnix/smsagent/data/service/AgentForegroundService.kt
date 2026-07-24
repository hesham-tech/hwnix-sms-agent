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

/**
 * تعليق عربي مختصر: خدمة خلفية فائقة التوفير للبطارية تعتمد على الأحداث المباشرة وكبح تحديثات الإشعارات لمنع تنبيه استنزاف الطاقة.
 */
class AgentForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var syncEngine: SyncEngine
    private lateinit var sessionManager: SessionManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var lastNotificationTitle: String? = null
    private var lastNotificationText: String? = null
    private var lastNotificationTime: Long = 0L

    companion object {
        private const val TAG = "AgentService"
        private const val CHANNEL_ID = "hwnix_agent_foreground_channel"
        private const val NOTIFICATION_ID = 2026
        private const val NOTIFICATION_THROTTLE_MS = 15 * 60 * 1000L // تحديث الإشعار كل 15 دقيقة فقط إذا لم تتغير الحالة
        private const val DEFAULT_IDLE_POLL_INTERVAL_SEC = 60L // مزامنة خفيفة كل 60 ثانية لحماية البطارية ومودم الـ 4G
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
            registerNetworkMonitor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed in onCreate: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.onCreate", e)
        }
    }

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkMonitor() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val builder = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)

            networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    val logMsg = "NETWORK_RESTORED: Internet connection restored. Triggering immediate sync..."
                    Log.i(TAG, logMsg)
                    BootTracker.updateStage(applicationContext, logMsg)

                    com.hwnix.smsagent.data.local.ServiceHealthMonitor.updateHealth(
                        isInternetAvailable = true,
                        reason = "عودة الاتصال بالإنترنت",
                        context = applicationContext
                    )
                    updateLiveNotification(force = true)

                    serviceScope.launch {
                        try {
                            if (ensureDependencies()) {
                                syncEngine.performFullSync()
                                com.hwnix.smsagent.data.local.ServiceHealthMonitor.recordSuccessfulSync(applicationContext)
                                com.hwnix.smsagent.data.local.ServiceHealthMonitor.recordHeartbeat(applicationContext)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Immediate sync after network restore failed: ${e.message}")
                        }
                    }
                }

                override fun onLost(network: android.net.Network) {
                    val logMsg = "NETWORK_DISCONNECTED: Internet connection lost!"
                    Log.w(TAG, logMsg)
                    BootTracker.updateStage(applicationContext, logMsg)

                    com.hwnix.smsagent.data.local.ServiceHealthMonitor.updateHealth(
                        isInternetAvailable = false,
                        reason = "انقطاع الاتصال بالإنترنت",
                        context = applicationContext
                    )
                    updateLiveNotification(force = true)
                }
            }

            cm.registerNetworkCallback(builder.build(), networkCallback!!)
            Log.i(TAG, "NetworkCallback registered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NetworkCallback: ${e.message}")
        }
    }

    private fun unregisterNetworkMonitor() {
        try {
            if (networkCallback != null) {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                cm.unregisterNetworkCallback(networkCallback!!)
                networkCallback = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister NetworkCallback: ${e.message}")
        }
    }

    private var isForegroundPromoted = false

    /**
     * تحديث ذكي ومكبوح لـ Notification الكشف لمنع تنبيه استنزاف الطاقة من أنظمة OEM (Huawei/Honor Phone Manager)
     */
    private fun updateLiveNotification(force: Boolean = false) {
        try {
            createNotificationChannel()
            val health = com.hwnix.smsagent.data.local.ServiceHealthMonitor.getHealth()

            val notificationTitle = "${health.overallHealth.icon} ${health.overallHealth.label}"
            val notificationText = health.statusMessage
            val now = System.currentTimeMillis()

            // كبح استدعاء manager.notify() إذا لم تتغير الحالة ولم ينقضِ وقت التهدئة (15 دقيقة)
            if (!force && isForegroundPromoted &&
                notificationTitle == lastNotificationTitle &&
                notificationText == lastNotificationText &&
                (now - lastNotificationTime) < NOTIFICATION_THROTTLE_MS
            ) {
                return
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, pendingIntentFlags
            )

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

            lastNotificationTitle = notificationTitle
            lastNotificationText = notificationText
            lastNotificationTime = now
        } catch (e: Exception) {
            Log.e(TAG, "Failed in updateLiveNotification: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.updateLiveNotification", e)
        }
    }

    private fun promoteToForeground() {
        if (isForegroundPromoted) {
            Log.d(TAG, "Foreground already active.")
            return
        }
        BootTracker.updateStage(applicationContext, "CALLING_START_FOREGROUND")
        updateLiveNotification(force = true)
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
                        updateLiveNotification(force = true)
                        wasLockedWaitingLogged = true
                    }
                    delay(5_000L)
                    continue
                }

                if (wasLockedWaitingLogged) {
                    BootTracker.updateStage(applicationContext, "USER_UNLOCKED_DETECTED: Phone unlocked, resuming full sync")
                    wasLockedWaitingLogged = false
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
                }

                val intervalSeconds: Long = try {
                    val configured = (sessionManager.getPollingInterval() as Number).toLong()
                    if (configured < DEFAULT_IDLE_POLL_INTERVAL_SEC) DEFAULT_IDLE_POLL_INTERVAL_SEC else configured
                } catch (_: Exception) {
                    DEFAULT_IDLE_POLL_INTERVAL_SEC
                }

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
        val forensicLog = "FORENSIC_EVENT: SERVICE_ON_DESTROY | Time: ${System.currentTimeMillis()}"
        Log.i(TAG, forensicLog)
        BootTracker.updateStage(applicationContext, forensicLog)
        unregisterNetworkMonitor()
        updateServiceState(AgentServiceState.STOPPED)
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) { /* ignore */ }
        serviceJob.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val forensicLog = "FORENSIC_EVENT: SERVICE_ON_TASK_REMOVED | Action: ${rootIntent?.action ?: "N/A"} | Time: ${System.currentTimeMillis()}"
        Log.i(TAG, forensicLog)
        BootTracker.updateStage(applicationContext, forensicLog)

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
            val alarmLog = "FORENSIC_EVENT: ALARM_RESTART_SCHEDULED | Delay: 1000ms"
            Log.i(TAG, alarmLog)
            BootTracker.updateStage(applicationContext, alarmLog)
        } catch (e: Exception) {
            val alarmErr = "FORENSIC_EVENT: ALARM_SCHEDULE_FAILED | Err: ${e.message}"
            Log.e(TAG, alarmErr)
            BootTracker.updateStage(applicationContext, alarmErr)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
