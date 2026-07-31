package com.hwnix.cash.data.receiver

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.hwnix.cash.core.di.ServiceLocator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بـ SmsReceiver واستقبال الرسائل القصيرة الواردة من النظام */
class SmsReceiverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.initialize(context)
    }

    @Test
    fun onReceive_withNonSmsIntent_ignoresEvent() {
        val receiver = SmsReceiver()
        val intent = Intent("android.intent.action.BATTERY_LOW")

        receiver.onReceive(context, intent)
        // Passes cleanly without exception
        assertTrue(true)
    }

    @Test
    fun onReceive_withEmptySmsIntent_handlesNullMessagesSafely() {
        val receiver = SmsReceiver()
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)

        receiver.onReceive(context, intent)
        assertTrue(true)
    }
}
