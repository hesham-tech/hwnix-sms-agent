package com.hwnix.smsagent.data.local

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.os.UserManagerCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * تعليق عربي مختصر: وحدة تشخيص دائمة ومتقدمة لتتبع الإقلاع محلياً وتجميع أخطاء التشغيل ومعلومات النظام.
 */
object BootTracker {

    private const val PREFS_NAME = "boot_diagnostics_prefs"

    private fun getSafePrefs(context: Context): android.content.SharedPreferences {
        val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun logBootEvent(context: Context, action: String) {
        val prefs = getSafePrefs(context)
        val currentCount = prefs.getInt("boot_count", 0) + 1
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        prefs.edit().apply {
            putInt("boot_count", currentCount)
            putString("last_action", action)
            putString("last_stage", "RECEIVER_ENTERED")
            putString("last_timestamp", timeStamp)
            putString("boot_history_${currentCount}", "[$timeStamp] Action: $action")
            apply()
        }
        updateStage(context, "RECEIVER_ENTERED (Action: $action)")
    }

    fun updateStage(context: Context, stage: String) {
        val prefs = getSafePrefs(context)
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentStageLog = prefs.getString("stage_log", "") ?: ""
        val newStageLog = if (currentStageLog.isEmpty()) "[$timeStamp] $stage" else "$currentStageLog\n[$timeStamp] $stage"

        prefs.edit().apply {
            putString("last_stage", stage)
            putString("stage_log", newStageLog)
            apply()
        }
    }

    fun logException(context: Context, tag: String, throwable: Throwable) {
        val prefs = getSafePrefs(context)
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = throwable.stackTraceToString()
        val errorLog = "[$timeStamp] Error inside $tag:\n$stackTrace"

        prefs.edit().apply {
            putString("exception_trace", errorLog)
            putString("last_stage", "ERROR_ENCOUNTERED")
            apply()
        }
        updateStage(context, "ERROR: ${throwable.message}")
    }

    fun clearLog(context: Context) {
        getSafePrefs(context).edit().clear().apply()
        // حذف ملف الـ Tripwire أيضاً عند مسح السجل
        try {
            val dpContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.createDeviceProtectedStorageContext() else context
            File(dpContext.filesDir, "boot_tripwire.txt").delete()
        } catch (_: Throwable) {}
    }

    private fun isBatteryOptimizationsIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    private fun readTripwire(context: Context): String {
        return try {
            val dpContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.createDeviceProtectedStorageContext() else context
            val file = File(dpContext.filesDir, "boot_tripwire.txt")
            if (file.exists()) file.readText().trim() else "NO_TRIPWIRE_FILE (BootReceiver لم يُستدعَ أصلاً)"
        } catch (t: Throwable) {
            "TRIPWIRE_READ_ERROR: ${t.message}"
        }
    }

    fun getDiagnostics(context: Context): Map<String, Any> {
        val prefs = getSafePrefs(context)
        val bootCount = prefs.getInt("boot_count", 0)
        val lastAction = prefs.getString("last_action", "None") ?: "None"
        val lastStage = prefs.getString("last_stage", "None") ?: "None"
        val lastTimestamp = prefs.getString("last_timestamp", "None") ?: "None"
        val stageLog = prefs.getString("stage_log", "") ?: ""
        val exceptionTrace = prefs.getString("exception_trace", "") ?: ""

        val historyList = mutableListOf<String>()
        for (i in 1..bootCount) {
            val record = prefs.getString("boot_history_${i}", null)
            if (record != null) {
                historyList.add(record)
            }
        }

        // قراءة معلومات النظام الحية
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val isBatteryIgnored = isBatteryOptimizationsIgnored(context)
        val isUnlocked = UserManagerCompat.isUserUnlocked(context)
        val tripwire = readTripwire(context)

        return mapOf(
            "boot_count" to bootCount,
            "last_action" to lastAction,
            "last_stage" to lastStage,
            "last_timestamp" to lastTimestamp,
            "stage_log" to stageLog,
            "exception_trace" to exceptionTrace,
            "history" to historyList,
            "system_manufacturer" to manufacturer,
            "system_model" to model,
            "system_android_version" to androidVersion,
            "system_sdk" to sdkInt,
            "system_battery_ignored" to isBatteryIgnored,
            "system_unlocked" to isUnlocked,
            "tripwire" to tripwire
        )
    }

    fun exportFormattedDiagnostics(context: Context): String {
        val diag = getDiagnostics(context)
        val report = StringBuilder()
        report.append("=== HWNix SMS Agent Diagnostics Report ===\n")
        report.append("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
        
        report.append("--- SYSTEM INFO ---\n")
        report.append("Manufacturer: ${diag["system_manufacturer"]}\n")
        report.append("Model: ${diag["system_model"]}\n")
        report.append("Android Version: ${diag["system_android_version"]} (SDK ${diag["system_sdk"]})\n")
        report.append("Battery Optimization Ignored: ${diag["system_battery_ignored"]}\n")
        report.append("User Unlocked (After Boot): ${diag["system_unlocked"]}\n\n")

        report.append("--- BOOT RECEIVER TRIPWIRE ---\n")
        report.append("${diag["tripwire"]}\n\n")

        report.append("--- BOOT TRACKER ---\n")
        report.append("Boot Count: ${diag["boot_count"]}\n")
        report.append("Last Boot Action: ${diag["last_action"]}\n")
        report.append("Last Boot Stage: ${diag["last_stage"]}\n")
        report.append("Last Boot Time: ${diag["last_timestamp"]}\n\n")

        report.append("--- SERVICE HEALTH ---\n")
        val health = ServiceHealthMonitor.getHealth()
        report.append("Overall Health: ${health.overallHealth.icon} ${health.overallHealth.name} (${health.overallHealth.label})\n")
        report.append("Service Running: ${health.isServiceRunning}\n")
        report.append("Foreground Active: ${health.isForegroundActive}\n")
        report.append("Sync Loop Running: ${health.isSyncLoopRunning}\n")
        report.append("Last Successful Sync: ${ServiceHealthMonitor.formatTime(health.lastSuccessfulSyncTime)}\n")
        report.append("Last Heartbeat: ${ServiceHealthMonitor.formatTime(health.lastHeartbeatTime)}\n")
        report.append("Last Poll: ${ServiceHealthMonitor.formatTime(health.lastPollTime)}\n")
        report.append("Consecutive Failures: ${health.consecutiveFailures}\n")
        report.append("Recovery Count: ${health.recoveryCount}\n")
        report.append("Status Message: ${health.statusMessage}\n")
        report.append("Reason for Last State Change: ${health.reasonForLastStateChange}\n\n")

        report.append("--- DETAILED TIMELINE ---\n")
        report.append("${diag["stage_log"]}\n\n")

        val exception = diag["exception_trace"] as? String ?: ""
        if (exception.isNotEmpty()) {
            report.append("--- EXCEPTION LOGS ---\n")
            report.append("$exception\n\n")
        }

        report.append("==========================================")
        return report.toString()
    }
}
