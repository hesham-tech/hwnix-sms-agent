package com.hwnix.cash.domain.model

data class FinancialAccount(
    val id: Int,
    val name: String,
    val senderIdentifier: String,
    val simPhone: String,
    val dailyWithdrawLimit: Double?,
    val dailyDepositLimit: Double?,
    val monthlyWithdrawLimit: Double?,
    val monthlyDepositLimit: Double?,
    val dailyWithdrawAlertValue: Double? = null,
    val dailyWithdrawAlertType: String = "percentage",
    val dailyDepositAlertValue: Double? = null,
    val dailyDepositAlertType: String = "percentage",
    val monthlyWithdrawAlertValue: Double? = null,
    val monthlyWithdrawAlertType: String = "percentage",
    val monthlyDepositAlertValue: Double? = null,
    val monthlyDepositAlertType: String = "percentage"
)
