package com.hwnix.cash.presentation.status

import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.domain.repository.DeviceRepository
import com.hwnix.cash.domain.usecase.CheckUpdateUseCase
import com.hwnix.cash.domain.usecase.SyncSimLinesUseCase
import com.hwnix.cash.manager.battery.BatteryManager
import com.hwnix.cash.manager.sim.SimManager
import com.hwnix.cash.manager.update.UpdateManager
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/* تعليق عربي مختصر: كلاس اختبارات الوحدة الخاص بـ StatusViewModel وتفاصيل حالة الواجهة والبطارية والشريحة */
class StatusViewModelTest {

    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockDeviceRepository: DeviceRepository
    private lateinit var mockCheckUpdateUseCase: CheckUpdateUseCase
    private lateinit var mockSyncSimLinesUseCase: SyncSimLinesUseCase
    private lateinit var mockUpdateManager: UpdateManager
    private lateinit var mockBatteryManager: BatteryManager
    private lateinit var mockSimManager: SimManager

    @Before
    fun setUp() {
        mockSessionManager = mockk(relaxed = true)
        mockDeviceRepository = mockk(relaxed = true)
        mockCheckUpdateUseCase = mockk(relaxed = true)
        mockSyncSimLinesUseCase = mockk(relaxed = true)
        mockUpdateManager = mockk(relaxed = true)
        mockBatteryManager = mockk(relaxed = true)
        mockSimManager = mockk(relaxed = true)
    }

    @Test
    fun statusUiState_defaultValues_areValid() {
        val state = StatusUiState()

        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals("غير متصل", state.connectionStatus)
    }

    @Test
    fun statusUiState_copyMutation_updatesState() {
        val state = StatusUiState()
        val updated = state.copy(connectionStatus = "متصل", gatewayName = "Gateway POCO X3")

        assertEquals("متصل", updated.connectionStatus)
        assertEquals("Gateway POCO X3", updated.gatewayName)
    }
}
