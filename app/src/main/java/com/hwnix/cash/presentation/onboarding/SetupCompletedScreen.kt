package com.hwnix.cash.presentation.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwnix.cash.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * شاشة جاهزية النظام واختبار الاتصال الحي بالسيرفر والانتقال إلى مرحلة استخدام التطبيق
 */
@Composable
fun SetupCompletedScreen(
    onStartUsingApp: () -> Unit
) {
    val context = LocalContext.current
    var isCheckingPing by remember { mutableStateOf(true) }
    var isPingSuccess by remember { mutableStateOf<Boolean?>(null) }

    // إجراء اختبار اتصال حي بالسيرفر عند تحميل الشاشة
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val sessionManager = com.hwnix.cash.core.di.ServiceLocator.sessionManager
                val deviceId = sessionManager.getDeviceId()
                val response = ApiClient.getService().getDeviceLines(deviceId)
                isPingSuccess = response.isSuccessful
            } catch (e: Exception) {
                isPingSuccess = false
            } finally {
                isCheckingPing = false
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // مؤشر التقدم العلوي الموحد (الخطوة 3)
            OnboardingFlowProgressIndicator(currentStep = 3)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // أيقونة النجاح والجاهزية
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "✅ النظام جاهز للعمل",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // كارت رسائل الطمأنة الجوهرية
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تم إنشاء أول محفظة بنجاح.",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النظام جاهز، وسيبدأ التطبيق تلقائيًا في استقبال الرسائل وإرسالها إلى السيرفر.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لن تحتاج إلى فتح التطبيق باستمرار، فهو يعمل في الخلفية.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // كارت حالة اختبار الاتصال المباشر بالسيرفر (Ping Status)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (isPingSuccess) {
                            true -> Color(0xFFE8F5E9)
                            false -> Color(0xFFFFF3E0)
                            null -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCheckingPing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "جارٍ اختبار الاتصال بالسيرفر...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isPingSuccess == true) {
                            Icon(imageVector = Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "✅ تم الاتصال بالسيرفر بنجاح.",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        } else {
                            Icon(imageVector = Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "⚠️ تعذر الاتصال بالسيرفر حالياً. سيعيد التطبيق الاتصال تلقائياً بمجرد توفر الإنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // كارت الشرط الجوهري (الاتصال بالإنترنت)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF57F17))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "تأكد فقط من إبقاء هذا الهاتف متصلًا بالإنترنت حتى يتمكن من إرسال الرسائل إلى السيرفر.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // تنبيه لوحة الويب الفرعي
                Text(
                    text = "إذا أردت إدارة المحافظ أو مراجعة الرسائل والتقارير يمكنك استخدام لوحة التحكم على الويب بنفس بيانات تسجيل الدخول.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // الزر الرئيسي الكبير: بدء استخدام التطبيق
                Button(
                    onClick = onStartUsingApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "بدء استخدام التطبيق",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // الزر الثانوي: فتح لوحة التحكم على الويب
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "فتح لوحة التحكم على الويب 🌐", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // الملاحظة التوضيحية السفلية
                Text(
                    text = "ℹ️ يمكنك فتح لوحة التحكم لاحقًا أيضًا من داخل التطبيق في أي وقت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
