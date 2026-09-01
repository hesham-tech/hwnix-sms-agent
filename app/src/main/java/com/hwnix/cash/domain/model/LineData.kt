package com.hwnix.cash.domain.model

data class LineData(
    val slotIndex: Int,
    val carrier: String,
    val phoneNumber: String,
    val totalBalance: Double,
    val totalActualBalance: Double
)
