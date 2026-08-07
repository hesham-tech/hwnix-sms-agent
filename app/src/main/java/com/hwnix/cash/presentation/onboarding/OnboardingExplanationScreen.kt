package com.hwnix.cash.presentation.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * شاشة الترحيب والدليل التعليمي الأولي لمستخدم تطبيق كاش هونكس الجدد توضح آلية العمل وفصل مسؤولية الويب والتطبيق
 */
@Composable
fun OnboardingExplanationScreen(
    onStartSetupClick: () -> Unit,
    onReadLaterClick: () -> Unit,
    webDashboardUrl: String = "https://bill.hwnix.com/app/hwnix-cash/dashboard"
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF1976D2)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PointOfSale,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(40.dp)
                    )
                }
                Text(
                    text = "مرحباً بك في Cash HWNix",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "دليلك السريع لمفهوم وإدارة نظام الكاش والمحافظ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Role separation callout (Crucial Rule Requirement)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مسؤولية التطبيق ولوحة الويب:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "التطبيق مسؤول عن استقبال الرسائل وإرسالها إلى السيرفر، بينما تتم إدارة النظام بالكامل من خلال لوحة التحكم على الويب باستخدام نفس بيانات تسجيل الدخول الخاصة بالتطبيق.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 2. How it works
            ExplanationCard(
                icon = Icons.Filled.PhonelinkRing,
                title = "كيف يعمل النظام؟",
                description = "يقوم التطبيق بالعمل في الخلفية لقراءة رسائل SMS المالية الواردة من شرائح المحافظ البنكية (مثل Vodafone Cash، Orange Cash، Etisalat Cash). يتم إرسال بيانات التحويل للسيرفر وتصفية الرسائل غير المالية بأمان تام."
            )

            // 3. Why create wallets
            ExplanationCard(
                icon = Icons.Filled.Wallet,
                title = "لماذا نربط الخطوط بالمحافظ؟",
                description = "لكل خط SIM في الجهاز محفظة مالية محددة. يتيح ربط الخط بالمحفظة متابعة الاستهلاك والحد اليومي والشهري وتنبيهك تلقائياً عند الاقتراب من سقف السحب أو الإيداع."
            )

            // 4. Web Dashboard Link Button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "لوحة التحكم على الويب (Web Dashboard)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يمكنك فتح لوحة التحكم على متصفح الويب لمتابعة التقارير التفصيلية، ضبط الحدود، وإدارة الحسابات بكل سهولة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webDashboardUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح لوحة التحكم على الويب الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. What happens next?
            ExplanationCard(
                icon = Icons.Filled.RocketLaunch,
                title = "ماذا سيحدث بعد النقر على الزر؟",
                description = "عند النقر على \"بدء الإعداد\"، سيساعدك التطبيق في التحقق من خطوط الـ SIM الموجودة بجهازك، وإنشاء المحفظة الأولى أو ربطها بالخطوط المتصلة لمباشرة العمل فوراً."
            )
        }

        // Bottom Action Buttons Area
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartSetupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    )
                ) {
                    Text(
                        text = "بدء الإعداد الآن 🚀",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onReadLaterClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "قراءة لاحقاً والانتقال للشاشة الرئيسية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
