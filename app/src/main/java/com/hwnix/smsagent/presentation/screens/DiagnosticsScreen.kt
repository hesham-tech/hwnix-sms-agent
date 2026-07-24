package com.hwnix.smsagent.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwnix.smsagent.data.local.BootTracker

/* تعليق عربي مختصر: شاشة تشخيصات مستقلة تعرض التقرير التفصيلي مع أزرار نسخ ومسح ثابته في أعلى الشاشة */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var bootDiagnostics by remember { mutableStateOf(BootTracker.getDiagnostics(context)) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 📌 الشريط العلوي الثابت للأزرار (لا يتأثر بالتمرير)
        Surface(
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("HWNix Diagnostics", BootTracker.exportFormattedDiagnostics(context))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ التقرير الحافظة ✅", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ التقرير")
                }

                Row {
                    IconButton(
                        onClick = {
                            bootDiagnostics = BootTracker.getDiagnostics(context)
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedButton(
                        onClick = {
                            BootTracker.clearLog(context)
                            bootDiagnostics = BootTracker.getDiagnostics(context)
                            Toast.makeText(context, "تم مسح السجل بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح السجل")
                    }
                }
            }
        }

        // 📜 محتوى التقرير القابل للتمرير
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "تقرير تشخيصات النظام والإقلاع (Diagnostics)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 0. SERVICE HEALTH LIVE CARD
            val health = com.hwnix.smsagent.data.local.ServiceHealthMonitor.getHealth()
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (health.overallHealth) {
                        com.hwnix.smsagent.data.local.ServiceHealthState.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                        com.hwnix.smsagent.data.local.ServiceHealthState.DEGRADED, com.hwnix.smsagent.data.local.ServiceHealthState.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صحة الخدمة الحية (SERVICE HEALTH)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${health.overallHealth.icon} ${health.overallHealth.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• حالة النظام الحالية: ${health.statusMessage}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("• الخدمة تعمل (Running): ${if (health.isServiceRunning) "نعم ✅" else "لا 🔴"}", style = MaterialTheme.typography.bodySmall)
                    Text("• الوضع الأمامي (Foreground): ${if (health.isForegroundActive) "نشط ✅" else "معطل 🔴"}", style = MaterialTheme.typography.bodySmall)
                    Text("• حلقة العمل (Sync Loop): ${if (health.isSyncLoopRunning) "تعمل ✅" else "متوقفة 🔴"}", style = MaterialTheme.typography.bodySmall)
                    Text("• آخر مزامنة ناجحة: ${com.hwnix.smsagent.data.local.ServiceHealthMonitor.formatTime(health.lastSuccessfulSyncTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("• آخر نبضة قلب (Heartbeat): ${com.hwnix.smsagent.data.local.ServiceHealthMonitor.formatTime(health.lastHeartbeatTime)}", style = MaterialTheme.typography.bodySmall)
                    Text("• عدد الأخطاء المتتالية: ${health.consecutiveFailures}", style = MaterialTheme.typography.bodySmall)
                    Text("• عدد مرات التعافي الذاتي: ${health.recoveryCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• سبب آخر تغيير للحالة: ${health.reasonForLastStateChange}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            // 1. معلومات العتاد والنظام
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("معلومات النظام والعتاد:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• الجهاز: ${bootDiagnostics["system_manufacturer"]} - ${bootDiagnostics["system_model"]}", style = MaterialTheme.typography.bodyMedium)
                    Text("• إصدار أندرويد: ${bootDiagnostics["system_android_version"]} (SDK ${bootDiagnostics["system_sdk"]})", style = MaterialTheme.typography.bodyMedium)
                    Text("• تم استثناء البطارية: ${if (bootDiagnostics["system_battery_ignored"] == true) "نعم ✅" else "لا ⚠️"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• فك القفل (Direct Boot): ${if (bootDiagnostics["system_unlocked"] == true) "نعم ✅" else "لا ⚠️"}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 2. Boot Receiver Tripwire
            val tripwire = bootDiagnostics["tripwire"] as? String ?: ""
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (tripwire.contains("BOOT_RECEIVER_CALLED"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Boot Receiver Tripwire (الدليل القاطع):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(tripwire, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            // 3. حالة الإقلاع والـ Timeline
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("سجل الإقلاع (Boot History):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• عدد الإقلاعات: ${bootDiagnostics["boot_count"]}", style = MaterialTheme.typography.bodyMedium)
                    Text("• آخر إشارة (Action): ${bootDiagnostics["last_action"]}", style = MaterialTheme.typography.bodyMedium)
                    Text("• آخر مرحلة: ${bootDiagnostics["last_stage"]}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("• توقيت آخر إقلاع: ${bootDiagnostics["last_timestamp"]}", style = MaterialTheme.typography.bodySmall)

                    val stageLog = bootDiagnostics["stage_log"] as? String ?: ""
                    if (stageLog.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("الخطوات التفصيلية (Timeline):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stageLog, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 4. السجلات والـ Exception Traces
            val exceptionTrace = bootDiagnostics["exception_trace"] as? String ?: ""
            if (exceptionTrace.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الأخطاء والكراشات الفعالية (Exception Trace):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(exceptionTrace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
