package com.hwnix.cash.data.service

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
import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.data.local.SyncEngine
import com.hwnix.cash.data.local.BootTracker
import com.hwnix.cash.presentation.MainActivity
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

            com.hwnix.cash.data.local.ServiceHealthMonitor.updateHealth(
                isServiceRunning = true,
                isForegroundActive = true,
                isSyncLoopRunning = true,
                consecutiveFailures = 0,
                reason = "بدء تشغيل الخدمة بنجاح",
                context = applicationContext
            )

            serviceScope.launch {
                com.hwnix.cash.core.di.ServiceLocator.syncEvents.collect {
                    updateLiveNotification(force = true)
                }
            }

            promoteToForeground()
            ensureDependencies()
            registerNetworkMonitor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed in onCreate: ${e.message}", e)
            BootTracker.logException(applicationContext, "AgentForegroundService.onCreate", e)
        }
    }

    private fun isInternetConnected(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = cm.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        } catch (e: Exception) {
            false
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

                    com.hwnix.cash.data.local.ServiceHealthMonitor.updateHealth(
                        isInternetAvailable = true,
                        reason = "عودة الاتصال بالإنترنت",
                        context = applicationContext
                    )
                    updateLiveNotification(force = true)

                    serviceScope.launch {
                        try {
                            if (ensureDependencies()) {
                                syncEngine.performFullSync()
                                com.hwnix.cash.data.local.ServiceHealthMonitor.recordSuccessfulSync(applicationContext)
                                com.hwnix.cash.data.local.ServiceHealthMonitor.recordHeartbeat(applicationContext)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Immediate sync after network restore failed: ${e.message}")
                        }
                    }
                }

                override fun onLost(network: android.net.Network) {
                    if (isInternetConnected()) {
                        Log.i(TAG, "Network interface lost, but another active connection is online (switching Wi-Fi/Cellular). Ignoring false offline.")
                        return
                    }

                    val logMsg = "NETWORK_DISCONNECTED: Internet connection lost!"
                    Log.w(TAG, logMsg)
                    BootTracker.updateStage(applicationContext, logMsg)

                    com.hwnix.cash.data.local.ServiceHealthMonitor.updateHealth(
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
            val health = com.hwnix.cash.data.local.ServiceHealthMonitor.getHealth()

            val notificationTitle = "HWNix Cash"
            val linesSummaryText = if (::sessionManager.isInitialized) sessionManager.getLinesSummary() else ""
            val limitAlertsSummary = if (::sessionManager.isInitialized) sessionManager.getLimitAlertsSummary() else ""
            
            val firstLineSummary = linesSummaryText.lines().firstOrNull { it.isNotBlank() } ?: ""
            val notificationText = when {
                limitAlertsSummary.isNotEmpty() -> "⚠️ $limitAlertsSummary"
                firstLineSummary.isNotEmpty() -> firstLineSummary
                else -> health.statusMessage
            }
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
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setLocalOnly(true)
                .setColor(health.overallHealth.colorHex)
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(null)

            if (linesSummaryText.isNotBlank()) {
                val expandedBody = if (limitAlertsSummary.isNotEmpty()) {
                    "⚠️ $limitAlertsSummary\n$linesSummaryText"
                } else {
                    linesSummaryText
                }
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            }

            val notification = builder.build().apply {
                flags = flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
            }

            if (!isForegroundPromoted) {
                if (Build.VERSION.SDK_INT >= 34) {
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } catch (fgsEx: Throwable) {
                        Log.w(TAG, "Fallback to standard startForeground: ${fgsEx.message}")
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
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

        updateServiceState(AgentServiceState.SYNCING, "Initial sync started")
        com.hwnix.cash.data.local.ServiceHealthMonitor.updateHealth(
            isSyncLoopRunning = true,
            reason = "بدء دورة المزامنة الفعالة عبر WorkManager",
            context = applicationContext
        )

        // Enqueue background periodic worker instead of while(true) loop
        com.hwnix.cash.data.worker.PeriodicSyncWorker.enqueue(applicationContext)

        // Perform one initial sync immediately
        syncJob = serviceScope.launch {
            if (!ensureDependencies()) {
                Log.w(TAG, "Dependencies not ready for initial sync.")
                return@launch
            }
            try {
                Log.d(TAG, "Initial sync triggered...")
                syncEngine.performFullSync()
                com.hwnix.cash.data.local.ServiceHealthMonitor.recordSuccessfulSync(applicationContext)
                com.hwnix.cash.data.local.ServiceHealthMonitor.recordHeartbeat(applicationContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error in initial sync: ${e.message}")
                com.hwnix.cash.data.local.ServiceHealthMonitor.recordFailure(e.message ?: "خطأ بالدورة", applicationContext)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "HWNix Gateway Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
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
        BootTracker.recordTaskRemoved(applicationContext)
        BootTracker.updateStage(applicationContext, forensicLog)

        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
            putExtra("launcher_source", "TASK_REMOVED_RESTART")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(applicationContext, 1001, restartServiceIntent, flags)
        } else {
            PendingIntent.getService(applicationContext, 1001, restartServiceIntent, flags)
        }

        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val triggerAt = System.currentTimeMillis() + 500L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(triggerAt, pendingIntent)
                alarmService.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmService.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            val alarmLog = "FORENSIC_EVENT: ALARM_CLOCK_RESTART_SCHEDULED | Delay: 500ms"
            Log.i(TAG, alarmLog)
            BootTracker.updateStage(applicationContext, alarmLog)
        } catch (e: Exception) {
            val errLog = "FORENSIC_EVENT: ALARM_SCHEDULE_FAILED | Err: ${e.message}"
            Log.e(TAG, errLog)
            BootTracker.updateStage(applicationContext, errLog)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
