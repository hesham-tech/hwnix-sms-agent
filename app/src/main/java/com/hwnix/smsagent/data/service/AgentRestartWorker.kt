package com.hwnix.smsagent.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hwnix.smsagent.data.local.SessionManager

/* تعليق عربي مختصر: عامل خلفية جدولي للتحقق وإعادة تشغيل الخدمة الأمامية لضمان استمراريتها بعد الإقلاع */
class AgentRestartWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("AgentRestartWorker", "Worker triggered. Checking foreground service state...")
        
        val sessionManager = SessionManager(applicationContext)
        val hasSession = sessionManager.getAuthToken() != null && sessionManager.getDeviceId() != -1L

        if (hasSession) {
            try {
                val serviceIntent = Intent(applicationContext, AgentForegroundService::class.java).apply {
                    putExtra("launcher_source", "RESTART_WORKER")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(serviceIntent)
                } else {
                    applicationContext.startService(serviceIntent)
                }
                Log.i("AgentRestartWorker", "Foreground service started successfully by worker.")
            } catch (e: Exception) {
                Log.e("AgentRestartWorker", "Failed to start foreground service from worker: ${e.message}", e)
                return Result.retry()
            }
        } else {
            Log.i("AgentRestartWorker", "No active session. Skipping service startup.")
        }

        return Result.success()
    }
}
