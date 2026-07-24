package com.hwnix.smsagent.data.local

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ServiceHealthState(val icon: String, val label: String, val colorHex: Int) {
    STARTING("🔵", "جاري تهيئة النظام...", 0xFF1565C0.toInt()), // أزرق غامق
    HEALTHY("🟢", "الخدمة تعمل بصورة طبيعية", 0xFF2E7D32.toInt()),  // أخضر مريح للعين
    DEGRADED("🟡", "توجد مشكلة ويتم إصلاحها", 0xFFF57F17.toInt()), // أصفر/أمبر
    WARNING("🟠", "تحذير: المزامنة متأخرة", 0xFFE65100.toInt()),  // برتقالي غامق
    BROKEN("🔴", "الخدمة غير مستقرة — جاري التعافي", 0xFFC62828.toInt()) // أحمر مريح
}

data class ServiceHealth(
    val isServiceRunning: Boolean = false,
    val isForegroundActive: Boolean = false,
    val isSyncLoopRunning: Boolean = false,
    val lastSuccessfulSyncTime: Long = 0L,
    val lastHeartbeatTime: Long = 0L,
    val lastPollTime: Long = 0L,
    val consecutiveFailures: Int = 0,
    val recoveryCount: Int = 0,
    val lastRecoveryTime: Long = 0L,
    val overallHealth: ServiceHealthState = ServiceHealthState.STARTING,
    val statusMessage: String = "جاري تهيئة الخدمة...",
    val reasonForLastStateChange: String = "بدء تشغيل النظام"
)

/* تعليق عربي مختصر: مراقب صحة الخدمة والتطبيق لحساب الحالة الحية لحظة بلحظة وتشغيل التعافي الذاتي وتحديث الإشعار */
object ServiceHealthMonitor {

    private const val TAG = "ServiceHealthMonitor"

    @Volatile
    private var currentHealth = ServiceHealth()

    fun getHealth(): ServiceHealth = currentHealth

    @Synchronized
    fun updateHealth(
        isServiceRunning: Boolean? = null,
        isForegroundActive: Boolean? = null,
        isSyncLoopRunning: Boolean? = null,
        lastSuccessfulSyncTime: Long? = null,
        lastHeartbeatTime: Long? = null,
        lastPollTime: Long? = null,
        consecutiveFailures: Int? = null,
        recoveryCountIncrement: Boolean = false,
        reason: String? = null,
        context: Context? = null
    ) {
        val old = currentHealth
        val newFailures = consecutiveFailures ?: old.consecutiveFailures
        val newRecoveryCount = if (recoveryCountIncrement) old.recoveryCount + 1 else old.recoveryCount
        val newRecoveryTime = if (recoveryCountIncrement) System.currentTimeMillis() else old.lastRecoveryTime

        val newServiceRunning = isServiceRunning ?: old.isServiceRunning
        val newForegroundActive = isForegroundActive ?: old.isForegroundActive
        val newSyncLoopRunning = isSyncLoopRunning ?: old.isSyncLoopRunning
        val newLastSync = lastSuccessfulSyncTime ?: old.lastSuccessfulSyncTime
        val newLastHeartbeat = lastHeartbeatTime ?: old.lastHeartbeatTime
        val newLastPoll = lastPollTime ?: old.lastPollTime
        val newReason = reason ?: old.reasonForLastStateChange

        // حساب الحالة الكلية (Overall Health)
        val now = System.currentTimeMillis()
        val timeSinceLastSync = if (newLastSync > 0) (now - newLastSync) / 1000 else 9999L

        val calculatedHealth = when {
            !newServiceRunning || !newForegroundActive -> ServiceHealthState.BROKEN
            !newSyncLoopRunning -> ServiceHealthState.BROKEN
            newFailures >= 3 -> ServiceHealthState.BROKEN
            newFailures >= 1 -> ServiceHealthState.DEGRADED
            timeSinceLastSync > 180 -> ServiceHealthState.WARNING // لم تتم المزامنة لأكثر من 3 دقائق
            timeSinceLastSync > 90 -> ServiceHealthState.DEGRADED // لم تتم المزامنة لأكثر من دقيقة ونصف
            newLastSync > 0 -> ServiceHealthState.HEALTHY
            else -> ServiceHealthState.STARTING
        }

        val formattedMessage = when (calculatedHealth) {
            ServiceHealthState.HEALTHY -> {
                val sec = if (newLastSync > 0) (now - newLastSync) / 1000 else 0
                when {
                    sec < 5 -> "آخر مزامنة تمت للتو"
                    sec < 60 -> "آخر مزامنة منذ $sec ثانية"
                    else -> "آخر مزامنة منذ ${sec / 60} دقيقة"
                }
            }
            ServiceHealthState.DEGRADED -> {
                "تأخر المزامنة (فشل $newFailures) — محاولة استعادة..."
            }
            ServiceHealthState.WARNING -> {
                "تأخر الاستجابة — جاري إعادة الاتصال..."
            }
            ServiceHealthState.BROKEN -> {
                "الخدمة غير مستقرة — جاري التعافي التلقائي..."
            }
            ServiceHealthState.STARTING -> {
                "جاري تهيئة خدمات بوابة الرسائل..."
            }
        }

        currentHealth = ServiceHealth(
            isServiceRunning = newServiceRunning,
            isForegroundActive = newForegroundActive,
            isSyncLoopRunning = newSyncLoopRunning,
            lastSuccessfulSyncTime = newLastSync,
            lastHeartbeatTime = newLastHeartbeat,
            lastPollTime = newLastPoll,
            consecutiveFailures = newFailures,
            recoveryCount = newRecoveryCount,
            lastRecoveryTime = newRecoveryTime,
            overallHealth = calculatedHealth,
            statusMessage = formattedMessage,
            reasonForLastStateChange = newReason
        )

        Log.i(TAG, "Health state updated: ${calculatedHealth.name} | Msg: $formattedMessage | Reason: $newReason")

        if (context != null && old.overallHealth != calculatedHealth) {
            BootTracker.updateStage(context, "HEALTH_CHANGE: ${old.overallHealth.name} ➔ ${calculatedHealth.name} ($newReason)")
        }
    }

    fun recordSuccessfulSync(context: Context? = null) {
        updateHealth(
            lastSuccessfulSyncTime = System.currentTimeMillis(),
            lastPollTime = System.currentTimeMillis(),
            consecutiveFailures = 0,
            reason = "مزامنة ناجحة",
            context = context
        )
    }

    fun recordHeartbeat(context: Context? = null) {
        updateHealth(
            lastHeartbeatTime = System.currentTimeMillis(),
            context = context
        )
    }

    fun recordFailure(reason: String, context: Context? = null) {
        val newFailures = currentHealth.consecutiveFailures + 1
        updateHealth(
            consecutiveFailures = newFailures,
            reason = "فشل: $reason",
            context = context
        )
    }

    fun recordRecovery(reason: String, context: Context? = null) {
        updateHealth(
            recoveryCountIncrement = true,
            consecutiveFailures = 0,
            reason = "تم التعافي التلقائي: $reason",
            context = context
        )
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "لم تتم بعد"
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}
