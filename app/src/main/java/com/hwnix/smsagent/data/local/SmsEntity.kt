package com.hwnix.smsagent.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_cache")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val messageBody: String,
    val direction: String, // "incoming" or "outgoing"
    val status: String,    // "pending_upload", "uploaded", "failed"
    val messageRef: String, // المعرف الفريد للرسالة محلياً
    val subscriptionId: String, // معرف الشريحة
    val sentAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)
