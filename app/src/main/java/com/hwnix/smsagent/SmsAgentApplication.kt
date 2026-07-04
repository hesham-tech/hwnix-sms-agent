package com.hwnix.smsagent

import android.app.Application
import android.util.Log

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.hwnix.smsagent.data.service.AgentRestartWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/* تعليق عربي مختصر: الفئة الأساسية للتطبيق لتهيئة الخدمات وجدولة عامل إعادة التشغيل التلقائي واستيراد رسائل وضع القفل */
class SmsAgentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        com.hwnix.smsagent.core.di.ServiceLocator.initialize(this)
        Log.i("SmsAgentApp", "HWNix SMS Gateway Agent Application Initialized.")
        
        val isUnlocked = androidx.core.os.UserManagerCompat.isUserUnlocked(this)
        if (isUnlocked) {
            scheduleRestartWorker()
            processDirectBootSms()
        } else {
            Log.i("SmsAgentApp", "Application started in LOCKED state (Direct Boot). Postponing initialization.")
        }
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

    private fun processDirectBootSms() {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            try {
                val deviceProtectedContext = createDeviceProtectedStorageContext()
                val directory = java.io.File(deviceProtectedContext.filesDir, "direct_boot_sms")
                if (directory.exists() && directory.isDirectory) {
                    val files = directory.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        Log.i("SmsAgentApp", "Found ${files.size} queued SMS from Direct Boot mode. Importing...")
                        val smsDao = com.hwnix.smsagent.core.di.ServiceLocator.database.smsDao()
                        val syncEngine = com.hwnix.smsagent.data.local.SyncEngine(applicationContext)
                        
                        files.forEach { file ->
                            try {
                                val content = file.readText()
                                val json = com.google.gson.JsonParser.parseString(content).asJsonObject
                                val smsEntity = com.hwnix.smsagent.data.local.SmsEntity(
                                    phoneNumber = json.get("phoneNumber").asString,
                                    messageBody = json.get("messageBody").asString,
                                    subscriptionId = json.get("subscriptionId").asString,
                                    sentAt = json.get("sentAt").asLong,
                                    direction = "incoming",
                                    status = "pending_upload",
                                    messageRef = java.util.UUID.randomUUID().toString()
                                )
                                smsDao.insert(smsEntity)
                                file.delete()
                                Log.i("SmsAgentApp", "Imported queued SMS: ${file.name}")
                            } catch (e: Exception) {
                                Log.e("SmsAgentApp", "Error importing queued SMS file: ${file.name}", e)
                            }
                        }
                        syncEngine.performFullSync()
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsAgentApp", "Error processing direct boot SMS directory: ${e.message}", e)
            }
        }
    }
}
