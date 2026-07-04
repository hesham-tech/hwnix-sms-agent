package com.hwnix.smsagent.manager.sim

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.hwnix.smsagent.domain.model.SimCard

// مدير إدارة شرائح الاتصال وكشفها في الهاتف
class SimManager(private val context: Context) {

    companion object {
        private const val TAG = "SimManager"
    }

    /**
     * تنظيف رقم الهاتف: حذف مفتاح الدولة (+20) وعلامة + للحصول على الصيغة المحلية.
     */
    fun cleanPhoneNumber(phone: String): String {
        var cleaned = phone.trim()
        if (cleaned.startsWith("+20")) cleaned = "0" + cleaned.removePrefix("+20")
        else if (cleaned.startsWith("+")) cleaned = "0" + cleaned.removePrefix("+")
        return cleaned
    }

    /**
     * قراءة الشرائح النشطة من الهاتف.
     */
    @SuppressLint("MissingPermission")
    fun getActiveSimCards(): List<SimCard> {
        val result = mutableListOf<SimCard>()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                        as? SubscriptionManager ?: return emptyList()
                val activeInfoList = subManager.activeSubscriptionInfoList ?: return emptyList()
                for (info in activeInfoList) {
                    val mccVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.mccString ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        info.mcc?.toString() ?: ""
                    }
                    val mncVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.mncString ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        info.mnc?.toString() ?: ""
                    }
                    result.add(
                        SimCard(
                            slotIndex = info.simSlotIndex,
                            subscriptionId = info.subscriptionId.toString(),
                            carrier = if (info.carrierName?.toString().isNullOrBlank()) "Unknown" else info.carrierName.toString(),
                            phoneNumber = cleanPhoneNumber(info.number ?: ""),
                            mcc = mccVal,
                            mnc = mncVal
                        )
                    )
                }
            } else {
                // Fallback للأجهزة القديمة
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val number = tm?.line1Number ?: ""
                val carrierName = tm?.networkOperatorName
                val carrier = if (carrierName.isNullOrBlank()) "Unknown" else carrierName
                result.add(
                    SimCard(
                        slotIndex = 0,
                        subscriptionId = "-1",
                        carrier = carrier,
                        phoneNumber = cleanPhoneNumber(number),
                        mcc = "",
                        mnc = ""
                    )
                )
            }
            result
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to read SIM info: ${e.message}")
            emptyList()
        }
    }
}
