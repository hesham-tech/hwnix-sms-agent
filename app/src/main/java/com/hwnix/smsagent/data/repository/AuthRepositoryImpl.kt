package com.hwnix.smsagent.data.repository

import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiClient
import com.hwnix.smsagent.data.remote.ApiService
import com.hwnix.smsagent.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun login(login: String, password: String): Result<Unit> {
        return try {
            val payload = JsonObject().apply {
                addProperty("login", login)
                addProperty("password", password)
                addProperty("device_uuid", sessionManager.getDeviceUuid())
                addProperty("device_name", Build.MODEL)
                addProperty("brand", Build.BRAND)
                addProperty("model", Build.MODEL)
                addProperty("android_version", Build.VERSION.RELEASE)
                addProperty("app_version", "1.0.11")
            }

            val response = apiService.login(payload)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                if (responseBody.get("status")?.asBoolean == true) {
                    val data = responseBody.getAsJsonObject("data")
                    val token = data.get("token").asString
                    sessionManager.saveAuthToken(token)
                    ApiClient.resetClient()
                    return Result.success(Unit)
                }
            }

            val errMsg = parseErrorMessage(response.errorBody()?.string())
            Result.failure(Exception(errMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun register(
        fullName: String,
        nickname: String,
        phone: String,
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            val payload = JsonObject().apply {
                addProperty("full_name", fullName)
                addProperty("nickname", nickname)
                addProperty("phone", phone)
                addProperty("email", email.ifBlank { null })
                addProperty("password", password)
                addProperty("device_uuid", sessionManager.getDeviceUuid())
            }

            val response = apiService.register(payload)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                if (responseBody.get("status")?.asBoolean == true) {
                    val data = responseBody.getAsJsonObject("data")
                    val token = data.get("token").asString
                    sessionManager.saveAuthToken(token)
                    ApiClient.resetClient()
                    return Result.success(Unit)
                }
            }

            val errMsg = parseErrorMessage(response.errorBody()?.string())
            Result.failure(Exception(errMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Register failed: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        sessionManager.clearSession()
        return Result.success(Unit)
    }

    override fun isLoggedIn(): Boolean {
        return sessionManager.getAuthToken() != null
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (!errorBody.isNullOrEmpty()) {
            try {
                val json = JsonParser.parseString(errorBody).asJsonObject
                val msg = json.get("message")?.asString
                if (!msg.isNullOrEmpty()) return msg
            } catch (ex: Exception) { /* ignore */ }
        }
        return "حدث خطأ غير معروف أثناء الاتصال بالسيرفر."
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is java.net.ConnectException -> Exception("فشل الاتصال بالسيرفر. يرجى التأكد من صحة الرابط.")
            is java.net.UnknownHostException -> Exception("فشل الاتصال بالإنترنت أو رابط السيرفر غير صحيح.")
            else -> Exception("حدث خطأ أثناء الاتصال: ${e.localizedMessage ?: e.message}")
        }
    }
}

