package com.hwnix.cash.data.service

import com.hwnix.cash.data.local.ServiceHealthState
import org.junit.Assert.*
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بمدير ومراقب صحة الإشعارات وحساب النصوص والألوان بحسب حالة الاتصال */
class NotificationManagerTest {

    @Test
    fun getHealthState_returnsCorrectColorsAndIcons() {
        assertEquals("🟢", ServiceHealthState.HEALTHY.icon)
        assertEquals("🟠", ServiceHealthState.WARNING.icon)
        assertEquals("🔴", ServiceHealthState.BROKEN.icon)

        assertEquals(0xFF2E7D32.toInt(), ServiceHealthState.HEALTHY.colorHex)
        assertEquals(0xFFE65100.toInt(), ServiceHealthState.WARNING.colorHex)
        assertEquals(0xFFC62828.toInt(), ServiceHealthState.BROKEN.colorHex)
    }
}
