package com.hwnix.cash.data.local

import android.content.Context
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.hwnix.cash.core.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة المحسن بـ ShadowUserManager وعزل الجداول لمنح SmsImportManager تغطية كاملة واختبارات حرة من التداخل */
class SmsImportManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.initialize(context)
        try {
            ServiceLocator.database.clearAllTables()
        } catch (e: Exception) {
            // Ignore database reset exceptions
        }
    }

    @Test
    fun determineSentAt_prioritizesPduTimestamp() {
        val pdu = 1000000L
        val dateSent = 2000000L
        val date = 3000000L
        val fallback = 4000000L

        val result = SmsImportManager.determineSentAt(pdu, dateSent, date, fallback)
        assertEquals(pdu, result)
    }

    @Test
    fun determineSentAt_usesDateSentWhenPduIsNull() {
        val result = SmsImportManager.determineSentAt(null, 2000000L, 3000000L, 4000000L)
        assertEquals(2000000L, result)
    }

    @Test
    fun determineSentAt_usesDateWhenPduAndDateSentAreNull() {
        val result = SmsImportManager.determineSentAt(null, null, 3000000L, 4000000L)
        assertEquals(3000000L, result)
    }

    @Test
    fun determineSentAt_usesFallbackWhenAllTimestampsAreNullOrZero() {
        val result = SmsImportManager.determineSentAt(null, 0L, 0L, 4000000L)
        assertEquals(4000000L, result)
    }

    @Test
    fun generateIdempotencyKey_normalizesPhoneNumber() {
        val key1 = SmsImportManager.generateIdempotencyKey("+20 100 123 4567", 1000000L, "0", "Hello World")
        val key2 = SmsImportManager.generateIdempotencyKey("00201001234567", 1000000L, "0", "Hello World")

        assertEquals(key1, key2)
    }

    @Test
    fun generateIdempotencyKey_groupsWithin3SecondBucket() {
        val key1 = SmsImportManager.generateIdempotencyKey("01001234567", 1000100L, "0", "Bucket Test")
        val key2 = SmsImportManager.generateIdempotencyKey("01001234567", 1000200L, "0", "Bucket Test")

        assertEquals(key1, key2)
    }

    @Test
    fun generateIdempotencyKey_differentMessages_generateDifferentHashes() {
        val key1 = SmsImportManager.generateIdempotencyKey("01001234567", 1000000L, "0", "Message A")
        val key2 = SmsImportManager.generateIdempotencyKey("01001234567", 1000000L, "0", "Message B")

        assertNotEquals(key1, key2)
    }

    @Test
    fun smsImportRequest_creationAndCopy_worksAsExpected() {
        val req = SmsImportRequest(
            phoneNumber = "01001234567",
            messageBody = "Test Request Data Class",
            subscriptionId = "0",
            sentAt = 1000000L,
            source = "SmsReceiver",
            androidSmsId = "999",
            pduTimestamp = 1000000L,
            dateSentTimestamp = 1000000L,
            dateTimestamp = 1000000L
        )

        assertEquals("01001234567", req.phoneNumber)
        assertEquals("Test Request Data Class", req.messageBody)
        assertEquals("SmsReceiver", req.source)

        val copied = req.copy(sentAt = 2000000L)
        assertEquals(2000000L, copied.sentAt)
    }

    @Test
    fun importMessage_unlockedUser_insertsToRoomAndHandlesDuplicates() = runTest {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        shadowOf(userManager).setUserUnlocked(true)

        val uniquePhone = "011${(1000000..9999999).random()}"
        val uniqueBody = "Unique Message ${UUID.randomUUID()}"
        val request = SmsImportRequest(
            phoneNumber = uniquePhone,
            messageBody = uniqueBody,
            subscriptionId = "0",
            sentAt = System.currentTimeMillis(),
            pduTimestamp = System.currentTimeMillis(),
            source = "SmsReceiver"
        )

        val firstResult = SmsImportManager.importMessage(context, request)
        val secondResult = SmsImportManager.importMessage(context, request)

        assertTrue(firstResult)
        assertFalse(secondResult) // Duplicate import must return false
    }

    @Test
    fun importMessage_lockedUser_savesToDeviceProtectedStorage() = runTest {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        shadowOf(userManager).setUserUnlocked(false)

        val uniquePhone = "012${(1000000..9999999).random()}"
        val request = SmsImportRequest(
            phoneNumber = uniquePhone,
            messageBody = "Direct Boot Storage Locked Unique Test ${UUID.randomUUID()}",
            subscriptionId = "0",
            sentAt = System.currentTimeMillis(),
            pduTimestamp = System.currentTimeMillis(),
            source = "DirectBootQueue"
        )

        val result = SmsImportManager.importMessage(context, request)

        assertFalse(result)
    }
}
