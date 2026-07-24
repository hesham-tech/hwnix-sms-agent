package com.hwnix.smsagent.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hwnix.smsagent.data.local.BootTracker
import com.hwnix.smsagent.data.service.AgentForegroundService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * تعليق عربي مختصر: مستقبل أحداث إقلاع وهيكلة تشغيل الهاتف لتسجيل التشخيصات محلياً وإطلاق خدمات الخلفية.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: "UNKNOWN_ACTION"

        // ════════════════════════════════════════════════════════
        // TRIPWIRE — أول سطر مطلق — مستقل عن أي منطق آخر
        // يكتب ملف خام مباشرة في Device Protected Storage
        // إذا وُجد هذا الملف بعد الإقلاع → BootReceiver استُدعي فعلاً
        // إذا لم يوجد → لم يُستدعَ أصلاً
        // ════════════════════════════════════════════════════════
        try {
            val dpContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.createDeviceProtectedStorageContext() else context
            val tripwireFile = File(dpContext.filesDir, "boot_tripwire.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = "[$timestamp] BOOT_RECEIVER_CALLED | action=$action\n"
            tripwireFile.appendText(entry)
            Log.i("BootReceiver", "TRIPWIRE written: $entry")
        } catch (t: Throwable) {
            Log.e("BootReceiver", "TRIPWIRE write failed: ${t.message}")
        }
        // ════════════════════════════════════════════════════════

        Log.i("BootReceiver", "System event detected (Action: $action).")

        try {
            // تسجيل الحدث محلياً كخطوة أولى
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
                    Log.e("BootReceiver", "Failed to start service: $errorMsg", e)
                    BootTracker.logException(context, "BootReceiver.startService", e)
                }
            } else {
                BootTracker.updateStage(context, "RECEIVER_ACTION_IGNORED")
            }
        } catch (globalEx: Exception) {
            Log.e("BootReceiver", "Global error inside onReceive: ${globalEx.message}", globalEx)
            try {
                BootTracker.logException(context, "BootReceiver.onReceive", globalEx)
            } catch (t: Throwable) { /* ignore */ }
        }
    }
}
