package com.hwnix.cash.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeleteDialog(
    slotIndex: Int,
    isDeleting: Boolean,
    deleteResult: String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB71C1C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد الحذف النهائي", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "هل أنت متأكد من رغبتك في حذف شريحة ${slotIndex + 1}؟\n\n" +
                    "تحذير: سيتم حذف الخط وجميع المحافظ التابعة له، وكل الرسائل وسجل العمليات نهائياً وبدون رجعة (Force Delete).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828)
                )

                if (isDeleting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (deleteResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = deleteResult,
                        color = if (deleteResult.contains("✅")) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) {
                Text("حذف نهائياً")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("إلغاء", color = Color(0xFF424242))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
