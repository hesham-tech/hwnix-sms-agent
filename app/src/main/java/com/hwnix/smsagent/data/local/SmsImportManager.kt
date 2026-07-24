package com.hwnix.smsagent.data.local

import android.content.Context
import android.util.Log
import com.hwnix.smsagent.core.di.ServiceLocator
import java.security.MessageDigest
import java.util.UUID

data class SmsImportRequest(
    val phoneNumber: String,
    val messageBody: String,
    val subscriptionId: String,
    val sentAt: Long,
    val source: String, // "SmsReceiver", "InboxScan", "DirectBootQueue", "RecoveryScan"
    val androidSmsId: String? = null,
    val pduTimestamp: Long? = null,
    val dateSentTimestamp: Long? = null,
    val dateTimestamp: Long? = null
)

/* تعليق عربي مختصر: مدير استيراد الرسائل الموحد لمنع تكرار الرسائل وتتبع مصادرها بدقة */
object SmsImportManager {

    private const val TAG = "SmsImportManager"

    fun determineSentAt(pduTimestamp: Long?, dateSent: Long?, date: Long?, fallbackSentAt: Long): Long {
        val selected = when {
            pduTimestamp != null && pduTimestamp > 0 -> pduTimestamp
            dateSent != null && dateSent > 0 -> dateSent
            date != null && date > 0 -> date
            else -> fallbackSentAt
        }
        Log.i(TAG, "SMS_TIMESTAMP_COMPARE | PDU: $pduTimestamp | DATE_SENT: $dateSent | DATE: $date | Selected: $selected")
        return selected
    }

    fun generateIdempotencyKey(req: SmsImportRequest): String {
        return if (!req.androidSmsId.isNullOrEmpty()) {
            "sys_id_${req.androidSmsId}"
        } else {
            val normalizedAddress = req.phoneNumber.replace("[^0-9]".toRegex(), "")
                .let { if (it.length > 9) it.takeLast(9) else it }
            val bodyHash = sha256(req.messageBody)
            "key_${normalizedAddress}_${req.sentAt}_${req.subscriptionId}_$bodyHash"
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(4).joinToString("") { "%02x".format(it) }
    }

    suspend fun importMessage(context: Context, request: SmsImportRequest): Boolean {
        val resolvedSentAt = determineSentAt(
            request.pduTimestamp,
            request.dateSentTimestamp,
            request.dateTimestamp,
            request.sentAt
        )
        val finalRequest = request.copy(sentAt = resolvedSentAt)

        val idempotencyKey = generateIdempotencyKey(finalRequest)
        val bodyHash = sha256(finalRequest.messageBody)
        val traceMessage = "SMS_IMPORT_ATTEMPT | source=${finalRequest.source} | id=${finalRequest.androidSmsId ?: "N/A"} | address=${finalRequest.phoneNumber} | date=${finalRequest.sentAt} | hash=$bodyHash | key=$idempotencyKey"

        Log.i(TAG, traceMessage)

        val isUnlocked = androidx.core.os.UserManagerCompat.isUserUnlocked(context)

        if (!isUnlocked) {
            Log.w(TAG, "Device is locked (Direct Boot). Saving SMS to Device Protected Storage. Source: ${request.source}")
            BootTracker.updateStage(context, "SMS_IMPORT_DIRECT_BOOT | source=${request.source} | key=$idempotencyKey")
            saveToDeviceProtectedStorage(context, request, idempotencyKey)
            return false
        }

        try {
            val smsDao = ServiceLocator.database.smsDao()

            val exists = smsDao.existsByIdempotencyKey(idempotencyKey)
            if (exists) {
                val skipMessage = "SMS_DUPLICATE_SKIPPED | source=${request.source} | key=$idempotencyKey"
                Log.i(TAG, skipMessage)
                BootTracker.updateStage(context, skipMessage)
                return false
            }

            val smsEntity = SmsEntity(
                phoneNumber = request.phoneNumber,
                messageBody = request.messageBody,
                direction = "incoming",
                status = "pending_upload",
                messageRef = UUID.randomUUID().toString(),
                subscriptionId = request.subscriptionId,
                sentAt = request.sentAt,
                idempotencyKey = idempotencyKey
            )

            val insertedRowId = smsDao.insert(smsEntity)
            if (insertedRowId > 0) {
                val successMessage = "SMS_DB_INSERT | rowId=$insertedRowId | source=${request.source} | key=$idempotencyKey"
                Log.i(TAG, successMessage)
                BootTracker.updateStage(context, successMessage)
                return true
            } else {
                val conflictMessage = "SMS_DUPLICATE_SKIPPED (DB Conflict) | source=${request.source} | key=$idempotencyKey"
                Log.w(TAG, conflictMessage)
                BootTracker.updateStage(context, conflictMessage)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed database write under unlocked state, falling back to file storage: ${e.message}")
            saveToDeviceProtectedStorage(context, request, idempotencyKey)
            return false
        }
    }

    private fun saveToDeviceProtectedStorage(context: Context, request: SmsImportRequest, idempotencyKey: String) {
        try {
            val deviceProtectedContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
            val directory = java.io.File(deviceProtectedContext.filesDir, "direct_boot_sms")
            if (!directory.exists()) directory.mkdirs()

            val fileName = "sms_${idempotencyKey}.json"
            val file = java.io.File(directory, fileName)
            if (file.exists()) {
                Log.i(TAG, "File $fileName already exists in Direct Boot storage. Skipping duplicate write.")
                return
            }

            val json = com.google.gson.JsonObject().apply {
                addProperty("phoneNumber", request.phoneNumber)
                addProperty("messageBody", request.messageBody)
                addProperty("subscriptionId", request.subscriptionId)
                addProperty("sentAt", request.sentAt)
                addProperty("idempotencyKey", idempotencyKey)
                addProperty("source", request.source)
                addProperty("androidSmsId", request.androidSmsId ?: "")
            }

            file.writeText(json.toString())
            Log.i(TAG, "Saved SMS to Direct Boot storage: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save SMS to Direct Boot storage: ${e.message}")
        }
    }
}
