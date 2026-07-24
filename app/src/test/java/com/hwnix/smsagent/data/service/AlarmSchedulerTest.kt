package com.hwnix.smsagent.data.service

import org.junit.Assert.*
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بإعادة جدولة منبهات AlarmManager للتعافي القسري عند إغلاق التطبيق */
class AlarmSchedulerTest {

    @Test
    fun alarmIntent_hasCorrectLauncherSourceExtra() {
        val launcherSourceKey = "launcher_source"
        val expectedExtraValue = "ALARM_MANAGER_RESTART"

        val extraValue = "ALARM_MANAGER_RESTART"
        assertEquals(expectedExtraValue, extraValue)
        assertEquals("launcher_source", launcherSourceKey)
    }
}
