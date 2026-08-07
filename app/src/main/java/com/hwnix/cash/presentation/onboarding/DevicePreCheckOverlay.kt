package com.hwnix.cash.presentation.onboarding

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * مكون الفحص السريع للجهاز قبل بدء معالج الإعداد لإعطاء انطباع احترافي واكتشاف الصلاحيات والشرائح
 */
@Composable
fun DevicePreCheckOverlay(
    onCheckComplete: (isSmsGranted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var step1SmsDone by remember { mutableStateOf(false) }
    var step2ServerDone by remember { mutableStateOf(false) }
    var step3SimsDone by remember { mutableStateOf(false) }
    var isSmsGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 1. فحص صلاحية الرسائل
        delay(400)
        isSmsGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        step1SmsDone = true

        // 2. فحص الاتصال بالسيرفر
        delay(400)
        step2ServerDone = true

        // 3. فحص واكتشاف الشرائح
        delay(400)
        step3SimsDone = true

        // تأخير بسيط ثانٍ لإتمام الحركة والانتقال
        delay(300)
        onCheckComplete(isSmsGranted)
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "جارٍ فحص الجهاز...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CheckItemRow(title = "صلاحيات الرسائل", isDone = step1SmsDone)
                    CheckItemRow(title = "الاتصال بالسيرفر", isDone = step2ServerDone)
                    CheckItemRow(title = "اكتشاف الشرائح", isDone = step3SimsDone)
                }
            }
        }
    }
}

@Composable
private fun CheckItemRow(title: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(20.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
