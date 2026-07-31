package com.hwnix.cash.data.local

import com.google.gson.JsonParser

/* تعليق عربي مختصر: مدير رسائل التخزين المؤقت أوفلاين لوضع Direct Boot قبل فك قفل الجهاز */
object DirectBootQueueManager {

    fun parseDirectBootJson(jsonString: String): SmsEntity? {
        return try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            SmsEntity(
                phoneNumber = json.get("phoneNumber").asString,
                messageBody = json.get("messageBody").asString,
                subscriptionId = json.get("subscriptionId").asString,
                sentAt = json.get("sentAt").asLong,
                direction = "incoming",
                status = "pending_upload",
                messageRef = java.util.UUID.randomUUID().toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}
