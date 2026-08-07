package com.hwnix.cash.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * شاشة التعريف والتوجيه الرئيسية للتركيز والقيادة المباشرة لرحلة إعداد النظام
 */
@Composable
fun OnboardingExplanationScreen(
    isFromMenu: Boolean = false,
    onStartSetup: () -> Unit,
    onPermissionNeeded: () -> Unit,
    onClose: () -> Unit
) {
    var showPreCheck by remember { mutableStateOf(false) }

    if (showPreCheck) {
        DevicePreCheckOverlay(
            onCheckComplete = { isSmsGranted ->
                showPreCheck = false
                if (isSmsGranted) {
                    onStartSetup()
                } else {
                    onPermissionNeeded()
                }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // مؤشر التقدم العلوي الموحد (يظهر فقط في أول تشغيل)
            if (!isFromMenu) {
                OnboardingFlowProgressIndicator(currentStep = 1)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // الهيدر الرئيسي المكثف
                Text(
                    text = "مرحباً بك في Cash HWNix",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "سنرشدك لإعداد النظام خطوة بخطوة.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                )

                // 1. كارت عزل المسؤوليات المباشر
                ExplanationCard(
                    icon = Icons.Filled.AppShortcut,
                    title = "مسؤولية التطبيق ولوحة الويب",
                    description = "التطبيق يستقبل الرسائل ويرسلها للسيرفر، بينما تتم إدارة النظام بالكامل من خلال لوحة الويب.",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. كارت كيف يعمل النظام؟
                ExplanationCard(
                    icon = Icons.Filled.Sync,
                    title = "كيف يعمل النظام؟",
                    description = "يعمل التطبيق في الخلفية، ويرسل الرسائل الواردة إلى السيرفر، الذي يحدد تلقائيًا الرسائل المالية ويحولها إلى معاملات، بينما يحتفظ بالرسائل الأخرى في السجل.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. كارت لماذا ننشئ محفظة؟
                ExplanationCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "لماذا ننشئ محفظة؟",
                    description = "لكل خط محفظة خاصة به. بدون إنشاء محفظة لن يستطيع النظام متابعة هذا الخط أو احتساب حدوده اليومية والشهرية.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. كارت العمل في الخلفية (هل يجب أن يبقى التطبيق مفتوحاً؟)
                ExplanationCard(
                    icon = Icons.Filled.Schedule,
                    title = "هل يجب أن يبقى التطبيق مفتوحًا؟",
                    description = "لا. بعد اكتمال الإعداد يعمل التطبيق تلقائيًا في الخلفية.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. كارت ماذا سيحدث الآن؟
                ExplanationCard(
                    icon = Icons.Filled.PlayArrow,
                    title = "ماذا سيحدث الآن؟",
                    description = "سنساعدك الآن في إنشاء أول محفظة وربطها بخط الهاتف، وبعدها سيبدأ النظام بالعمل تلقائيًا.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. كارت التهيئة النفسية (قبل أن نبدأ)
                ExplanationCard(
                    icon = Icons.Filled.RocketLaunch,
                    title = "قبل أن نبدأ",
                    description = "ستستغرق عملية الإعداد أقل من دقيقة، وسنساعدك خطوة بخطوة حتى يصبح النظام جاهزًا للعمل.",
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // الزر السفلي الوحيد حسب حالة المصدر
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (!isFromMenu) {
                        Button(
                            onClick = { showPreCheck = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "بدء الإعداد 🚀",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "إغلاق",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
