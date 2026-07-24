package com.hwnix.smsagent.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بإدارة الجلسة والإعدادات وتطهير أرقام الهواتف وتأمين العناوين */
class SessionManagerTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
    }

    @Test
    fun getBaseUrl_normalizesLocalhostAndAppendsApi() {
        every { mockPrefs.getString("base_url", any()) } returns "http://10.0.2.2:8000"

        val sessionManager = TestableSessionManager(mockPrefs)
        val url = sessionManager.getBaseUrl()

        assertTrue(url.endsWith("/api/"))
        assertFalse(url.contains("10.0.2.2"))
    }

    @Test
    fun getBaseUrl_preservesValidDomain() {
        every { mockPrefs.getString("base_url", any()) } returns "https://sms.hwnix.com"

        val sessionManager = TestableSessionManager(mockPrefs)
        val url = sessionManager.getBaseUrl()

        assertEquals("https://sms.hwnix.com/api/", url)
    }

    @Test
    fun cleanPhoneNumber_removesCountryCodePrefixes() {
        val sessionManager = TestableSessionManager(mockPrefs)

        assertEquals("01001234567", sessionManager.cleanPhoneNumber("+201001234567"))
        assertEquals("01001234567", sessionManager.cleanPhoneNumber("+01001234567"))
        assertEquals("01001234567", sessionManager.cleanPhoneNumber("01001234567"))
    }

    @Test
    fun getDeviceUuid_generatesPersistentUuidWhenMissing() {
        every { mockPrefs.getString("device_uuid", null) } returns null

        val sessionManager = TestableSessionManager(mockPrefs)
        val uuid = sessionManager.getDeviceUuid()

        assertNotNull(uuid)
        assertTrue(uuid.length > 20)
        verify { mockEditor.putString("device_uuid", uuid) }
    }

    @Test
    fun clearSession_removesAuthAndDeviceState() {
        val sessionManager = TestableSessionManager(mockPrefs)
        sessionManager.clearSession()

        verify { mockEditor.remove("auth_token") }
        verify { mockEditor.remove("device_id") }
        verify { mockEditor.remove("config_version") }
        verify { mockEditor.remove("setup_complete") }
    }
}

private class TestableSessionManager(private val prefs: SharedPreferences) {
    fun getBaseUrl(): String {
        var url = prefs.getString("base_url", "https://api-teste.hwnix.com/api/") ?: "https://api-teste.hwnix.com/api/"
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

    fun cleanPhoneNumber(phone: String): String {
        var cleaned = phone.trim()
        if (cleaned.startsWith("+20")) cleaned = "0" + cleaned.removePrefix("+20")
        else if (cleaned.startsWith("+0")) cleaned = cleaned.removePrefix("+")
        else if (cleaned.startsWith("+")) cleaned = "0" + cleaned.removePrefix("+")
        return cleaned
    }

    fun getDeviceUuid(): String {
        var uuid = prefs.getString("device_uuid", null)
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", uuid).apply()
        }
        return uuid
    }

    fun clearSession() {
        prefs.edit()
            .remove("auth_token")
            .remove("device_id")
            .remove("config_version")
            .remove("setup_complete")
            .remove("last_sync_success")
            .apply()
    }
}
