package com.hwnix.smsagent.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

class SessionManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "hwnix_secured_session",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_CONFIG_VERSION = "config_version"
        private const val KEY_POLLING_INTERVAL = "polling_interval"
        private const val KEY_LOGGING_LEVEL = "logging_level"
        private const val KEY_MAX_RETRY = "max_retry"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_GATEWAY_NAME = "gateway_name"
        private const val KEY_LAST_SYNC_SUCCESS = "last_sync_success"

        
        private const val DEFAULT_BASE_URL = "https://api-teste.hwnix.com/api/" // الافتراضي للمحاكي المحلي
    }

    fun getBaseUrl(): String {
        var url = sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        var cleanUrl = url.trim()
        if (cleanUrl.contains("10.0.2.2") || cleanUrl.contains("localhost") || cleanUrl.isEmpty()) {
            cleanUrl = "https://api-teste.hwnix.com/api"
        }
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        if (!cleanUrl.endsWith("/api")) {
            cleanUrl += "/api"
        }
        return "$cleanUrl/"
    }

    fun saveBaseUrl(url: String) {
        sharedPreferences.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun clearAuthToken() {
        sharedPreferences.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun getDeviceId(): Long {
        return sharedPreferences.getLong(KEY_DEVICE_ID, -1L)
    }

    fun saveDeviceId(id: Long) {
        sharedPreferences.edit().putLong(KEY_DEVICE_ID, id).apply()
    }

    /**
     * الحصول على أو إنشاء UUID فريد للجهاز الحالي.
     */
    fun getDeviceUuid(): String {
        var uuid = sharedPreferences.getString(KEY_DEVICE_UUID, null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(KEY_DEVICE_UUID, uuid).apply()
        }
        return uuid
    }

    fun getConfigVersion(): Int {
        return sharedPreferences.getInt(KEY_CONFIG_VERSION, 0)
    }

    fun saveConfigVersion(version: Int) {
        sharedPreferences.edit().putInt(KEY_CONFIG_VERSION, version).apply()
    }

    fun getPollingInterval(): Int {
        return sharedPreferences.getInt(KEY_POLLING_INTERVAL, 60)
    }

    fun savePollingInterval(seconds: Int) {
        sharedPreferences.edit().putInt(KEY_POLLING_INTERVAL, seconds).apply()
    }

    fun getLoggingLevel(): String {
        return sharedPreferences.getString(KEY_LOGGING_LEVEL, "info") ?: "info"
    }

    fun saveLoggingLevel(level: String) {
        sharedPreferences.edit().putString(KEY_LOGGING_LEVEL, level).apply()
    }

    fun getMaxRetry(): Int {
        return sharedPreferences.getInt(KEY_MAX_RETRY, 3)
    }

    fun saveMaxRetry(count: Int) {
        sharedPreferences.edit().putInt(KEY_MAX_RETRY, count).apply()
    }

    /**
     * إدارة حالة إكمال الإعداد الأولي (اسم البوابة + أرقام الخطوط).
     */
    fun isSetupComplete(): Boolean {
        return sharedPreferences.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun markSetupComplete() {
        sharedPreferences.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    fun getGatewayName(): String {
        val name = sharedPreferences.getString(KEY_GATEWAY_NAME, "")
        if (name.isNullOrBlank()) {
            return android.os.Build.MODEL
        }
        return name
    }

    fun saveGatewayName(name: String) {
        sharedPreferences.edit().putString(KEY_GATEWAY_NAME, name).apply()
    }

    /**
     * تنظيف رقم الهاتف: حذف مفتاح الدولة (+20) وعلامة + للحصول على الصيغة المحلية.
     */
    fun cleanPhoneNumber(phone: String): String {
        var cleaned = phone.trim()
        if (cleaned.startsWith("+20")) cleaned = "0" + cleaned.removePrefix("+20")
        else if (cleaned.startsWith("+")) cleaned = "0" + cleaned.removePrefix("+")
        return cleaned
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_CONFIG_VERSION)
            .remove(KEY_SETUP_COMPLETE)
            .remove(KEY_LAST_SYNC_SUCCESS)
            .apply()
    }

    fun getLastSyncSuccessTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC_SUCCESS, 0L)
    }

    fun saveLastSyncSuccessTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_SUCCESS, time).apply()
    }
}
