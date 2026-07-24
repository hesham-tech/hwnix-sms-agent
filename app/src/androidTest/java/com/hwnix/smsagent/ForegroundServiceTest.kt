package com.hwnix.smsagent

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hwnix.smsagent.data.local.ServiceHealthMonitor
import com.hwnix.smsagent.data.local.ServiceHealthState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
/* تعليق عربي مختصر: كلاس اختبارات الأداء التشغيلي لدورة حياة الخدمة الفعالة والإشعارات وحالة الاتصال بالأندرويد */
class ForegroundServiceTest {

    @Test
    fun serviceLifecycle_updatesHealthStateCorrectly() {
        ServiceHealthMonitor.updateHealth(
            isServiceRunning = true,
            isForegroundActive = true,
            isSyncLoopRunning = true,
            isInternetAvailable = true,
            lastSuccessfulSyncTime = System.currentTimeMillis()
        )

        val health = ServiceHealthMonitor.getHealth()
        assertEquals(ServiceHealthState.HEALTHY, health.overallHealth)
        assertTrue(health.isServiceRunning)
        assertTrue(health.isForegroundActive)
    }
}
