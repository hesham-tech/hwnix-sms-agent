import re

file_path = "app/src/main/java/com/hwnix/cash/presentation/onboarding/OnboardingWizardScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

old_limits = \"\"\"fun StepLimits(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OutlinedTextField(
        value = state.dailyDepositLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, it, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
        label = { Text("?? ??????? ??????") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.dailyWithdrawLimit,
        onValueChange = { viewModel.onLimitChange(it, state.dailyDepositLimit, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
        label = { Text("?? ????? ??????") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.monthlyDepositLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, state.monthlyWithdrawLimit, it) },
        label = { Text("?? ??????? ??????") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.monthlyWithdrawLimit,
        onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, it, state.monthlyDepositLimit) },
        label = { Text("?? ????? ??????") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}\"\"\"

new_limits = \"\"\"@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepLimits(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("???? ??????? (???????)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.dailyDepositLimit,
            onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, it, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
            label = { Text("?? ??????? ??????") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.dailyWithdrawLimit,
            onValueChange = { viewModel.onLimitChange(it, state.dailyDepositLimit, state.monthlyWithdrawLimit, state.monthlyDepositLimit) },
            label = { Text("?? ????? ??????") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.monthlyDepositLimit,
            onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, state.monthlyWithdrawLimit, it) },
            label = { Text("?? ??????? ??????") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.monthlyWithdrawLimit,
            onValueChange = { viewModel.onLimitChange(state.dailyWithdrawLimit, state.dailyDepositLimit, it, state.monthlyDepositLimit) },
            label = { Text("?? ????? ??????") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("???? ??????? (???????)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        AlertLimitField(
            label = "????? ??????? ??????",
            value = state.dailyDepositAlertValue,
            type = state.dailyDepositAlertType,
            onValueChange = { v -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, v, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) },
            onTypeChange = { t -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, t, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) }
        )

        AlertLimitField(
            label = "????? ????? ??????",
            value = state.dailyWithdrawAlertValue,
            type = state.dailyWithdrawAlertType,
            onValueChange = { v -> viewModel.onAlertLimitChange(v, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) },
            onTypeChange = { t -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, t, state.dailyDepositAlertValue, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) }
        )

        AlertLimitField(
            label = "????? ??????? ??????",
            value = state.monthlyDepositAlertValue,
            type = state.monthlyDepositAlertType,
            onValueChange = { v -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, v, state.monthlyDepositAlertType) },
            onTypeChange = { t -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, t) }
        )

        AlertLimitField(
            label = "????? ????? ??????",
            value = state.monthlyWithdrawAlertValue,
            type = state.monthlyWithdrawAlertType,
            onValueChange = { v -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, state.dailyDepositAlertType, v, state.monthlyWithdrawAlertType, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) },
            onTypeChange = { t -> viewModel.onAlertLimitChange(state.dailyWithdrawAlertValue, state.dailyWithdrawAlertType, state.dailyDepositAlertValue, state.dailyDepositAlertType, state.monthlyWithdrawAlertValue, t, state.monthlyDepositAlertValue, state.monthlyDepositAlertType) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertLimitField(
    label: String,
    value: String,
    type: String,
    onValueChange: (String) -> Unit,
    onTypeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(120.dp).padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = if (type == "percentage") "???? %" else "???? ?????",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("???? %") },
                    onClick = { onTypeChange("percentage"); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("???? ?????") },
                    onClick = { onTypeChange("value"); expanded = false }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}\"\"\"

content = content.replace(old_limits, new_limits)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Patched StepLimits successfully!")
