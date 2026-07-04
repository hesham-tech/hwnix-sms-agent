package com.hwnix.smsagent.domain.model

// كلاس تمثيل شريحة الاتصال (SIM Card) في طبقة الدومين
data class SimCard(
    val slotIndex: Int,
    val subscriptionId: String,
    val carrier: String,
    val phoneNumber: String,
    val mcc: String,
    val mnc: String
)
