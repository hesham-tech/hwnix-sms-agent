package com.hwnix.cash.manager.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * تعليق عربي مختصر: فئة مساعدة لاكتشاف نوع الجهاز وتوجيه المستخدم لشاشات التشغيل التلقائي وحماية خلفية النظام (MIUI/Oppo/Vivo/Samsung).
 */
object OEMAutostartHelper {

    private const val TAG = "OEMAutostartHelper"

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.lowercase()
    }

    fun isXiaomi(): Boolean = getDeviceManufacturer().contains("xiaomi") || getDeviceManufacturer().contains("redmi") || getDeviceManufacturer().contains("poco")
    fun isSamsung(): Boolean = getDeviceManufacturer().contains("samsung")
    fun isHuawei(): Boolean = getDeviceManufacturer().contains("huawei") || getDeviceManufacturer().contains("honor")
    fun isOppo(): Boolean = getDeviceManufacturer().contains("oppo") || getDeviceManufacturer().contains("realme")
    fun isVivo(): Boolean = getDeviceManufacturer().contains("vivo")

    /**
     * فتح شاشة التشغيل التلقائي (Autostart) بحسب نوع جهاز المستخدم
     */
    fun openAutostartSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        // 1. Xiaomi / MIUI / HyperOS
        if (isXiaomi()) {
            intents.add(Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            })
            intents.add(Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementMain")
            })
        }

        // 2. OPPO / Realme
        if (isOppo()) {
            intents.add(Intent().apply {
                component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            })
            intents.add(Intent().apply {
                component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            })
        }

        // 3. Vivo
        if (isVivo()) {
            intents.add(Intent().apply {
                component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            })
            intents.add(Intent().apply {
                component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManagerActivity")
            })
        }

        // 4. Huawei / Honor
        if (isHuawei()) {
            intents.add(Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            })
            intents.add(Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            })
        }

        // محاولة تشغيل الإنتنت الخاص بالشركة
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
                    context.startActivity(intent)
                    Log.i(TAG, "Successfully opened OEM autostart screen via ${intent.component}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed intent for autostart: ${e.message}")
            }
        }

        // Fallback: فتح شاشة إعدادات التطبيق العامة
        return openAppDetailsSettings(context)
    }

    /**
     * فتح شاشة إعدادات البطارية وتخطي القيود (Ignore Battery Optimizations)
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open direct battery optimization intent: ${e.message}")
            return openAppDetailsSettings(context)
        }
    }

    /**
     * فتح شاشة تفاصيل التطبيق العامة للنظام
     */
    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details: ${e.message}")
            false
        }
    }
}
