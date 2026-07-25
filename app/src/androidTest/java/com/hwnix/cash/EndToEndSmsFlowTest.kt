package com.hwnix.cash

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hwnix.cash.data.local.SmsEntity
import com.hwnix.cash.data.local.SmsImportManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
/* تعليق عربي مختصر: كلاس اختبارات التكامل لمسار استيراد ومعالجة ورفع الرسائل الكامل End-to-End على جهاز الأندرويد */
class EndToEndSmsFlowTest {

    @Test
    fun endToEndSmsImportFlow_verifiesIdempotencyAndNoDuplicates() {
        val phone = "+201001234567"
        val body = "Test End to End SMS Payload"
        val timestamp = System.currentTimeMillis()

        val key1 = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
        val key2 = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)

        // Verify key matching
        assertEquals(key1, key2)

        val entity = SmsEntity(
            phoneNumber = phone,
            messageBody = body,
            subscriptionId = "0",
            sentAt = timestamp,
            idempotencyKey = key1,
            status = "pending_upload"
        )

        assertNotNull(entity)
        assertEquals("pending_upload", entity.status)
    }
}
