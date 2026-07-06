package com.hwnix.smsagent.domain.repository

import com.google.gson.JsonObject
import com.hwnix.smsagent.domain.model.AppUpdate
import com.hwnix.smsagent.domain.model.SimCard

interface DeviceRepository {
    suspend fun checkAppUpdate(): Result<AppUpdate?>
    suspend fun registerDevice(): Result<Long>
    suspend fun syncLines(simCards: List<SimCard>): Result<Unit>
    suspend fun sendHeartbeat(
        batteryLevel: Int,
        networkType: String,
        isInternetAvailable: Boolean,
        freeMemory: Long,
        freeStorage: Long
    ): Result<Int> // يعيد فترة النبض المحدثة (polling interval)
    
    suspend fun getDeviceConfig(): Result<JsonObject>
    suspend fun getDeviceLines(deviceId: Long): Result<Map<Int, Pair<String, String>>>
    suspend fun getPendingCommands(deviceId: Long): Result<JsonObject>
    suspend fun executeCommand(commandId: Long, idempotencyKey: String, body: JsonObject): Result<JsonObject>
    suspend fun decoupleDevice(deviceId: Long): Result<Unit>
}

