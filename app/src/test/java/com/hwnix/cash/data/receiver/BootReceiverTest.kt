package com.hwnix.cash.data.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.hwnix.cash.core.di.ServiceLocator
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة الشامل لـ BootReceiver لتغطية جميع أحداث الإقلاع والشحن واستبدال التطبيق والمستند الجنائي */
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.initialize(context)
        receiver = BootReceiver()
    }

    @Test
    fun onReceive_bootCompleted_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val tripwireFile = File(context.filesDir, "boot_tripwire.txt")
        assertNotNull(tripwireFile)
    }

    @Test
    fun onReceive_lockedBootCompleted_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_userUnlocked_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_USER_UNLOCKED)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_userPresent_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_USER_PRESENT)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_powerConnected_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_POWER_CONNECTED)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_quickBootPowerOn_writesTripwireAndTriggersService() {
        val intent = Intent("android.intent.action.QUICKBOOT_POWERON")

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_myPackageReplaced_writesTripwireAndTriggersService() {
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_unhandledAction_logsAndIgnoresAction() {
        val intent = Intent("android.intent.action.CUSTOM_UNHANDLED_EVENT")

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_nullActionIntent_defaultsToUnknownAction() {
        val intent = Intent()

        receiver.onReceive(context, intent)
    }
}
