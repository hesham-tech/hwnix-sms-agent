package com.hwnix.smsagent.data.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.JsonObject
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

// مستقبل تأكيد إرسال وتسليم الرسائل — يُحدِّث حالة الرسالة على السيرفر
class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsDeliveryReceiver"
        const val ACTION_SMS_SENT = "com.hwnix.smsagent.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.hwnix.smsagent.SMS_DELIVERED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val commandId = intent.getLongExtra("command_id", -1L)
        val messageId = intent.getLongExtra("message_id", -1L)
        val phoneNumber = intent.getStringExtra("phone_number") ?: ""
        val messageBody = intent.getStringExtra("message_body") ?: ""
        val capturedResultCode = resultCode // Capture immediately on the main thread!

        val extrasString = StringBuilder()
        intent.extras?.keySet()?.forEach { key ->
            extrasString.append("$key=${intent.extras?.get(key)}; ")
        }
        Log.i(TAG, "TRACE 1.5: SmsDeliveryReceiver.onReceive() triggered. action=$action, resultCode=$capturedResultCode, command_id=$commandId, message_id=$messageId, extras={$extrasString}")

        android.widget.Toast.makeText(
            context,
            "📩 Receiver Action: $action\ncmd: $commandId, msg: $messageId",
            android.widget.Toast.LENGTH_LONG
        ).show()

        if (commandId == -1L || messageId == -1L) return

        val pendingResult = goAsync()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // إرسال اللوج الخلفي أولاً
                sendRemoteLog(context, "TRACE 1.5", "SmsDeliveryReceiver.onReceive() triggered. action=$action, resultCode=$capturedResultCode, command_id=$commandId, message_id=$messageId, extras={$extrasString}", commandId, messageId)
                
                when (action) {
                    ACTION_SMS_SENT -> {
                        val status = when (capturedResultCode) {
                            Activity.RESULT_OK -> "executed"
                            else -> "failed"
                        }
                        Log.i(TAG, "SMS_SENT — cmd: $commandId, msg: $messageId, status: $status")
                        if (status == "executed" && phoneNumber.isNotBlank() && messageBody.isNotBlank()) {
                            writeSmsToSentBox(context, phoneNumber, messageBody)
                        }
                        reportCommandExecution(context, commandId, messageId, status, capturedResultCode)
                    }
                    ACTION_SMS_DELIVERED -> {
                        Log.i(TAG, "SMS_DELIVERED — cmd: $commandId, msg: $messageId")
                        updateMessageStatus(context, commandId, messageId, "delivered")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun writeSmsToSentBox(context: Context, phoneNumber: String, messageBody: String) {
        try {
            val values = android.content.ContentValues().apply {
                put("address", phoneNumber)
                put("body", messageBody)
                put("date", System.currentTimeMillis())
                put("read", 1) // 1 means read
                put("type", 2) // 2 means MESSAGE_TYPE_SENT
            }
            context.contentResolver.insert(android.net.Uri.parse("content://sms/sent"), values)
            Log.i(TAG, "Successfully wrote sent SMS to system sent box")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sent SMS to system sent box: ${e.message}")
        }
    }

    private suspend fun reportCommandExecution(context: Context, commandId: Long, messageId: Long, status: String, resultCode: Int) {
        try {
            val apiService = ApiClient.getService(context)
            val sessionManager = com.hwnix.smsagent.core.di.ServiceLocator.sessionManager
            val errorMsg = if (status == "failed") {
                getSmsErrorString(resultCode)
            } else null

            val responsePayload = JsonObject().apply {
                addProperty("message_id", messageId)
                if (errorMsg != null) {
                    addProperty("error", errorMsg)
                }
            }

            val payload = JsonObject().apply {
                addProperty("device_id", sessionManager.getDeviceId())
                addProperty("status", status)
                add("response_payload", responsePayload)
            }

            val key = "CMD_EXEC_REP_${commandId}_${status}"
            Log.i(TAG, "TRACE 1.6: BEFORE calling API executeCommand, commandId=$commandId, key=$key, payload=$payload")
            sendRemoteLog(context, "TRACE 1.6", "BEFORE calling API executeCommand, commandId=$commandId, key=$key", commandId, messageId)
            
            try {
                val response = apiService.executeCommand(commandId, key, payload)
                if (response.isSuccessful) {
                    Log.i(TAG, "TRACE 1.7: API executeCommand succeeded. code=${response.code()}, body=${response.body()}")
                    sendRemoteLog(context, "TRACE 1.7", "API executeCommand succeeded. code=${response.code()}, body=${response.body()}", commandId, messageId)
                } else {
                    Log.e(TAG, "TRACE 1.7: API executeCommand failed. code=${response.code()}, errorBody=${response.errorBody()?.string()}")
                    sendRemoteLog(context, "TRACE 1.7", "API executeCommand failed. code=${response.code()}, errorBody=${response.errorBody()?.string()}", commandId, messageId)
                }
            } catch (apiEx: Exception) {
                Log.e(TAG, "TRACE 1.7: API executeCommand thrown exception: ${apiEx.message}", apiEx)
                sendRemoteLog(context, "TRACE 1.7", "API executeCommand thrown exception: ${apiEx.message}", commandId, messageId)
                throw apiEx
            }
            
            Log.i(TAG, "Command $commandId execution status reported: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report command execution: ${e.message}")
        }
    }

    private fun getSmsErrorString(resultCode: Int): String {
        return when (resultCode) {
            android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
            android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
            android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
            android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
            else -> "Unknown SMS error code: $resultCode"
        }
    }

    private suspend fun updateMessageStatus(context: Context, commandId: Long, messageId: Long, status: String) {
        try {
            val apiService = ApiClient.getService(context)
            val sessionManager = com.hwnix.smsagent.core.di.ServiceLocator.sessionManager
            val payload = JsonObject().apply {
                addProperty("device_id", sessionManager.getDeviceId())
                addProperty("message_id", messageId)
                addProperty("status", status)
            }
            val key = "STATUS_${commandId}_${status}_${UUID.randomUUID()}"
            
            Log.i(TAG, "TRACE 1.8: BEFORE calling API syncSmsStatus, commandId=$commandId, messageId=$messageId, status=$status")
            sendRemoteLog(context, "TRACE 1.8", "BEFORE calling API syncSmsStatus, commandId=$commandId, messageId=$messageId, status=$status", commandId, messageId)
            
            try {
                val response = apiService.syncSmsStatus(key, payload)
                if (response.isSuccessful) {
                    Log.i(TAG, "TRACE 1.9: API syncSmsStatus succeeded. code=${response.code()}, body=${response.body()}")
                    sendRemoteLog(context, "TRACE 1.9", "API syncSmsStatus succeeded. code=${response.code()}, body=${response.body()}", commandId, messageId)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "TRACE 1.9: API syncSmsStatus failed. code=${response.code()}, error=$errorBody")
                    sendRemoteLog(context, "TRACE 1.9", "API syncSmsStatus failed. code=${response.code()}, error=$errorBody", commandId, messageId)
                }
            } catch (apiEx: Exception) {
                Log.e(TAG, "TRACE 1.9: API syncSmsStatus thrown exception: ${apiEx.message}", apiEx)
                sendRemoteLog(context, "TRACE 1.9", "API syncSmsStatus thrown exception: ${apiEx.message}", commandId, messageId)
                throw apiEx
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status: ${e.message}")
        }
    }

    private fun sendRemoteLog(context: Context, tag: String, message: String, commandId: Long = -1L, messageId: Long = -1L) {
        val sessionManager = com.hwnix.smsagent.core.di.ServiceLocator.sessionManager
        val deviceId = sessionManager.getDeviceId()
        if (deviceId == -1L) return
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getService(context)
                val payload = JsonObject().apply {
                    addProperty("device_id", deviceId)
                    addProperty("tag", tag)
                    addProperty("message", message)
                    val detailsObj = JsonObject().apply {
                        addProperty("client_timestamp", System.currentTimeMillis())
                        addProperty("thread", Thread.currentThread().name)
                        if (commandId != -1L) addProperty("command_id", commandId)
                        if (messageId != -1L) addProperty("message_id", messageId)
                    }
                    add("details", detailsObj)
                }
                apiService.logDiagnostic(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send remote log: ${e.message}")
            }
        }
    }
}
