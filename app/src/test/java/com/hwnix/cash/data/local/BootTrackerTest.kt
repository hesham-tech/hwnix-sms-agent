package com.hwnix.cash.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة لتتبع التشخيص ومراحل الإقلاع BootTracker ومسار الـ Feature Flag معطلاً ومفعلاً */
class BootTrackerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun logBootEvent_respectsEnableDiagnosticsFlag() {
        BootTracker.logBootEvent(context, "android.intent.action.BOOT_COMPLETED")
        BootTracker.updateStage(context, "STAGE_SYNC_COMPLETE")

        val diagnostics = BootTracker.getDiagnostics(context)

        assertNotNull(diagnostics)
        if (!BootTracker.ENABLE_DIAGNOSTICS) {
            assertEquals("DISABLED", diagnostics["last_action"])
            assertEquals("DISABLED", diagnostics["last_stage"])
        } else {
            assertEquals("android.intent.action.BOOT_COMPLETED", diagnostics["last_action"])
            assertEquals("STAGE_SYNC_COMPLETE", diagnostics["last_stage"])
        }
    }
}
