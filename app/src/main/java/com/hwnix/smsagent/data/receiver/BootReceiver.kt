package com.hwnix.smsagent.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hwnix.smsagent.data.service.AgentForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_POWER_CONNECTED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("BootReceiver", "System event detected (Action: $action). Starting background services...")
            
            // إطلاق الخدمة الأمامية لإيقاظ الهاتف والاتصال
            val serviceIntent = Intent(context, AgentForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start foreground service on system event: ${e.message}", e)
            }
        }
    }
}
