package com.hwnix.cash.data.repository

import com.google.gson.JsonObject
import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.data.local.SmsDao
import com.hwnix.cash.data.local.SmsEntity
import com.hwnix.cash.data.remote.ApiService
import com.hwnix.cash.domain.repository.SmsRepository
import retrofit2.Response

class SmsRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val smsDao: SmsDao
) : SmsRepository {

    override suspend fun uploadIncomingSms(idempotencyKey: String, body: JsonObject): Response<JsonObject> {
        return apiService.uploadIncomingSms(idempotencyKey, body)
    }

    override suspend fun syncSmsStatus(idempotencyKey: String, body: JsonObject): Response<JsonObject> {
        return apiService.syncSmsStatus(idempotencyKey, body)
    }

    override suspend fun batchSyncSms(idempotencyKey: String, body: JsonObject): Response<JsonObject> {
        return apiService.batchSyncSms(idempotencyKey, body)
    }

    override suspend fun insertSms(sms: SmsEntity): Long {
        return smsDao.insert(sms)
    }

    override suspend fun updateSms(sms: SmsEntity) {
        smsDao.update(sms)
    }

    override suspend fun getPendingUploads(): List<SmsEntity> {
        return smsDao.getPendingUploads()
    }

    override suspend fun exists(messageRef: String): Boolean {
        return smsDao.exists(messageRef)
    }

    override suspend fun cleanOldLogs(timestamp: Long) {
        smsDao.cleanOldLogs(timestamp)
    }
}

