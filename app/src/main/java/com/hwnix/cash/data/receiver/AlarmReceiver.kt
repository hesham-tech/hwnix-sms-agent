package com.hwnix.cash.data.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hwnix.cash.data.local.BootTracker
import com.hwnix.cash.data.service.AgentForegroundService
import com.hwnix.cash.data.service.AgentRestartWorker

/* تعليق عربي مختصر: حارس النبضات المستقل عبر AlarmManager لإيقاظ معالج الهاتف CPU وضمان تشغيل الخدمة الأمامية بعد الإقلاع */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ALARM_ACTION = "com.hwnix.cash.ACTION_ALARM_WATCHDOG"
        private const val REQUEST_CODE = 9090

        fun scheduleWatchdog(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ALARM_ACTION
                }
                
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                
                val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
                val intervalMs = 15 * 60 * 1000L // كل 15 دقيقة
                val triggerAtMs = System.currentTimeMillis() + 60 * 1000L // تبدأ بعد دقيقة من التشغيل

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        intervalMs,
                        pendingIntent
                    )
                }
                Log.i(TAG, "Watchdog alarm scheduled successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule Watchdog alarm: ${e.message}", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "AlarmReceiver watchdog triggered: action=${intent.action}")
        BootTracker.updateStage(context, "ALARM_WATCHDOG_TRIGGERED")

        try {
            val serviceIntent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra("launcher_source", "ALARM_WATCHDOG")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            BootTracker.updateStage(context, "ALARM_SERVICE_STARTED")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service directly from AlarmReceiver, enqueuing worker: ${e.message}", e)
            BootTracker.logException(context, "AlarmReceiver.onReceive", e)
            AgentRestartWorker.enqueue(context)
        }

        // إشعار وإعادة جدولة المنبه القادم
        scheduleWatchdog(context)
    }
}
