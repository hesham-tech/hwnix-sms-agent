package com.hwnix.smsagent.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwnix.smsagent.presentation.status.StatusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusUiState,
    onRefresh: () -> Unit,
    onSyncNowClick: () -> Unit,
    onSimSetupClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBatteryOptimizeClick: () -> Unit,
    onAutostartClick: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
        // بطاقة معلومات الجهاز
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "حالة الاتصال: ${state.connectionStatus}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "معرف الجهاز (ID): ${state.deviceId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الـ UUID: ${state.deviceUuid}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "إصدار الإعدادات: ${state.configVersion}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.gatewayName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اسم الجهاز: ${state.gatewayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // بطاقة حالة تحسين البطارية (ألوان جذابة وغير باهتة)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isBatteryOptimized)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.isBatteryOptimized) Icons.Filled.BatteryAlert else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (state.isBatteryOptimized)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isBatteryOptimized) "تحسين البطارية: مقيّد ⚠️" else "تحسين البطارية: مُعطَّل ✅",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.isBatteryOptimized)
                            "قد يؤثر على عمل الخدمة في الخلفية"
                        else
                            "الخدمة تعمل بكامل طاقتها في الخلفية",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.isBatteryOptimized) {
                    TextButton(onClick = onBatteryOptimizeClick) {
                        Text("إعداد")
                    }
                }
            }
        }

        if (state.isAutostartAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التشغيل التلقائي (Autostart) ⚠️",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يرجى تفعيل التشغيل التلقائي بعد إعادة التشغيل لضمان استقرار الخدمة.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = onAutostartClick) {
                        Text("إعداد")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // الأزرار التفاعلية
        Button(
            onClick = onSyncNowClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("مزامنة فورية الآن")
        }

        OutlinedButton(
            onClick = onSimSetupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SimCard,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("إعدادات الجهاز والأرقام")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLogoutClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل الخروج وإلغاء الربط")
        }
    }
}
}
