package com.hwnix.smsagent.presentation.status

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hwnix.smsagent.core.di.ServiceLocator
import com.hwnix.smsagent.data.local.BootTracker
import com.hwnix.smsagent.data.local.ServiceHealthMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: اختبار التحقق النهائي من خلو الخيط الرئيسي من عمليات حظر القرص أو التثقيل المتسبب بالـ ANR */
class MainThreadAnrShieldTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.initialize(context)
    }

    @Test
    fun bootTracker_whenDiagnosticsDisabled_returnsInstantMapWithoutDiskIo() {
        assertFalse(BootTracker.ENABLE_DIAGNOSTICS)

        val startTime = System.currentTimeMillis()
        val diagnostics = BootTracker.getDiagnostics(context)
        val executionTime = System.currentTimeMillis() - startTime

        assertNotNull(diagnostics)
        assertEquals("DISABLED", diagnostics["last_action"])
        assertEquals("DIAGNOSTICS_DISABLED", diagnostics["stage_log"])
        assertTrue("Execution time must be under 10ms to prevent ANR", executionTime < 10L)
    }

    @Test
    fun serviceHealthMonitor_getHealth_executesInstantlyOnMainThread() {
        val startTime = System.currentTimeMillis()
        val health = ServiceHealthMonitor.getHealth()
        val executionTime = System.currentTimeMillis() - startTime

        assertNotNull(health)
        assertTrue("Health check must execute instantly under 5ms", executionTime < 5L)
    }
}
