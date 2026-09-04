package com.hwnix.cash.domain.model

data class FinancialAccount(
    val id: Int,
    val name: String,
    val senderIdentifier: String,
    val simPhone: String,
    val dailyWithdrawLimit: Double?,
    val dailyDepositLimit: Double?,
    val monthlyWithdrawLimit: Double?,
    val monthlyDepositLimit: Double?
)
