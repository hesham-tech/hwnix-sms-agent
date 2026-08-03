package com.hwnix.cash.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.hwnix.cash.data.local.SyncEngine
import java.util.concurrent.TimeUnit

class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PeriodicSyncWorker"
        const val WORK_NAME = "hwnix_periodic_sync"

        fun enqueue(context: Context, intervalMinutes: Long = 15L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "PeriodicSyncWorker enqueued with interval: $intervalMinutes minutes.")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "PeriodicSyncWorker executing sync...")
        val syncEngine = SyncEngine(applicationContext)
        return try {
            syncEngine.performFullSync()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "PeriodicSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
