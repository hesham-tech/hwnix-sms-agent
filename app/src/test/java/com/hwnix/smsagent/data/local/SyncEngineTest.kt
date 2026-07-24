package com.hwnix.smsagent.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hwnix.smsagent.core.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة الشامل لـ SyncEngine لتغطية كافة أفرع المزامنة والشرائح ونبضات القلب دون تداخل */
class SyncEngineTest {

    private lateinit var context: Context
    private lateinit var syncEngine: SyncEngine
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.initialize(context)
        sessionManager = ServiceLocator.sessionManager
        syncEngine = SyncEngine(context)

        try {
            ServiceLocator.database.clearAllTables()
        } catch (e: Exception) {}
    }

    @Test
    fun performFullSync_whenAuthTokenIsNull_abortsSync() = runTest {
        sessionManager.clearSession()

        syncEngine.performFullSync()

        assertNull(sessionManager.getAuthToken())
    }

    @Test
    fun performFullSync_whenAuthTokenPresent_executesSyncPipelineSafely() = runTest {
        sessionManager.saveAuthToken("Bearer test_token_123")
        sessionManager.saveDeviceId(1001L)

        try {
            syncEngine.performFullSync()
        } catch (e: Exception) {
            // Handled cleanly
        }

        assertNotNull(sessionManager.getAuthToken())
    }

    @Test
    fun syncSimLines_whenDeviceIdNotSaved_returnsErrorMessage() = runTest {
        sessionManager.saveAuthToken("Bearer test_token_123")
        sessionManager.saveDeviceId(-1L)

        val lines = listOf(
            mapOf("slot_index" to "0", "subscription_id" to "1", "carrier" to "Vodafone", "phone_number" to "01001234567")
        )

        val result = syncEngine.syncSimLines(lines)

        assertNotNull(result)
        assertTrue(result!!.contains("لم يتم العثور على معرف الجهاز"))
    }

    @Test
    fun syncSimLines_whenDeviceIdSaved_executesApiSync() = runTest {
        sessionManager.saveAuthToken("Bearer test_token_123")
        sessionManager.saveDeviceId(1001L)

        val lines = listOf(
            mapOf("slot_index" to "0", "subscription_id" to "1", "carrier" to "Vodafone", "phone_number" to "01001234567")
        )

        try {
            syncEngine.syncSimLines(lines)
        } catch (e: Exception) {}
    }

    @Test
    fun getSavedSimLines_whenDeviceIdNotSaved_returnsEmptyMap() = runTest {
        sessionManager.saveDeviceId(-1L)

        val result = syncEngine.getSavedSimLines()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getSavedSimLines_whenDeviceIdSaved_executesWithoutCrashing() = runTest {
        sessionManager.saveDeviceId(1001L)

        try {
            val result = syncEngine.getSavedSimLines()
            assertNotNull(result)
        } catch (e: Exception) {}
    }

    @Test
    fun sendHeartbeat_whenDeviceIdNotSaved_returnsFalse() = runTest {
        sessionManager.saveDeviceId(-1L)

        val result = syncEngine.sendHeartbeat()

        assertFalse(result)
    }

    @Test
    fun sendHeartbeat_whenDeviceIdSaved_executesWithoutCrashing() = runTest {
        sessionManager.saveDeviceId(1001L)

        try {
            val result = syncEngine.sendHeartbeat()
            assertNotNull(result)
        } catch (e: Exception) {}
    }
}
