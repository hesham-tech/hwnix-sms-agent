package com.hwnix.smsagent.data.repository

import android.os.Build
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiService
import com.hwnix.smsagent.domain.model.AppUpdate
import com.hwnix.smsagent.domain.model.SimCard
import com.hwnix.smsagent.domain.repository.DeviceRepository
import java.util.UUID

class DeviceRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val smsDao: com.hwnix.smsagent.data.local.SmsDao
) : DeviceRepository {

    companion object {
        private const val TAG = "DeviceRepository"
    }

    override suspend fun checkAppUpdate(): Result<AppUpdate?> {
        return try {
            val response = apiService.checkAppUpdate()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    val data = body.getAsJsonObject("data")
                    val versionCode = data.get("version_code").asInt
                    val versionName = data.get("version_name").asString
                    val downloadUrl = data.get("download_url").asString
                    return Result.success(AppUpdate(versionCode, versionName, downloadUrl))
                }
            }
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "checkAppUpdate failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun registerDevice(): Result<Long> {
        return try {
            val payload = JsonObject().apply {
                addProperty("android_id", sessionManager.getDeviceUuid())
                addProperty("uuid", sessionManager.getDeviceUuid())
                addProperty("device_name", Build.MODEL)
                addProperty("brand", Build.BRAND)
                addProperty("model", Build.MODEL)
                addProperty("android_version", Build.VERSION.RELEASE)
                addProperty("app_version", "1.0.11")
            }

            val key = UUID.randomUUID().toString()
            val response = apiService.registerDevice(key, payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    val data = body.getAsJsonObject("data")
                    val deviceId = data.get("device_id").asLong
                    sessionManager.saveDeviceId(deviceId)
                    return Result.success(deviceId)
                }
            }
            Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
        } catch (e: Exception) {
            Log.e(TAG, "registerDevice failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncLines(simCards: List<SimCard>): Result<Unit> {
        return try {
            val linesArray = JsonArray()
            simCards.forEach { card ->
                val lineObj = JsonObject().apply {
                    addProperty("slot_index", card.slotIndex)
                    addProperty("subscription_id", card.subscriptionId)
                    addProperty("carrier", card.carrier)
                    addProperty("phone_number", card.phoneNumber)
                    addProperty("mcc", card.mcc)
                    addProperty("mnc", card.mnc)
                }
                linesArray.add(lineObj)
            }

            val payload = JsonObject().apply {
                addProperty("device_id", sessionManager.getDeviceId())
                add("sims", linesArray)
            }

            val key = UUID.randomUUID().toString()
            val response = apiService.syncLines(key, payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    return Result.success(Unit)
                }
            }
            Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
        } catch (e: Exception) {
            Log.e(TAG, "syncLines failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendHeartbeat(
        batteryLevel: Int,
        networkType: String,
        isInternetAvailable: Boolean,
        freeMemory: Long,
        freeStorage: Long
    ): Result<Int> {
        return try {
            val deviceId = sessionManager.getDeviceId()
            if (deviceId == -1L) return Result.failure(Exception("Device not registered"))

            val payload = JsonObject().apply {
                addProperty("device_id", deviceId)
                addProperty("network_type", networkType)
                addProperty("battery_level", batteryLevel)
                addProperty("is_internet_available", isInternetAvailable)
                addProperty("free_memory_bytes", freeMemory)
                addProperty("free_storage_bytes", freeStorage)
                addProperty("app_version", "1.0.11")
                addProperty("configuration_version", sessionManager.getConfigVersion())
            }

            val response = apiService.sendHeartbeat(payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.get("status")?.asBoolean == true) {
                    val data = body.getAsJsonObject("data")
                    val interval = data.get("polling_interval")?.asInt ?: 60
                    // حفظ الـ interval في الإعدادات المحلية
                    sessionManager.savePollingInterval(interval)
                    return Result.success(interval)
                }
            }
            Result.success(sessionManager.getPollingInterval())
        } catch (e: Exception) {
            Log.e(TAG, "sendHeartbeat failed: ${e.message}", e)
            Result.success(sessionManager.getPollingInterval()) // fallback to local stored interval on failure
        }
    }

    override suspend fun getDeviceConfig(): Result<JsonObject> {
        return try {
            val response = apiService.getDeviceConfig()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDeviceLines(deviceId: Long): Result<Map<Int, Pair<String, String>>> {
        return try {
            val response = apiService.getDeviceLines(deviceId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val result = mutableMapOf<Int, Pair<String, String>>()
                if (body.get("status")?.asBoolean == true) {
                    val data = body.getAsJsonObject("data")
                    val lines = data.getAsJsonArray("lines")
                    lines.forEach { element ->
                        val obj = element.asJsonObject
                        val slotIndex = obj.get("slot_index").asInt
                        val carrier = obj.get("carrier")?.asString ?: ""
                        val phone = obj.get("phone_number")?.asString ?: ""
                        result[slotIndex] = Pair(carrier, phone)
                    }
                }
                Result.success(result)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDeviceLines failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPendingCommands(deviceId: Long): Result<JsonObject> {
        return try {
            val response = apiService.getPendingCommands(deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun executeCommand(
        commandId: Long,
        idempotencyKey: String,
        body: JsonObject
    ): Result<JsonObject> {
        return try {
            val response = apiService.executeCommand(commandId, idempotencyKey, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (!errorBody.isNullOrEmpty()) {
            try {
                val json = JsonParser.parseString(errorBody).asJsonObject
                val msg = json.get("message")?.asString
                if (!msg.isNullOrEmpty()) return msg
            } catch (ex: Exception) { /* ignore */ }
        }
        return "حدث خطأ أثناء التواصل مع السيرفر."
    }
}

