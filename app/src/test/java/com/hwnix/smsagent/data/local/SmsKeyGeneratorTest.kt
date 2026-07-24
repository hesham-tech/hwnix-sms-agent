package com.hwnix.smsagent.data.local

import org.junit.Assert.*
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بتسوية الأرقام والترميز وتوليد مفاتيح التفرّد للرسائل العربية ورموز الإيموجي */
class SmsKeyGeneratorTest {

    @Test
    fun generateIdempotencyKey_handlesArabicText() {
        val time = 1784868257000L
        val textArabic1 = "تم استلام مبلغ 150 جنيه بنجاح"
        val textArabic2 = "تم استلام مبلغ 150 جنيه بنجاح"

        val key1 = SmsImportManager.generateIdempotencyKey("01001234567", time, "0", textArabic1)
        val key2 = SmsImportManager.generateIdempotencyKey("01001234567", time, "0", textArabic2)

        assertEquals(key1, key2)
        assertTrue(key1.startsWith("key_001234567_"))
    }

    @Test
    fun generateIdempotencyKey_handlesEmojisAndSpecialCharacters() {
        val time = 1784868257000L
        val textEmoji = "OTP Verification: 🔑 4920 !@#$%^&*()"

        val key1 = SmsImportManager.generateIdempotencyKey("+20-100-123-4567", time, "0", textEmoji)
        val key2 = SmsImportManager.generateIdempotencyKey("01001234567", time, "0", textEmoji)

        assertEquals(key1, key2)
    }

    @Test
    fun generateIdempotencyKey_trimsWhitespaceBeforeHashing() {
        val time = 1784868257000L
        val text1 = "  Code 8849  \n"
        val text2 = "Code 8849"

        val key1 = SmsImportManager.generateIdempotencyKey("01001234567", time, "0", text1)
        val key2 = SmsImportManager.generateIdempotencyKey("01001234567", time, "0", text2)

        assertEquals(key1, key2)
    }

    @Test
    fun generateIdempotencyKey_handlesShortPhoneNumbers() {
        val time = 1784868257000L
        val shortNumber = "7700" // Service center short code

        val key = SmsImportManager.generateIdempotencyKey(shortNumber, time, "0", "Alert Message")

        assertTrue(key.contains("_7700_"))
    }
}
