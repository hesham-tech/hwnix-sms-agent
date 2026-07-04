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

    /**
     * التحقق مما إذا كان جهاز المستخدم يدعم إعدادات التشغيل التلقائي المخصصة (Autostart).
     */
    fun isAutostartAvailable(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
               manufacturer.contains("oppo") ||
               manufacturer.contains("vivo") ||
               manufacturer.contains("huawei") ||
               manufacturer.contains("samsung")
    }

    /**
     * فتح صفحة إعدادات التشغيل التلقائي الخاصة بالشركة المصنعة (OEM).
     */
    fun requestAutostartPermission() {
        val intent = Intent()
        val manufacturer = Build.MANUFACTURER.lowercase()
        var componentName: android.content.ComponentName? = null

        when {
            manufacturer.contains("xiaomi") -> {
                componentName = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            manufacturer.contains("oppo") -> {
                componentName = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            manufacturer.contains("vivo") -> {
                componentName = android.content.ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            manufacturer.contains("huawei") -> {
                componentName = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
                )
            }
            manufacturer.contains("samsung") -> {
                componentName = android.content.ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
        }

        if (componentName != null) {
            intent.component = componentName
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppDetailsSettings()
            }
        } else {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}
