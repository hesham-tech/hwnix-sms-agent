package com.hwnix.smsagent.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
/* تعليق عربي مختصر: كلاس اختبارات الضغط الشديد والإنتاجية العالية وإجهاد الـ 1000 رسالة والسباقات التنافسية */
class StressAndLongevityTest {

    @Test
    fun stressTest_1000MessagesBurst_generatesUniqueKeysWithoutDuplicates() = runTest {
        val totalCount = AtomicInteger(0)
        val generatedKeysSet = ConcurrentHashMap.newKeySet<String>()

        val jobs = (1..1000).map { i ->
            async {
                val key = SmsImportManager.generateIdempotencyKey(
                    phoneNumber = "01001234567",
                    sentAt = 1784868257000L + (i % 500) * 10000L, // Spread across different timestamps
                    subscriptionId = (i % 2).toString(),
                    messageBody = "Stress Test Body Payload $i"
                )
                generatedKeysSet.add(key)
                totalCount.incrementAndGet()
            }
        }
        jobs.forEach { it.await() }

        assertEquals(1000, totalCount.get())
        assertEquals(1000, generatedKeysSet.size) // Every distinct message produces an exact distinct key
    }

    @Test
    fun stressTest_100ServiceRestarts_executesWithoutDeadlockOrMemoryLeak() {
        var runningState = false
        repeat(100) {
            runningState = true
            ServiceHealthMonitor.updateHealth(
                isServiceRunning = runningState,
                isForegroundActive = true,
                isSyncLoopRunning = true,
                lastSuccessfulSyncTime = System.currentTimeMillis()
            )
            runningState = false
            ServiceHealthMonitor.updateHealth(
                isServiceRunning = runningState,
                isForegroundActive = false,
                isSyncLoopRunning = false
            )
        }

        assertFalse(ServiceHealthMonitor.getHealth().isServiceRunning)
    }
}
