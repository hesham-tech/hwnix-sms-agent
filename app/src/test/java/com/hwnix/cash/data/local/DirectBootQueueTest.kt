package com.hwnix.cash.data.local

import org.junit.Assert.*
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة لمعالجة تحويل وقراءة رسائل وضع Direct Boot والتأكد من تحويل JSON لـ SmsEntity */
class DirectBootQueueTest {

    @Test
    fun parseDirectBootJson_validJson_returnsSmsEntity() {
        val json = """
            {
                "phoneNumber": "+201001234567",
                "messageBody": "Test Direct Boot Payload",
                "subscriptionId": "1",
                "sentAt": 1784868257000
            }
        """.trimIndent()

        val entity = DirectBootQueueManager.parseDirectBootJson(json)

        assertNotNull(entity)
        assertEquals("+201001234567", entity?.phoneNumber)
        assertEquals("Test Direct Boot Payload", entity?.messageBody)
        assertEquals("1", entity?.subscriptionId)
        assertEquals(1784868257000L, entity?.sentAt)
        assertEquals("pending_upload", entity?.status)
    }

    @Test
    fun parseDirectBootJson_invalidJson_returnsNull() {
        val invalidJson = "{ malformed json }"

        val entity = DirectBootQueueManager.parseDirectBootJson(invalidJson)

        assertNull(entity)
    }
}
