package com.hwnix.cash.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(
    viewModel: OnboardingViewModel,
    onSuccess: () -> Unit,
    onBackToMain: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    // التعامل مع زر الرجوع
    BackHandler(enabled = true) {
        if (state.currentStep > 1) {
            viewModel.previousStep()
        } else {
            onBackToMain?.invoke()
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعداد المحفظة الجديدة") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // مؤشر الخطوات
            LinearProgressIndicator(
                progress = { state.currentStep / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "الخطوة ${state.currentStep} من 4",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = state.currentStep, label = "Steps") { step ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                ) {
                    when (step) {
                        1 -> StepLinesSetup(state, viewModel)
                        2 -> StepWalletDetails(state, viewModel)
                        3 -> StepLimits(state, viewModel)
                        4 -> StepReview(state)
                    }
                }
            }

            // أزرار التنقل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentStep > 1) {
                    OutlinedButton(onClick = { viewModel.previousStep() }) {
                        Text("السابق")
                    }
                } else if (onBackToMain != null) {
                    OutlinedButton(onClick = { onBackToMain() }) {
                        Text("إلغاء")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (state.currentStep < 4) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        enabled = canProceedToNextStep(state)
                    ) {
                        Text("التالي")
                    }
                } else {
                    Button(
                        onClick = { viewModel.submitOnboarding() },
                        enabled = !state.isSubmitting
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("إرسال")
                        }
                    }
                }
            }
        }
    }
}

fun canProceedToNextStep(state: OnboardingUiState): Boolean {
    return when (state.currentStep) {
        1 -> !state.isDiscovering && (state.line1Phone.isNotBlank() || state.line2Phone.isNotBlank())
        2 -> state.walletName.isNotBlank() && state.selectedSimPhone.isNotBlank() && state.selectedSender.isNotBlank()
        3 -> true
        else -> true
    }
}

@Composable
fun StepLinesSetup(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    if (state.isDiscovering) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("جاري استكشاف وجلب بيانات الشرائح...")
            }
        }
    } else {
        if (!state.isDualSim) {
            // شريحة واحدة
            Text("إعداد بيانات شريحة الاتصال:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.line1Name,
                onValueChange = { viewModel.onLine1NameChange(it) },
                label = { Text("اسم الخط") },
                placeholder = { Text("مثال: فودافون الرئيسية") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.line1Phone,
                onValueChange = { viewModel.onLine1PhoneChange(it) },
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        } else {
            // شريحتي اتصال
            Text("إعداد بيانات شرائح الاتصال (Dual SIM):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("الخط الأول (SIM 1):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.line1Name,
                        onValueChange = { viewModel.onLine1NameChange(it) },
                        label = { Text("اسم الخط الأول") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.line1Phone,
                        onValueChange = { viewModel.onLine1PhoneChange(it) },
                        label = { Text("رقم الخط الأول") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("الخط الثاني (SIM 2):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.line2Name,
                        onValueChange = { viewModel.onLine2NameChange(it) },
                        label = { Text("اسم الخط الثاني") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.line2Phone,
                        onValueChange = { viewModel.onLine2PhoneChange(it) },
                        label = { Text("رقم الخط الثاني") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepWalletDetails(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    var expandedSim by remember { mutableStateOf(false) }
    var expandedSender by remember { mutableStateOf(false) }

    val phoneOptions = remember(state.line1Phone, state.line2Phone, state.availableSims) {
        listOfNotNull(
            state.line1Phone.takeIf { it.isNotBlank() },
            state.line2Phone.takeIf { state.isDualSim && it.isNotBlank() }
        ).ifEmpty {
            state.availableSims.map { it.phoneNumber }.filter { it.isNotBlank() }
        }
    }

    OutlinedTextField(
        value = state.walletName,
        onValueChange = { viewModel.onWalletNameChange(it) },
        label = { Text("اسم المحفظة") },
        placeholder = { Text("مثال: محفظة فودافون كاش - الخط الرئيسي") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    // Dropdown for SIM Phone Selection
    ExposedDropdownMenuBox(
        expanded = expandedSim,
        onExpandedChange = { expandedSim = it }
    ) {
        OutlinedTextField(
            value = state.selectedSimPhone,
            onValueChange = { viewModel.onSimPhoneChange(it) },
            label = { Text("رقم الهاتف (الخط)") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSim) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expandedSim,
            onDismissRequest = { expandedSim = false }
        ) {
            phoneOptions.forEach { phone ->
                DropdownMenuItem(
                    text = { Text(phone) },
                    onClick = {
                        viewModel.onSimPhoneChange(phone)
                        expandedSim = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Dropdown for Sender (مصدر رسائل المحفظة)
    ExposedDropdownMenuBox(
        expanded = expandedSender,
        onExpandedChange = { expandedSender = it }
    ) {
        OutlinedTextField(
            value = state.selectedSender,
            onValueChange = { viewModel.onSenderChange(it) },
            label = { Text("مصدر رسائل المحفظة (Sender)") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSender) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expandedSender,
            onDismissRequest = { expandedSender = false }
        ) {
            state.recentSenders.forEach { sender ->
                DropdownMenuItem(
                    text = { Text(sender) },
                    onClick = {
                        viewModel.onSenderChange(sender)
                        expandedSender = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // حالة التحقق
    if (state.isValidationLoading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("جاري التحقق...", style = MaterialTheme.typography.bodySmall)
        }
    } else if (state.validationSuccess != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (state.validationSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (state.validationSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = state.validationMessage,
                color = if (state.validationSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun StepLimits(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OutlinedTextField(
        value = state.dailyDepositLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, it, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
        label = { Text("حد الإيداع اليومي") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.dailyWithdrawLimit,
        onValueChange = { viewModel.onLimitChange(it, state.dailyDepositLimit, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
        label = { Text("حد السحب اليومي") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.monthlyDepositLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, state.monthlyWithdrawLimit, it) },
        label = { Text("حد الإيداع الشهري") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.monthlyWithdrawLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, it, state.monthlyDepositLimit) },
        label = { Text("حد السحب الشهري") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
fun StepReview(state: OnboardingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ملخص المحفظة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("اسم المحفظة: ${state.walletName}")
            Text("الخط المرتبط: ${state.selectedSimPhone}")
            Text("مصدر الرسائل: ${state.selectedSender}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("إيداع يومي: ${state.dailyDepositLimit.ifBlank { "غير محدد" }}")
            Text("سحب يومي: ${state.dailyWithdrawLimit.ifBlank { "غير محدد" }}")
            Text("إيداع شهري: ${state.monthlyDepositLimit.ifBlank { "غير محدد" }}")
            Text("سحب شهري: ${state.monthlyWithdrawLimit.ifBlank { "غير محدد" }}")
        }
    }
    if (state.submitError.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = state.submitError, color = Color.Red)
    }
}
