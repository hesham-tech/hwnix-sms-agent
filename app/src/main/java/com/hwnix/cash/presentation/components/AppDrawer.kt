package com.hwnix.cash.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// مكوّن القائمة الجانبية - يحتوي على التنقل وخيارات الحساب وزر الخروج مع تأكيد
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    drawerState: DrawerState,
    currentVersionName: String,
    currentVersionCode: Int,
    localUpdateVersionName: String,
    localUpdateApk: File?,
    isCheckingUpdate: Boolean,
    updateStatusMessage: String?,
    currentScreen: String = "status",
    onNavigateToScreen: (String) -> Unit = {},
    onInstallLocalApk: (File) -> Unit,
    onCheckForUpdate: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onDecoupleClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // حالات dialogs التأكيد
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDecoupleDialog by remember { mutableStateOf(false) }

    // Dialog تأكيد تسجيل الخروج
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    "تسجيل الخروج",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("هل تريد تسجيل الخروج حقاً؟ ستحتاج إلى إعادة تسجيل الدخول لاستئناف الخدمة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("نعم، تسجيل الخروج")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }

    // Dialog تأكيد إلغاء الربط
    if (showDecoupleDialog) {
        AlertDialog(
            onDismissRequest = { showDecoupleDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B)
                )
            },
            title = {
                Text(
                    "إلغاء ربط الجهاز",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("سيتم إلغاء ربط هذا الجهاز بالحساب. هل أنت متأكد؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDecoupleDialog = false
                        onDecoupleClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B)
                    )
                ) {
                    Text("نعم، إلغاء الربط")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDecoupleDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.82f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PointOfSale,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "HWNix Cash",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "بوابة كاش ونقاط البيع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("الرئيسية", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    selected = currentScreen == "status",
                    onClick = { onNavigateToScreen("status") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("تقرير التشخيصات (Diagnostics)", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    selected = currentScreen == "diagnostics",
                    onClick = { onNavigateToScreen("diagnostics") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("تغيير الشركة", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.PointOfSale, contentDescription = null) },
                    selected = currentScreen == "company_selection",
                    onClick = { onNavigateToScreen("company_selection") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(12.dp))

                // Version Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الإصدار الحالي: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "v$currentVersionName ($currentVersionCode)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (localUpdateApk != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إصدار جديد جاهز: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text("v$localUpdateVersionName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (localUpdateApk != null) {
                    Button(
                        onClick = { onInstallLocalApk(localUpdateApk) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("تثبيت التحديث (v$localUpdateVersionName)")
                    }
                } else {
                    OutlinedButton(
                        onClick = onCheckForUpdate,
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الفحص...")
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("التحقق من التحديثات")
                        }
                    }
                }

                if (updateStatusMessage != null) {
                    Text(
                        text = updateStatusMessage,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // أزرار الخروج وإلغاء الربط
                OutlinedButton(
                    onClick = { showDecoupleDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF59E0B)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إلغاء ربط الجهاز", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل الخروج", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "HWNix Cash Platform © 2026",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    ) {
        content()
    }
}
