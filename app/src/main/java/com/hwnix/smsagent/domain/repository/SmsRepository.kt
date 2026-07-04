package com.hwnix.smsagent.domain.repository

import com.google.gson.JsonObject
import com.hwnix.smsagent.data.local.SmsEntity
import retrofit2.Response

interface SmsRepository {
    // عمليات الشبكة (Remote API)
    suspend fun uploadIncomingSms(idempotencyKey: String, body: JsonObject): Response<JsonObject>
    suspend fun syncSmsStatus(idempotencyKey: String, body: JsonObject): Response<JsonObject>
    suspend fun batchSyncSms(idempotencyKey: String, body: JsonObject): Response<JsonObject>

    // عمليات قاعدة البيانات المحلية (Local DB)
    suspend fun insertSms(sms: SmsEntity): Long
    suspend fun updateSms(sms: SmsEntity)
    suspend fun getPendingUploads(): List<SmsEntity>
    suspend fun exists(messageRef: String): Boolean
    suspend fun cleanOldLogs(timestamp: Long)
}

