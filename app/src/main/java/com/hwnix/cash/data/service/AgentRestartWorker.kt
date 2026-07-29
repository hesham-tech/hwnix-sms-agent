package com.hwnix.cash.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hwnix.cash.data.local.SessionManager

/* تعليق عربي مختصر: عامل خلفية جدولي واستثنائي (Expedited) لإعادة تشغيل الخدمة الأمامية وتجاوز قيود الخلفية في أندرويد 12+ */
class AgentRestartWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        fun enqueue(context: Context) {
            try {
                val builder = OneTimeWorkRequestBuilder<AgentRestartWorker>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
                val request = builder.build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "AgentRestartWorker",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
                Log.i("AgentRestartWorker", "AgentRestartWorker enqueued successfully (Expedited mode).")
            } catch (e: Exception) {
                Log.e("AgentRestartWorker", "Failed to enqueue AgentRestartWorker: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.i("AgentRestartWorker", "Worker triggered. Ensuring foreground service is active...")

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

        return Result.success()
    }
}
