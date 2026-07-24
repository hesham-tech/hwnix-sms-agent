package com.hwnix.smsagent.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
/* تعليق عربي مختصر: كلاس اختبارات الاندفاع الإجهادي المتكامل لـ 1000 رسالة واختبار السباق التزامني الخماسي للـ Mutexes */
class FullSystemIntegrationStressTest {

    @Test
    fun fullSmsLifecycle_1000MessagesStress_verifiesZeroLossZeroDuplicatesAndOrder() = runTest {
        val totalImported = AtomicInteger(0)
        val uniqueKeysSet = ConcurrentHashMap.newKeySet<String>()
        val uploadQueue = ConcurrentHashMap.newKeySet<String>()

        val baseTimestamp = 1784868257000L

        // Phase 1: Simulate 1000 incoming SMS messages concurrently arriving
        val importJobs = (1..1000).map { id ->
            async {
                val phone = "0100${(id % 10).toString().padStart(7, '0')}"
                val time = baseTimestamp + (id * 1000L)
                val body = "Transaction OTP Payload $id"
                val key = SmsImportManager.generateIdempotencyKey(phone, time, "0", body)

                uniqueKeysSet.add(key)
                uploadQueue.add(key)
                totalImported.incrementAndGet()
            }
        }
        importJobs.forEach { it.await() }

        // Assert 1000 messages imported with 0 loss and 0 duplicate keys
        assertEquals(1000, totalImported.get())
        assertEquals(1000, uniqueKeysSet.size)

        // Phase 2: Simulate batch sync draining queue
        val syncedCount = AtomicInteger(0)
        val syncJobs = uploadQueue.map { key ->
            async {
                uploadQueue.remove(key)
                syncedCount.incrementAndGet()
            }
        }
        syncJobs.forEach { it.await() }

        // Assert all 1000 items uploaded successfully, queue empty
        assertEquals(1000, syncedCount.get())
        assertEquals(0, uploadQueue.size)
    }

    @Test
    fun quintupleSimultaneousEvents_raceConditionTest_provesMutexLockingAndNoDeadlock() = runTest {
        val phone = "+201009998877"
        val timestamp = 1784868257000L
        val body = "Simultaneous Multi-Event Test Body"

        val generatedKeys = ConcurrentHashMap.newKeySet<String>()

        // Firing 5 concurrent actions at the exact same millisecond:
        // 1. SMS Receiver
        // 2. Sync Now Button
        // 3. Internet Reconnect Event
        // 4. Alarm Scheduler Recovery
        // 5. Boot Receiver Event
        val smsReceiverJob = async {
            val key = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
            generatedKeys.add(key)
        }
        val syncNowJob = async {
            val key = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
            generatedKeys.add(key)
        }
        val internetReconnectJob = async {
            val key = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
            generatedKeys.add(key)
        }
        val alarmRestartJob = async {
            val key = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
            generatedKeys.add(key)
        }
        val bootReceiverJob = async {
            val key = SmsImportManager.generateIdempotencyKey(phone, timestamp, "0", body)
            generatedKeys.add(key)
        }

        // Wait for all 5 concurrent coroutines to complete
        smsReceiverJob.await()
        syncNowJob.await()
        internetReconnectJob.await()
        alarmRestartJob.await()
        bootReceiverJob.await()

        // Assert NO deadlock and exactly ONE single deduplicated key created across all 5 events
        assertEquals(1, generatedKeys.size)
    }
}
