package com.hwnix.cash.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs
import com.hwnix.cash.data.local.BootTracker
import com.hwnix.cash.data.local.ServiceHealthMonitor
import com.hwnix.cash.manager.oem.OEMAutostartHelper
import com.hwnix.cash.presentation.status.StatusUiState

// الشاشة الرئيسية - تعرض حالة الخدمة والإحصائيات والتحكم
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusUiState,
    onRefresh: () -> Unit,
    onSyncNowClick: () -> Unit,
    onSimSetupClick: () -> Unit,
    onBatteryOptimizeClick: () -> Unit,
    onAddWalletForLineClick: (String) -> Unit,
    onReconcileLineClick: (Int) -> Unit = {},
    onDeleteLineClick: (Int) -> Unit = {},
    onEditWalletClick: (com.hwnix.cash.domain.model.FinancialAccount) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var bootDiagnostics by remember { mutableStateOf(BootTracker.getDiagnostics(context)) }
    val health by ServiceHealthMonitor.healthFlow.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(state.isRefreshing) {
        bootDiagnostics = BootTracker.getDiagnostics(context)
    }

    val isConnected = state.connectionStatus.contains("متصل") && !state.connectionStatus.contains("غير")
    val serviceOk = !state.isBatteryOptimized

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val sessionManager = remember { com.hwnix.cash.core.di.ServiceLocator.sessionManager }
            val limitAlertsSummary = remember(state.isRefreshing) { sessionManager.getLimitAlertsSummary() }
            val linesSummary = remember(state.isRefreshing) { sessionManager.getLinesSummary() }
            val isWalletMissing = remember(state.isRefreshing) { sessionManager.isWalletMissing() }

            if (isWalletMissing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "لابد من إضافة محفظة حتى يعمل التطبيق بشكل طبيعي وتتم حساب الأرصدة والحدود. يرجى إنشاء محفظة من بطاقات الخطوط أدناه.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ─── بطاقة تنبيه حدود الحسابات ───────────────────────────────
            if (visible && limitAlertsSummary.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه: حدود الحسابات المالية",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404),
                                fontSize = 13.sp
                            )
                            Text(
                                text = limitAlertsSummary,
                                color = Color(0xFF7D5A00),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ─── البطاقة الرئيسية Hero ────────────────────────────────────
            if (visible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1565C0),
                                        Color(0xFF0D47A1),
                                        Color(0xFF1976D2)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            // الصف العلوي: حالة الخدمة + الاتصال
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // مؤشر حالة الخدمة
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val serviceColor = Color(health.overallHealth.colorHex)
                                    val serviceText = health.overallHealth.label
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(serviceColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = serviceText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = serviceColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // حالة الاتصال
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isConnected) Color(0xFF4ADE80).copy(alpha = 0.2f)
                                    else Color(0xFFF87171).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isConnected) Color(0xFF4ADE80).copy(alpha = 0.5f)
                                        else Color(0xFFF87171).copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                            contentDescription = null,
                                            tint = if (isConnected) Color(0xFF4ADE80) else Color(0xFFF87171),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = state.connectionStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isConnected) Color(0xFF4ADE80) else Color(0xFFF87171),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // اسم البوابة
                            Text(
                                text = if (state.gatewayName.isNotBlank()) state.gatewayName else "بوابة كاش ونقاط البيع",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // تفاصيل الجهاز
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // معرف الجهاز
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Outlined.PhoneAndroid,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ID: #${state.deviceId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // UUID مختصر
                                if (state.deviceUuid.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Fingerprint,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = state.deviceUuid.take(12) + "…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // ─── بطاقة أرصدة وحدود خطوط الهاتف ────────────────────────────
            if (visible && !isWalletMissing && linesSummary.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // العنوان
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "أرصدة وحدود خطوط الهاتف",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))

                        if (state.deviceLines.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.deviceLines.forEach { (slot, info) ->
                                        val serverCarrier = info.carrier ?: ""
                                        val hardwareCarrier = state.detectedSims.find { it.slotIndex == slot }?.carrier ?: ""
                                        val lineName = com.hwnix.cash.utils.LineNameHelper.resolveLineName(serverCarrier, hardwareCarrier, slot)
                                        val phoneStr = info.phoneNumber
                                        val lineWallet = state.wallets.find { it.simPhone == phoneStr || (phoneStr.isNotEmpty() && it.simPhone.contains(phoneStr)) }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = if (lineWallet != null) "${lineWallet.name} ($lineName)" else "خط $lineName",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                val df = DecimalFormat("#,##0.00")
                                                val actualStr = df.format(info.totalActualBalance)
                                                val ledgerStr = df.format(info.totalBalance)
                                                val isBalanced = abs(info.totalActualBalance - info.totalBalance) < 0.01

                                                if (isBalanced) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("الرصيد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("${actualStr} ج.م", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column {
                                                            Text("الرصيد الفعلي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("${actualStr} ج.م", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text("الرصيد الدفتري", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("${ledgerStr} ج.م", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                if (lineWallet != null && (lineWallet.dailyWithdrawLimit != null || lineWallet.dailyDepositLimit != null)) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column {
                                                            Text("حد السحب اليومي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("${lineWallet.dailyWithdrawLimit ?: 0.0} ج.م", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text("حد الإيداع اليومي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("${lineWallet.dailyDepositLimit ?: 0.0} ج.م", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (!isBalanced) {
                                                        OutlinedButton(
                                                            onClick = { onReconcileLineClick(slot) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D47A1)),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D47A1).copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("تسوية", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }

                                                    if (lineWallet != null) {
                                                        OutlinedButton(
                                                            onClick = { onEditWalletClick(lineWallet) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("تعديل", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = { onAddWalletForLineClick(info.phoneNumber) },
                                                            modifier = Modifier.weight(2f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("إنشاء محفظة لهذا الخط", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }

                                                    OutlinedButton(
                                                        onClick = { onDeleteLineClick(slot) },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.5f)),
                                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("حذف", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── بطاقة حالة البطارية والحماية ────────────────────────────
            if (visible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isBatteryOptimized)
                            Color(0xFFFFEBEE)
                        else
                            Color(0xFFE8F5E9)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (state.isBatteryOptimized) Color(0xFFFFCDD2)
                                        else Color(0xFFC8E6C9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isBatteryOptimized) Icons.Filled.BatteryAlert else Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (state.isBatteryOptimized) Color(0xFFB71C1C) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حماية خلفية ${OEMAutostartHelper.getDeviceManufacturer().uppercase()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isBatteryOptimized) Color(0xFFB71C1C) else Color(0xFF2E7D32)
                                )
                                Text(
                                    text = if (state.isBatteryOptimized)
                                        "قيود البطارية قد تعطّل المزامنة"
                                    else
                                        "التشغيل والبطارية محميّان ✓",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isBatteryOptimized) Color(0xFFC62828) else Color(0xFF388E3C)
                                )
                            }
                        }

                        if (state.isBatteryOptimized) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { OEMAutostartHelper.openAutostartSettings(context) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("التشغيل التلقائي", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { OEMAutostartHelper.openBatteryOptimizationSettings(context) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("إلغاء القيود", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ─── أزرار التحكم ─────────────────────────────────────────────
            if (visible) {
                Column {
                    // زر المزامنة الفورية
                    Button(
                        onClick = onSyncNowClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D47A1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            Icons.Filled.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "مزامنة فورية الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // زر إعدادات الجهاز
                    OutlinedButton(
                        onClick = onSimSetupClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0D47A1)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFF0D47A1).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "إعدادات الجهاز والأرقام",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // زر فتح لوحة التحكم على الويب
                    OutlinedButton(
                        onClick = {
                            val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                            coroutineScope.launch {
                                com.hwnix.cash.utils.WebDashboardHelper.openMagicLink(
                                    context,
                                    com.hwnix.cash.data.remote.ApiClient.getService()
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2E7D32)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFF2E7D32).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "فتح لوحة التحكم على الويب 💻",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }



                }
            }
        }
    }

// ─── مكوّن كارت الإحصائية ───────────────────────────────────────────────────
@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    iconTint: Color,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = iconTint.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

// ─── مكوّن صف معلومة ─────────────────────────────────────────────────────────
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
