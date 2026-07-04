package com.hwnix.smsagent.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwnix.smsagent.domain.model.SimCard

@Composable
fun SimSetupDialog(
    sims: List<SimCard>,
    phoneInputs: Map<Int, String>,
    carrierInputs: Map<Int, String>,
    gatewayName: String,
    isSaving: Boolean,
    saveResult: String?,
    isFirstSetup: Boolean,
    onGatewayNameChange: (String) -> Unit,
    onPhoneChange: (Int, String) -> Unit,
    onCarrierChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving && !isFirstSetup) onDismiss() },
        title = { Text(if (isFirstSetup) "إعداد البوابة (مطلوب)" else "إعداد أرقام الخطوط") },
        text = {
            Column {
                if (isFirstSetup) {
                    Text(
                        "يرجى إكمال إعداد البوابة قبل المتابعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = gatewayName,
                    onValueChange = onGatewayNameChange,
                    label = { Text("اسم البوابة *") },
                    placeholder = { Text("مثال: الفرع الرئيسي") },
                    singleLine = true,
                    isError = gatewayName.isBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                if (sims.isEmpty()) {
                    Text("⚠️ لم يتم اكتشاف أي شريحة نشطة في الجهاز.")
                } else {
                    Text(
                        "تم اكتشاف ${sims.size} خط. أدخل البيانات لكل خط:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sims.forEach { sim ->
                        val currentCarrier = carrierInputs[sim.slotIndex] ?: ""
                        val currentPhone = phoneInputs[sim.slotIndex] ?: ""

                        OutlinedTextField(
                            value = currentCarrier,
                            onValueChange = { onCarrierChange(sim.slotIndex, it) },
                            label = { Text("مشغل الشبكة (SIM ${sim.slotIndex + 1}) *") },
                            placeholder = { Text("مثال: Vodafone أو Orange") },
                            singleLine = true,
                            isError = currentCarrier.isBlank(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = currentPhone,
                            onValueChange = { onPhoneChange(sim.slotIndex, it) },
                            label = { Text("رقم الهاتف (SIM ${sim.slotIndex + 1})") },
                            placeholder = { Text("مثال: 01XXXXXXXXX") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }
                }

                if (saveResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        saveResult,
                        color = if (saveResult.startsWith("✅")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                if (isSaving) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            val isAllCarriersValid = sims.all { !carrierInputs[it.slotIndex].isNullOrBlank() }
            Button(
                onClick = onSave,
                enabled = !isSaving && gatewayName.isNotBlank() && (sims.isEmpty() || isAllCarriersValid)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            if (!isFirstSetup) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("إغلاق")
                }
            }
        }
    )
}
