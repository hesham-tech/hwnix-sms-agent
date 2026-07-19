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

        android.widget.Toast.makeText(
            context,
            "📩 Receiver Action: $action\ncmd: $commandId, msg: $messageId",
            android.widget.Toast.LENGTH_LONG
        ).show()

        if (commandId == -1L || messageId == -1L) return

        when (intent.action) {
            ACTION_SMS_SENT -> {
                val status = when (resultCode) {
                    Activity.RESULT_OK -> "executed"
                    else -> "failed"
                }
                Log.i(TAG, "SMS_SENT — cmd: $commandId, msg: $messageId, status: $status")
                reportCommandExecution(context, commandId, messageId, status, resultCode)
            }
            ACTION_SMS_DELIVERED -> {
                Log.i(TAG, "SMS_DELIVERED — cmd: $commandId, msg: $messageId")
                updateMessageStatus(context, commandId, messageId, "delivered")
            }
        }
    }

    private fun reportCommandExecution(context: Context, commandId: Long, messageId: Long, status: String, resultCode: Int) {
        GlobalScope.launch(Dispatchers.IO) {
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
                apiService.executeCommand(commandId, key, payload)
                Log.i(TAG, "Command $commandId execution status reported: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report command execution: ${e.message}")
            }
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

    private fun updateMessageStatus(context: Context, commandId: Long, messageId: Long, status: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getService(context)
                val sessionManager = com.hwnix.smsagent.core.di.ServiceLocator.sessionManager
                val payload = JsonObject().apply {
                    addProperty("device_id", sessionManager.getDeviceId())
                    addProperty("message_id", messageId)
                    addProperty("status", status)
                }
                val key = "STATUS_${commandId}_${status}_${UUID.randomUUID()}"
                apiService.syncSmsStatus(key, payload)
                Log.i(TAG, "Status updated: message $messageId → $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update status: ${e.message}")
            }
        }
    }
}
