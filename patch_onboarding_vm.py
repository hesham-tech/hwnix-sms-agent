import re

file_path = "app/src/main/java/com/hwnix/cash/presentation/onboarding/OnboardingViewModel.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add onAlertLimitChange
new_method = \"\"\"    fun onAlertLimitChange(
        dwValue: String, dwType: String,
        ddValue: String, ddType: String,
        mwValue: String, mwType: String,
        mdValue: String, mdType: String
    ) {
        _uiState.update { 
            it.copy(
                dailyWithdrawAlertValue = dwValue,
                dailyWithdrawAlertType = dwType,
                dailyDepositAlertValue = ddValue,
                dailyDepositAlertType = ddType,
                monthlyWithdrawAlertValue = mwValue,
                monthlyWithdrawAlertType = mwType,
                monthlyDepositAlertValue = mdValue,
                monthlyDepositAlertType = mdType
            )
        }
    }

    fun loadWalletForEdit\"\"\"

content = content.replace("    fun loadWalletForEdit", new_method)

# Update loadWalletForEdit
old_load = \"\"\"                dailyDepositLimit = wallet.dailyDepositLimit?.toString() ?: "",
                monthlyWithdrawLimit = wallet.monthlyWithdrawLimit?.toString() ?: "",
                monthlyDepositLimit = wallet.monthlyDepositLimit?.toString() ?: ""\"\"\"
new_load = \"\"\"                dailyDepositLimit = wallet.dailyDepositLimit?.toString() ?: "",
                monthlyWithdrawLimit = wallet.monthlyWithdrawLimit?.toString() ?: "",
                monthlyDepositLimit = wallet.monthlyDepositLimit?.toString() ?: "",
                dailyWithdrawAlertValue = wallet.dailyWithdrawAlertValue?.toString() ?: "",
                dailyWithdrawAlertType = wallet.dailyWithdrawAlertType ?: "percentage",
                dailyDepositAlertValue = wallet.dailyDepositAlertValue?.toString() ?: "",
                dailyDepositAlertType = wallet.dailyDepositAlertType ?: "percentage",
                monthlyWithdrawAlertValue = wallet.monthlyWithdrawAlertValue?.toString() ?: "",
                monthlyWithdrawAlertType = wallet.monthlyWithdrawAlertType ?: "percentage",
                monthlyDepositAlertValue = wallet.monthlyDepositAlertValue?.toString() ?: "",
                monthlyDepositAlertType = wallet.monthlyDepositAlertType ?: "percentage"\"\"\"

content = content.replace(old_load, new_load)

# Update submitOnboarding payload
old_submit = \"\"\"                    addProperty("monthly_withdraw_limit", state.monthlyWithdrawLimit.toDoubleOrNull() ?: 0.0)
                    addProperty("monthly_deposit_limit", state.monthlyDepositLimit.toDoubleOrNull() ?: 0.0)
                }\"\"\"

new_submit = \"\"\"                    addProperty("monthly_withdraw_limit", state.monthlyWithdrawLimit.toDoubleOrNull() ?: 0.0)
                    addProperty("monthly_deposit_limit", state.monthlyDepositLimit.toDoubleOrNull() ?: 0.0)
                    
                    addProperty("daily_withdraw_alert_value", state.dailyWithdrawAlertValue.toDoubleOrNull())
                    addProperty("daily_withdraw_alert_type", state.dailyWithdrawAlertType)
                    addProperty("daily_deposit_alert_value", state.dailyDepositAlertValue.toDoubleOrNull())
                    addProperty("daily_deposit_alert_type", state.dailyDepositAlertType)
                    
                    addProperty("monthly_withdraw_alert_value", state.monthlyWithdrawAlertValue.toDoubleOrNull())
                    addProperty("monthly_withdraw_alert_type", state.monthlyWithdrawAlertType)
                    addProperty("monthly_deposit_alert_value", state.monthlyDepositAlertValue.toDoubleOrNull())
                    addProperty("monthly_deposit_alert_type", state.monthlyDepositAlertType)
                }\"\"\"
content = content.replace(old_submit, new_submit)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Patched OnboardingViewModel.kt successfully!")
