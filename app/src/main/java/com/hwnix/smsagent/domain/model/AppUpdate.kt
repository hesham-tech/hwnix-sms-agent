package com.hwnix.smsagent.domain.model

// موديل يمثل معلومات تحديث التطبيق المستلمة من السيرفر
data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String
)
