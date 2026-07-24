package com.hwnix.smsagent.data.local

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الشامل لـ ServiceHealthMonitor لمنح تغطية عالية لكافة الفروع والمسارات الزمنية */
class ServiceHealthMonitorTest {

    @Before
    fun setUp() {
        ServiceHealthMonitor.updateHealth(
            isServiceRunning = true,
            isForegroundActive = true,
            isSyncLoopRunning = true,
            isInternetAvailable = true,
            consecutiveFailures = 0,
            lastSuccessfulSyncTime = System.currentTimeMillis()
        )
    }

    @Test
    fun getHealth_returnsHealthyStateInitially() {
        val health = ServiceHealthMonitor.getHealth()

        assertTrue(health.isServiceRunning)
        assertTrue(health.isForegroundActive)
        assertEquals(ServiceHealthState.HEALTHY, health.overallHealth)
    }

    @Test
    fun updateHealth_whenServiceNotRunning_becomesBroken() {
        ServiceHealthMonitor.updateHealth(isServiceRunning = false)
        assertEquals(ServiceHealthState.BROKEN, ServiceHealthMonitor.getHealth().overallHealth)
    }

    @Test
    fun updateHealth_whenInternetLost_becomesWarning() {
        ServiceHealthMonitor.updateHealth(isInternetAvailable = false)

        val health = ServiceHealthMonitor.getHealth()
        assertEquals(ServiceHealthState.WARNING, health.overallHealth)
        assertTrue(health.statusMessage.contains("المزامنة متوقفة"))
    }

    @Test
    fun updateHealth_whenFailuresEscalate_degradesThenBreaks() {
        ServiceHealthMonitor.updateHealth(consecutiveFailures = 1)
        assertEquals(ServiceHealthState.DEGRADED, ServiceHealthMonitor.getHealth().overallHealth)

        ServiceHealthMonitor.updateHealth(consecutiveFailures = 3)
        assertEquals(ServiceHealthState.BROKEN, ServiceHealthMonitor.getHealth().overallHealth)
    }

    @Test
    fun recordSuccessfulSync_resetsFailuresAndUpdateTimes() {
        ServiceHealthMonitor.updateHealth(consecutiveFailures = 2)
        ServiceHealthMonitor.recordSuccessfulSync()

        val health = ServiceHealthMonitor.getHealth()
        assertEquals(0, health.consecutiveFailures)
        assertTrue(health.lastSuccessfulSyncTime > 0)
    }

    @Test
    fun recordHeartbeat_updatesLastHeartbeatTime() {
        val before = System.currentTimeMillis()
        ServiceHealthMonitor.recordHeartbeat()

        assertTrue(ServiceHealthMonitor.getHealth().lastHeartbeatTime >= before)
    }

    @Test
    fun recordFailure_incrementsConsecutiveFailures() {
        val initialFailures = ServiceHealthMonitor.getHealth().consecutiveFailures
        ServiceHealthMonitor.recordFailure("Timeout Exception")

        assertEquals(initialFailures + 1, ServiceHealthMonitor.getHealth().consecutiveFailures)
        assertTrue(ServiceHealthMonitor.getHealth().reasonForLastStateChange.contains("Timeout Exception"))
    }

    @Test
    fun recordRecovery_incrementsRecoveryCountAndResetsFailures() {
        ServiceHealthMonitor.recordFailure("Network Drop")
        val initialRecoveryCount = ServiceHealthMonitor.getHealth().recoveryCount

        ServiceHealthMonitor.recordRecovery("Reconnected")

        val health = ServiceHealthMonitor.getHealth()
        assertEquals(initialRecoveryCount + 1, health.recoveryCount)
        assertEquals(0, health.consecutiveFailures)
    }

    @Test
    fun formatTime_formatsTimestampOrReturnsNever() {
        assertEquals("لم تتم بعد", ServiceHealthMonitor.formatTime(0))
        assertNotEquals("لم تتم بعد", ServiceHealthMonitor.formatTime(1784868257000L))
    }

    @Test
    fun updateHealth_timeSinceLastSync_transitionsToDegradedAndWarning() {
        val oldSync = System.currentTimeMillis() - 100000L // 100s ago (> 90s)
        ServiceHealthMonitor.updateHealth(lastSuccessfulSyncTime = oldSync)
        assertEquals(ServiceHealthState.DEGRADED, ServiceHealthMonitor.getHealth().overallHealth)

        val veryOldSync = System.currentTimeMillis() - 200000L // 200s ago (> 180s)
        ServiceHealthMonitor.updateHealth(lastSuccessfulSyncTime = veryOldSync)
        assertEquals(ServiceHealthState.WARNING, ServiceHealthMonitor.getHealth().overallHealth)
    }
}
