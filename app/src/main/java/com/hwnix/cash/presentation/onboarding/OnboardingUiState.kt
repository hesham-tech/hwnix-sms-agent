package com.hwnix.cash.presentation.onboarding

import com.hwnix.cash.domain.model.SimCard

data class OnboardingUiState(
    val isEditMode: Boolean = false,
    val editingWalletId: Int? = null,
    val currentStep: Int = 1,
    
    // Step 1: SIM Lines Setup
    val availableSims: List<SimCard> = emptyList(),
    val recentSenders: List<String> = emptyList(),
    val isDiscovering: Boolean = true,
    val line1Name: String = "",
    val line1Phone: String = "",
    val line2Name: String = "",
    val line2Phone: String = "",
    val isDualSim: Boolean = false,
    
    // Step 2: Wallet Details
    val walletName: String = "",
    val selectedSimPhone: String = "",
    val selectedSender: String = "",
    val isValidationLoading: Boolean = false,
    val validationSuccess: Boolean? = null,
    val validationMessage: String = "",
    
    // Step 3: Limits
    val dailyWithdrawLimit: String = "",
    val dailyDepositLimit: String = "",
    val monthlyWithdrawLimit: String = "",
    val monthlyDepositLimit: String = "",
    val dailyWithdrawAlertValue: String = "",
    val dailyWithdrawAlertType: String = "percentage",
    val dailyDepositAlertValue: String = "",
    val dailyDepositAlertType: String = "percentage",
    val monthlyWithdrawAlertValue: String = "",
    val monthlyWithdrawAlertType: String = "percentage",
    val monthlyDepositAlertValue: String = "",
    val monthlyDepositAlertType: String = "percentage",
    
    // Step 4: Review
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val submitError: String = ""
)
