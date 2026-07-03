package com.hwnix.smsagent.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("v1/agent/public/app-update/check")
    suspend fun checkAppUpdate(): Response<JsonObject>

    @POST("v1/agent/auth/login")
    suspend fun login(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/agent/auth/refresh")
    suspend fun refreshToken(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/agent/device/register")
    suspend fun registerDevice(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/agent/device/sync-lines")
    suspend fun syncLines(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/agent/device/heartbeat")
    suspend fun sendHeartbeat(@Body body: JsonObject): Response<JsonObject>

    @GET("v1/agent/device/config")
    suspend fun getDeviceConfig(): Response<JsonObject>

    @GET("v1/agent/device/lines")
    suspend fun getDeviceLines(@Query("device_id") deviceId: Long): Response<JsonObject>

    @GET("v1/agent/commands/pending")
    suspend fun getPendingCommands(@Query("device_id") deviceId: Long): Response<JsonObject>

    @POST("v1/agent/commands/{id}/execute")
    suspend fun executeCommand(
        @Path("id") commandId: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/agent/sms/incoming")
    suspend fun uploadIncomingSms(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/agent/sms/sync-status")
    suspend fun syncSmsStatus(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/agent/sms/batch-sync")
    suspend fun batchSyncSms(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>
}
