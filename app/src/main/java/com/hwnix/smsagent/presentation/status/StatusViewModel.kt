package com.hwnix.smsagent.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.domain.repository.DeviceRepository
import com.hwnix.smsagent.domain.usecase.CheckUpdateUseCase
import com.hwnix.smsagent.domain.usecase.SyncSimLinesUseCase
import com.hwnix.smsagent.manager.battery.BatteryManager
import com.hwnix.smsagent.manager.sim.SimManager
import com.hwnix.smsagent.manager.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusViewModel(
    private val sessionManager: SessionManager,
    private val deviceRepository: DeviceRepository,
    private val checkUpdateUseCase: CheckUpdateUseCase,
    private val syncSimLinesUseCase: SyncSimLinesUseCase,
    private val updateManager: UpdateManager,
    private val batteryManager: BatteryManager,
    private val simManager: SimManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        refreshDeviceInfo()
    }

    fun refreshAll(currentVersionCode: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // تحديث معلومات الجهاز محلياً وفحص تحسين البطارية
            refreshDeviceInfo()
            
            // فحص وجود تحديث جديد من السيرفر
            try {
                val result = checkUpdateUseCase.execute()
                if (result.isSuccess) {
                    val update = result.getOrNull()
                    if (update != null && update.versionCode > currentVersionCode) {
                        _uiState.update {
                            it.copy(
                                updateVersionName = update.versionName,
                                updateDownloadUrl = update.downloadUrl,
                                showUpdateDialog = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore update failures during pull-to-refresh
            }
            
            // مزامنة أرقام الخطوط المخزنة
            try {
                val deviceId = sessionManager.getDeviceId()
                if (deviceId != -1L) {
                    deviceRepository.getDeviceLines(deviceId)
                }
            } catch (e: Exception) {
                // ignore line fetch failures during refresh
            }

            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshDeviceInfo() {
        val deviceIdVal = sessionManager.getDeviceId().let { id ->
            if (id == -1L) "غير مسجل" else id.toString()
        }
        val isFirstSetupVal = !sessionManager.isSetupComplete()
        
        val lastSyncTime = sessionManager.getLastSyncSuccessTime()
        val isOnline = lastSyncTime > 0L && (System.currentTimeMillis() - lastSyncTime < 180000)
        val connStatus = if (sessionManager.getAuthToken() == null) {
            "غير متصل"
        } else if (isOnline) {
            "Online"
        } else {
            "Offline"
        }

        _uiState.update {
            it.copy(
                connectionStatus = connStatus,
                deviceId = deviceIdVal,
                deviceUuid = sessionManager.getDeviceUuid(),
                configVersion = sessionManager.getConfigVersion().toString(),
                gatewayName = sessionManager.getGatewayName(),
                isFirstSetup = isFirstSetupVal,
                isBatteryOptimized = batteryManager.isBatteryOptimizationActive(),
                isAutostartAvailable = batteryManager.isAutostartAvailable()
            )
        }
        
        // إذا لم يتم الإعداد بعد الدخول لأول مرة
        if (isFirstSetupVal && sessionManager.getAuthToken() != null) {
            openSimSetupDialog()
        }
    }

    fun updateConnectionStatus(status: String) {
        _uiState.update { it.copy(connectionStatus = status) }
    }

    fun checkBatteryOptimization() {
        _uiState.update {
            it.copy(
                isBatteryOptimized = batteryManager.isBatteryOptimizationActive(),
                isAutostartAvailable = batteryManager.isAutostartAvailable()
            )
        }
    }

    fun disableBatteryOptimization() {
        batteryManager.requestIgnoreBatteryOptimizations()
    }

    fun disableAutostartRestriction() {
        batteryManager.requestAutostartPermission()
    }

    fun openUpdateDialog(versionName: String, downloadUrl: String) {
        _uiState.update {
            it.copy(
                showUpdateDialog = true,
                updateVersionName = versionName,
                updateDownloadUrl = downloadUrl
            )
        }
    }

    fun dismissUpdateDialog() {
        if (!_uiState.value.isDownloadingUpdate) {
            _uiState.update { it.copy(showUpdateDialog = false) }
        }
    }

    fun downloadAndInstallUpdate() {
        val state = _uiState.value
        if (state.updateDownloadUrl.isBlank()) return

        _uiState.update { it.copy(isDownloadingUpdate = true, updateDownloadProgress = 0f) }
        viewModelScope.launch {
            val apkFile = updateManager.downloadApk(
                downloadUrl = state.updateDownloadUrl,
                versionName = state.updateVersionName
            ) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }

            _uiState.update { it.copy(isDownloadingUpdate = false) }
            if (apkFile != null) {
                _uiState.update { it.copy(showUpdateDialog = false) }
                updateManager.installApk(apkFile)
            } else {
                _uiState.update { it.copy(errorMessage = "فشل تنزيل ملف التحديث.") }
            }
        }
    }

    fun scanLocalUpdates(currentVersionCode: Int) {
        val (apkFile, versionName) = updateManager.scanLocalUpdates(currentVersionCode)
        _uiState.update {
            it.copy(
                localUpdateApk = apkFile,
                localUpdateVersionName = versionName
            )
        }
    }

    fun checkForUpdate(currentVersionCode: Int) {
        _uiState.update { it.copy(isCheckingUpdate = true, updateStatusMessage = null) }
        viewModelScope.launch {
            val result = checkUpdateUseCase.execute()
            if (result.isSuccess) {
                val update = result.getOrNull()
                if (update != null && update.versionCode > currentVersionCode) {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateVersionName = update.versionName,
                            updateDownloadUrl = update.downloadUrl,
                            showUpdateDialog = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateStatusMessage = "✅ التطبيق محدّث. لا يوجد إصدار جديد."
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateStatusMessage = "⚠️ تعذّر الاتصال بالسيرفر لفحص التحديثات."
                    )
                }
            }
        }
    }

    fun installLocalApk(file: java.io.File) {
        updateManager.installApk(file)
    }

    fun openSimSetupDialog() {

        _uiState.update { it.copy(isSavingSims = false, simSaveResult = null) }
        viewModelScope.launch {
            val sims = simManager.getActiveSimCards()
            val savedLinesResult = deviceRepository.getDeviceLines(sessionManager.getDeviceId())
            val savedLines = savedLinesResult.getOrDefault(emptyMap())

            val phoneInputs = sims.associate { sim ->
                val savedPhone = savedLines[sim.slotIndex]?.second
                val rawPhone = if (!savedPhone.isNullOrBlank()) savedPhone else sim.phoneNumber
                sim.slotIndex to simManager.cleanPhoneNumber(rawPhone)
            }
            val carrierInputs = sims.associate { sim ->
                val savedCarrier = savedLines[sim.slotIndex]?.first
                val carrierVal = if (!savedCarrier.isNullOrBlank()) savedCarrier else sim.carrier
                sim.slotIndex to (if (carrierVal == "Unknown") "" else carrierVal)
            }

            _uiState.update {
                it.copy(
                    detectedSims = sims,
                    simPhoneInputs = phoneInputs,
                    simCarrierInputs = carrierInputs,
                    showSimDialog = true
                )
            }
        }
    }

    fun onGatewayNameChange(name: String) {
        _uiState.update { it.copy(gatewayName = name) }
    }

    fun onSimPhoneChange(slotIndex: Int, value: String) {
        val currentInputs = _uiState.value.simPhoneInputs.toMutableMap()
        currentInputs[slotIndex] = value
        _uiState.update { it.copy(simPhoneInputs = currentInputs) }
    }

    fun onSimCarrierChange(slotIndex: Int, value: String) {
        val currentInputs = _uiState.value.simCarrierInputs.toMutableMap()
        currentInputs[slotIndex] = value
        _uiState.update { it.copy(simCarrierInputs = currentInputs) }
    }

    fun dismissSimDialog() {
        if (!_uiState.value.isFirstSetup) {
            _uiState.update { it.copy(showSimDialog = false) }
        }
    }

    fun saveSimSetup() {
        val state = _uiState.value
        _uiState.update { it.copy(isSavingSims = true, simSaveResult = null) }
        viewModelScope.launch {
            val cards = state.detectedSims.map { sim ->
                sim.copy(
                    carrier = state.simCarrierInputs[sim.slotIndex]?.ifBlank { "Unknown" } ?: "Unknown",
                    phoneNumber = simManager.cleanPhoneNumber(state.simPhoneInputs[sim.slotIndex] ?: "")
                )
            }

            val syncResult = deviceRepository.syncLines(cards)
            if (syncResult.isSuccess) {
                sessionManager.saveGatewayName(state.gatewayName)
                sessionManager.markSetupComplete()
                _uiState.update {
                    it.copy(
                        isSavingSims = false,
                        isFirstSetup = false,
                        simSaveResult = "✅ تم حفظ الإعدادات بنجاح!"
                    )
                }
                // إغلاق التلقائي بعد ثانيتين
                withContext(Dispatchers.IO) {
                    Thread.sleep(1500)
                }
                _uiState.update { it.copy(showSimDialog = false) }
                refreshDeviceInfo()
            } else {
                val err = syncResult.exceptionOrNull()?.message ?: "فشل المزامنة"
                _uiState.update {
                    it.copy(
                        isSavingSims = false,
                        simSaveResult = "❌ $err"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            val deviceId = sessionManager.getDeviceId()
            if (deviceId != -1L) {
                try {
                    deviceRepository.decoupleDevice(deviceId)
                } catch (e: Exception) {
                    // تجاهل الأخطاء لضمان تسجيل الخروج المحلي في حال انقطاع الشبكة
                }
            }
            sessionManager.clearSession()
            onLogoutSuccess()
        }
    }

    fun performFullSync(syncEngine: com.hwnix.smsagent.data.local.SyncEngine) {
        _uiState.update { it.copy(connectionStatus = "جاري المزامنة...") }
        viewModelScope.launch {
            val syncSuccess = withContext(Dispatchers.IO) {
                try {
                    syncEngine.performFullSync()
                    true
                } catch (e: Exception) {
                    false
                }
            }
            _uiState.update { it.copy(connectionStatus = if (syncSuccess) "Online" else "Offline") }
        }
    }
}

