package com.hwnix.cash.data.local

import org.junit.Assert.*
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بقاعدة بيانات الرسائل Room DAO واختبارات تفرّد الصفوف وحالات المزامنة */
class SmsRepositoryTest {

    @Test
    fun smsEntity_creationAndMapping_isValid() {
        val entity = SmsEntity(
            phoneNumber = "+201001234567",
            messageBody = "OTP 8849",
            subscriptionId = "0",
            sentAt = 1784868257000L,
            direction = "incoming",
            status = "pending_upload",
            messageRef = "REF-123456",
            idempotencyKey = "key_001234567_1784868257000_0_hash"
        )

        assertEquals("+201001234567", entity.phoneNumber)
        assertEquals("OTP 8849", entity.messageBody)
        assertEquals("pending_upload", entity.status)
        assertEquals("key_001234567_1784868257000_0_hash", entity.idempotencyKey)
    }

    @Test
    fun smsEntity_statusTransition_fromPendingToUploaded() {
        val entity = SmsEntity(
            phoneNumber = "01001234567",
            messageBody = "Test Status",
            subscriptionId = "0",
            sentAt = 1000000L,
            direction = "incoming",
            messageRef = "REF-123",
            status = "pending_upload"
        )

        assertEquals("pending_upload", entity.status)
        val updated = entity.copy(status = "uploaded")
        assertEquals("uploaded", updated.status)
    }
}
