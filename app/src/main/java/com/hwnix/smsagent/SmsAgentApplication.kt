package com.hwnix.smsagent

import android.app.Application
import android.util.Log

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.hwnix.smsagent.data.service.AgentRestartWorker

/* تعليق عربي مختصر: الفئة الأساسية للتطبيق لتهيئة الخدمات وجدولة عامل إعادة التشغيل التلقائي */
class SmsAgentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        com.hwnix.smsagent.core.di.ServiceLocator.initialize(this)
        Log.i("SmsAgentApp", "HWNix SMS Gateway Agent Application Initialized.")
        
        scheduleRestartWorker()
    }

    private fun scheduleRestartWorker() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<AgentRestartWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AgentRestartWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i("SmsAgentApp", "AgentRestartWorker scheduled successfully.")
        } catch (e: Exception) {
            Log.e("SmsAgentApp", "Failed to schedule AgentRestartWorker: ${e.message}", e)
        }
    }
}
