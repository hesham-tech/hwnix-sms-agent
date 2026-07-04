package com.hwnix.smsagent.manager.battery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

// مسؤولية هذا الكلاس: مراقبة حالة تحسين استهلاك البطارية وطلب الاستثناءات لضمان التشغيل المستمر بالخلفية
class BatteryManager(private val context: Context) {

    /**
     * فحص ما إذا كان تحسين البطارية فعالاً ومقيداً للتطبيق.
     */
    fun isBatteryOptimizationActive(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * طلب استثناء من تحسينات البطارية بفتح واجهة إعدادات النظام.
     */
    fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general battery saver settings if direct request fails
                    val fallbackIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                }
            }
        }
    }
}
