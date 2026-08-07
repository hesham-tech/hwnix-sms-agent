package com.hwnix.cash.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * مكون مؤشر التقدم العلوي لرحلة الإعداد الموحدة يعرض المراحل الثلاث بنقاط متصلة وحالة الإنجاز
 */
@Composable
fun OnboardingFlowProgressIndicator(
    currentStep: Int, // 1: الترحيب, 2: إعداد المحفظة, 3: جاهزية النظام
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "الترحيب",
        "إعداد المحفظة",
        "جاهزية النظام"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isCompleted = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // شارة الخطوة (دايرة الرقـم أو صح)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Color(0xFF2E7D32) // خـضر مكتمل
                                isCurrent -> Color(0xFF0D47A1) // أزرق حـالي
                                else -> MaterialTheme.colorScheme.surfaceVariant // رمادي قادم
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // عنوان الخطوة
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onBackground
                        isCompleted -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // الخط الفاصل المتصل بين الخطوات
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (stepNumber < currentStep) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
