package com.hwnix.cash.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconcileDialog(
    slotIndex: Int,
    targetBalance: String,
    note: String,
    isReconciling: Boolean,
    reconcileResult: String?,
    onTargetBalanceChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onReconcile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isReconciling) onDismiss() },
        title = {
            Text(
                "تسوية رصيد شريحة ${slotIndex + 1}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )
        },
        text = {
            Column {
                Text(
                    "أدخل الرصيد الفعلي الحالي للخط:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetBalance,
                    onValueChange = { onTargetBalanceChange(it) },
                    label = { Text("الرصيد الفعلي (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isReconciling
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { onNoteChange(it) },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isReconciling,
                    maxLines = 3
                )

                if (reconcileResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = reconcileResult,
                        color = if (reconcileResult.contains("نجاح")) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onReconcile,
                enabled = !isReconciling && targetBalance.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isReconciling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التسوية...")
                } else {
                    Text("تأكيد التسوية", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isReconciling
            ) {
                Text("إلغاء", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
