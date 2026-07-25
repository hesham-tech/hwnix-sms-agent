package com.hwnix.cash.data.service

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.hwnix.cash.core.di.ServiceLocator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة المحسن بـ Robolectric لإدارة كامل أفرع وأوامر ودورة حياة AgentForegroundService */
class AgentForegroundServiceTest {

    @Before
    fun setUp() {
        ServiceLocator.initialize(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun agentServiceState_enumVariants_areDefinedCorrectly() {
        val states = AgentForegroundService.AgentServiceState.values()
        assertTrue(states.contains(AgentForegroundService.AgentServiceState.CREATED))
        assertTrue(states.contains(AgentForegroundService.AgentServiceState.SYNCING))
        assertTrue(states.contains(AgentForegroundService.AgentServiceState.STOPPED))
    }

    @Test
    fun serviceLifecycle_createAndStartCommand_executesWithoutCrashing() {
        val controller = Robolectric.buildService(AgentForegroundService::class.java)
        val service = controller.create().get()

        assertNotNull(service)

        val intent = Intent(ApplicationProvider.getApplicationContext(), AgentForegroundService::class.java).apply {
            putExtra("launcher_source", "TEST_SUITE")
        }
        val startId = service.onStartCommand(intent, 0, 1)

        assertEquals(android.app.Service.START_STICKY, startId)

        controller.destroy()
    }

    @Test
    fun serviceLifecycle_startWithCustomIntent_returnsStartSticky() {
        val controller = Robolectric.buildService(AgentForegroundService::class.java)
        val service = controller.create().get()

        val intent = Intent(ApplicationProvider.getApplicationContext(), AgentForegroundService::class.java).apply {
            putExtra("force_sync", true)
        }
        val startId = service.onStartCommand(intent, 0, 1)

        assertEquals(android.app.Service.START_STICKY, startId)

        controller.destroy()
    }

    @Test
    fun onBind_returnsNull() {
        val controller = Robolectric.buildService(AgentForegroundService::class.java)
        val service = controller.create().get()

        val binder = service.onBind(Intent())

        assertNull(binder)

        controller.destroy()
    }
}
