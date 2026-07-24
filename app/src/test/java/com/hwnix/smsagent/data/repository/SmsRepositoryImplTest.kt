package com.hwnix.smsagent.data.repository

import com.google.gson.JsonObject
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.local.SmsDao
import com.hwnix.smsagent.data.local.SmsEntity
import com.hwnix.smsagent.data.remote.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة المحسن لـ SmsRepositoryImpl لمنح تغطية 100% لكافة دوال المزامنة والرفع */
class SmsRepositoryImplTest {

    private lateinit var mockApiService: ApiService
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockSmsDao: SmsDao
    private lateinit var repository: SmsRepositoryImpl

    @Before
    fun setUp() {
        mockApiService = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockSmsDao = mockk(relaxed = true)
        repository = SmsRepositoryImpl(mockApiService, mockSessionManager, mockSmsDao)
    }

    @Test
    fun insertSms_delegatesToSmsDao() = runTest {
        val sms = SmsEntity(
            phoneNumber = "01001234567",
            messageBody = "Repository Test",
            direction = "incoming",
            status = "pending_upload",
            messageRef = "REF-001",
            subscriptionId = "0",
            sentAt = 1000000L
        )
        coEvery { mockSmsDao.insert(sms) } returns 1L

        val result = repository.insertSms(sms)

        assertEquals(1L, result)
        coVerify { mockSmsDao.insert(sms) }
    }

    @Test
    fun updateSms_delegatesToSmsDao() = runTest {
        val sms = SmsEntity(
            id = 10L,
            phoneNumber = "01001234567",
            messageBody = "Repository Test",
            direction = "incoming",
            status = "uploaded",
            messageRef = "REF-001",
            subscriptionId = "0",
            sentAt = 1000000L
        )

        repository.updateSms(sms)

        coVerify { mockSmsDao.update(sms) }
    }

    @Test
    fun getPendingUploads_returnsPendingListFromDao() = runTest {
        val list = listOf(
            SmsEntity(
                phoneNumber = "01001234567",
                messageBody = "Pending Msg",
                direction = "incoming",
                status = "pending_upload",
                messageRef = "REF-002",
                subscriptionId = "0",
                sentAt = 1000000L
            )
        )
        coEvery { mockSmsDao.getPendingUploads() } returns list

        val result = repository.getPendingUploads()

        assertEquals(1, result.size)
        assertEquals("Pending Msg", result[0].messageBody)
        coVerify { mockSmsDao.getPendingUploads() }
    }

    @Test
    fun uploadIncomingSms_delegatesToApiService() = runTest {
        val json = JsonObject()
        coEvery { mockApiService.uploadIncomingSms("key_123", json) } returns Response.success(JsonObject())

        val response = repository.uploadIncomingSms("key_123", json)

        assertNotNull(response)
        coVerify { mockApiService.uploadIncomingSms("key_123", json) }
    }

    @Test
    fun syncSmsStatus_delegatesToApiService() = runTest {
        val json = JsonObject()
        coEvery { mockApiService.syncSmsStatus("key_123", json) } returns Response.success(JsonObject())

        val response = repository.syncSmsStatus("key_123", json)

        assertNotNull(response)
        coVerify { mockApiService.syncSmsStatus("key_123", json) }
    }

    @Test
    fun batchSyncSms_delegatesToApiService() = runTest {
        val json = JsonObject()
        coEvery { mockApiService.batchSyncSms("key_123", json) } returns Response.success(JsonObject())

        val response = repository.batchSyncSms("key_123", json)

        assertNotNull(response)
        coVerify { mockApiService.batchSyncSms("key_123", json) }
    }
}
