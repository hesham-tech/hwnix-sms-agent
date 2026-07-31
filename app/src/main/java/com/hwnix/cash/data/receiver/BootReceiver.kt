package com.hwnix.cash.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hwnix.cash.data.local.BootTracker
import com.hwnix.cash.data.service.AgentForegroundService
import com.hwnix.cash.data.service.AgentRestartWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * تعليق عربي مختصر: مستقبل الإقلاع التلقائي واستبدال الحزمة مع حماية التعافي عبر WorkManager في أندرويد 12+.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: "UNKNOWN_ACTION"
        Log.i("BootReceiver", "BootReceiver trigger received with action: $action")

        try {
            if (BootTracker.ENABLE_DIAGNOSTICS) {
                val dpContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    context.createDeviceProtectedStorageContext() else context
                val tripwireFile = File(dpContext.filesDir, "boot_tripwire.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val entry = "[$timestamp] BOOT_RECEIVER_CALLED | action=$action\n"
                tripwireFile.appendText(entry)
            }
        } catch (t: Throwable) {
            Log.e("BootReceiver", "Failed to write boot tripwire: ${t.message}")
        }

        try {
            BootTracker.logBootEvent(context, action)

            if (action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == Intent.ACTION_USER_UNLOCKED ||
                action == Intent.ACTION_USER_PRESENT ||
                action == Intent.ACTION_POWER_CONNECTED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED
            ) {
                BootTracker.updateStage(context, "RECEIVER_ACTION_MATCHED")
                
                val serviceIntent = Intent(context, AgentForegroundService::class.java).apply {
                    putExtra("launcher_source", "BOOT_RECEIVER_$action")
                }
                try {
                    BootTracker.updateStage(context, "STARTING_FOREGROUND_SERVICE")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    BootTracker.updateStage(context, "SERVICE_START_COMMAND_SENT")
                } catch (e: Exception) {
                    val errorMsg = "START_FAILED: ${e.message}"
                    Log.e("BootReceiver", "Failed to start service directly, triggering WorkManager fallback: $errorMsg", e)
                    BootTracker.logException(context, "BootReceiver.startService", e)
                    // WorkManager Fallback لإعادة إحياء الخدمة أماناً عند حظر الخلفية في أندرويد 12+
                    try {
                        AgentRestartWorker.enqueue(context)
                    } catch (wEx: Exception) {
                        Log.e("BootReceiver", "WorkManager fallback also failed: ${wEx.message}")
                    }
                }
            } else {
                BootTracker.updateStage(context, "RECEIVER_ACTION_IGNORED")
            }
        } catch (globalEx: Exception) {
            Log.e("BootReceiver", "Global error inside onReceive: ${globalEx.message}", globalEx)
            try {
                BootTracker.logException(context, "BootReceiver.onReceive", globalEx)
            } catch (_: Throwable) {}
        }
    }
}
