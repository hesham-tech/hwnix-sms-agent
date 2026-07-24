package com.hwnix.smsagent.data.local

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بمراقب الاتصال وملاحظات الشبكة وتحديث حالة النظام فوراً عند انقطاع وعودة الإنترنت */
class ConnectivityMonitorTest {

    @Before
    fun setUp() {
        ServiceHealthMonitor.updateHealth(
            isInternetAvailable = true,
            reason = "Test Setup"
        )
    }

    @Test
    fun onNetworkLost_updatesHealthToWarningAndOffline() {
        // Simulate network lost event
        ServiceHealthMonitor.updateHealth(
            isInternetAvailable = false,
            reason = "Network Lost (Callback)"
        )

        val health = ServiceHealthMonitor.getHealth()
        assertFalse(health.isInternetAvailable)
        assertEquals(ServiceHealthState.WARNING, health.overallHealth)
        assertTrue(health.statusMessage.contains("لا يوجد اتصال بالإنترنت"))
    }

    @Test
    fun onNetworkAvailable_updatesHealthToHealthy() {
        // First disconnect
        ServiceHealthMonitor.updateHealth(
            isInternetAvailable = false,
            reason = "Network Lost"
        )

        // Then restore
        ServiceHealthMonitor.updateHealth(
            isInternetAvailable = true,
            lastSuccessfulSyncTime = System.currentTimeMillis(),
            isServiceRunning = true,
            isForegroundActive = true,
            isSyncLoopRunning = true,
            consecutiveFailures = 0,
            reason = "Network Restored"
        )

        val health = ServiceHealthMonitor.getHealth()
        assertTrue(health.isInternetAvailable)
        assertEquals(ServiceHealthState.HEALTHY, health.overallHealth)
    }
}
