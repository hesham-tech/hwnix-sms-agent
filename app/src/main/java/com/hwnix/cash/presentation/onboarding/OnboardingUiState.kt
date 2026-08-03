package com.hwnix.cash.presentation.onboarding

import com.hwnix.cash.domain.model.SimCard

data class OnboardingUiState(
    val currentStep: Int = 1,
    // Step 1: Discovery
    val availableSims: List<SimCard> = emptyList(),
    val recentSenders: List<String> = emptyList(),
    val isDiscovering: Boolean = true,
    
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
    
    // Step 4: Review
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val submitError: String = ""
)
