package com.hwnix.cash.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.domain.repository.DeviceRepository
import com.hwnix.cash.domain.usecase.CheckUpdateUseCase
import com.hwnix.cash.domain.usecase.SyncSimLinesUseCase
import com.hwnix.cash.manager.battery.BatteryManager
import com.hwnix.cash.manager.sim.SimManager
import com.hwnix.cash.manager.update.UpdateManager
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
        
        viewModelScope.launch {
            com.hwnix.cash.data.local.ServiceHealthMonitor.healthFlow.collect { health ->
                val connStatus = when {
                    sessionManager.getAuthToken() == null -> "غير متصل"
                    !health.isInternetAvailable -> "غير متصل (لا يوجد انترنت)"
                    health.overallHealth == com.hwnix.cash.data.local.ServiceHealthState.BROKEN -> "غير متصل (الخدمة معطلة)"
                    health.isInternetAvailable -> "متصل"
                    else -> "غير متصل"
                }
                _uiState.update { it.copy(connectionStatus = connStatus) }
            }
        }
        
        viewModelScope.launch {
            com.hwnix.cash.core.di.ServiceLocator.syncEvents.collect {
                refreshDeviceInfo()
            }
        }
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
                    val linesResult = deviceRepository.getDeviceLines(deviceId)
                    if (linesResult.isSuccess) {
                        _uiState.update { it.copy(deviceLines = linesResult.getOrDefault(emptyMap())) }
                    }
                }
            } catch (e: Exception) {
                // ignore line fetch failures during refresh
            }

            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceIdVal = sessionManager.getDeviceId().let { id ->
                if (id == -1L) "غير مسجل" else id.toString()
            }
            val isFirstSetupVal = !sessionManager.isSetupComplete()
            
            val health = com.hwnix.cash.data.local.ServiceHealthMonitor.getHealth()
            val lastSyncTime = sessionManager.getLastSyncSuccessTime()
            
            val connStatus = when {
                sessionManager.getAuthToken() == null -> "🔴 غير متصل"
                !health.isInternetAvailable -> "🔴 غير متصل (لا يوجد إنترنت)"
                !health.isServerReachable -> "🟠 تعذّر الوصول للسيرفر"
                health.overallHealth == com.hwnix.cash.data.local.ServiceHealthState.BROKEN -> "🔴 غير متصل (الخدمة متوقفة)"
                health.pendingSmsCount > 0 -> "🟡 متصل (توجد ${health.pendingSmsCount} رسائل معلقة)"
                health.isInternetAvailable -> "🟢 متصل"
                else -> "🔴 غير متصل"
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
            
            // جلب الخطوط
            if (deviceIdVal != "غير مسجل") {
                val linesResult = deviceRepository.getDeviceLines(sessionManager.getDeviceId())
                if (linesResult.isSuccess) {
                    _uiState.update { it.copy(deviceLines = linesResult.getOrDefault(emptyMap())) }
                }
                checkWallets { }
            }

            // إذا لم يتم الإعداد بعد الدخول لأول مرة
            if (isFirstSetupVal && sessionManager.getAuthToken() != null) {
                withContext(Dispatchers.Main) {
                    openSimSetupDialog()
                }
            }
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
                val savedPhone = savedLines[sim.slotIndex]?.phoneNumber
                val rawPhone = if (!savedPhone.isNullOrBlank()) savedPhone else sim.phoneNumber
                sim.slotIndex to simManager.cleanPhoneNumber(rawPhone)
            }
            val carrierInputs = sims.associate { sim ->
                val savedCarrier = savedLines[sim.slotIndex]?.carrier
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
        _uiState.update { it.copy(showSimDialog = false) }
    }

    fun saveSimSetup() {
        val state = _uiState.value
        _uiState.update { it.copy(isSavingSims = true, simSaveResult = null) }
        viewModelScope.launch {
            sessionManager.saveGatewayName(state.gatewayName)

            val cards = state.detectedSims.map { sim ->
                sim.copy(
                    carrier = state.simCarrierInputs[sim.slotIndex]?.ifBlank { "Unknown" } ?: "Unknown",
                    phoneNumber = simManager.cleanPhoneNumber(state.simPhoneInputs[sim.slotIndex] ?: "")
                )
            }

            val syncResult = deviceRepository.syncLines(cards)
            if (syncResult.isSuccess) {
                sessionManager.markSetupComplete()
                _uiState.update {
                    it.copy(
                        isSavingSims = false,
                        isFirstSetup = false,
                        simSaveResult = "✅ تم حفظ الإعدادات بنجاح!"
                    )
                }
                // إغلاق التلقائي بعد ثانيتين
                kotlinx.coroutines.delay(1500)
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
            com.hwnix.cash.core.di.ServiceLocator.clearAllAppData()
            onLogoutSuccess()
        }
    }

    fun performFullSync(syncEngine: com.hwnix.cash.data.local.SyncEngine) {
        val health = com.hwnix.cash.data.local.ServiceHealthMonitor.getHealth()
        if (!health.isInternetAvailable) {
            _uiState.update { it.copy(connectionStatus = "غير متصل (لا يوجد إنترنت)", errorMessage = "⚠️ تعذّر المزامنة: لا يوجد اتصال بالإنترنت.") }
            return
        }

        _uiState.update { it.copy(connectionStatus = "جاري المزامنة...") }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    syncEngine.performFullSync()
                } catch (e: Exception) {
                    /* ignore */
                }
            }
            refreshDeviceInfo()
        }
    }

    fun checkWallets(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = com.hwnix.cash.data.remote.ApiClient.getService().getWallets()
                if (response.isSuccessful) {
                    val body = response.body()
                    val hasWallets = if (body?.has("data") == true && body.get("data").isJsonArray) {
                        body.getAsJsonArray("data").size() > 0
                    } else {
                        false
                    }
                    val wallets = mutableListOf<com.hwnix.cash.domain.model.FinancialAccount>()
                    if (hasWallets) {
                        body?.getAsJsonArray("data")?.forEach { element ->
                            val obj = element.asJsonObject
                            wallets.add(
                                com.hwnix.cash.domain.model.FinancialAccount(
                                    id = obj.get("id").asInt,
                                    name = obj.get("name")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
                                    senderIdentifier = obj.get("sender")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
                                    simPhone = obj.get("line_number")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
                                    dailyWithdrawLimit = obj.get("daily_withdraw_limit")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0,
                                    dailyDepositLimit = obj.get("daily_deposit_limit")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0,
                                    monthlyWithdrawLimit = obj.get("monthly_withdraw_limit")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0,
                                    monthlyDepositLimit = obj.get("monthly_deposit_limit")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0
                                )
                            )
                        }
                    }
                    _uiState.update { it.copy(wallets = wallets) }
                    sessionManager.setWalletMissing(!hasWallets)
                    if (hasWallets) {
                        sessionManager.markSetupComplete()
                    }
                    onResult(hasWallets)
                } else {
                    // Fallback to status screen if API fails
                    onResult(true)
                }
            } catch (e: Exception) {
                onResult(true)
            }
        }
    }

    // --- وظائف تسوية الرصيد ---

    fun openReconcileDialog(slotIndex: Int) {
        val lineData = _uiState.value.deviceLines[slotIndex]
        val actualBalance = lineData?.totalActualBalance ?: 0.0
        val bookBalance = lineData?.totalBalance ?: 0.0
        val diff = actualBalance - bookBalance
        val serverCarrier = lineData?.carrier ?: ""
        val hardwareCarrier = _uiState.value.detectedSims.find { it.slotIndex == slotIndex }?.carrier ?: ""
        val lineName = com.hwnix.cash.utils.LineNameHelper.resolveLineName(serverCarrier, hardwareCarrier, slotIndex)
        
        _uiState.update { 
            it.copy(
                showReconcileDialog = true,
                reconcileSlotIndex = slotIndex,
                reconcileTargetBalance = actualBalance.toString(),
                reconcileNote = "تسوية بعد مراجعة الرصيد الفعلي",
                reconcileResult = null,
                reconcileLineName = lineName,
                reconcileBookBalance = bookBalance,
                reconcileActualBalance = actualBalance,
                reconcileDiff = diff
            ) 
        }
    }

    fun dismissReconcileDialog() {
        _uiState.update { it.copy(showReconcileDialog = false) }
    }

    fun onReconcileTargetBalanceChange(value: String) {
        _uiState.update { it.copy(reconcileTargetBalance = value) }
    }

    fun onReconcileNoteChange(value: String) {
        _uiState.update { it.copy(reconcileNote = value) }
    }

    fun reconcileLine() {
        val targetBalance = _uiState.value.reconcileTargetBalance.toDoubleOrNull()
        if (targetBalance == null || targetBalance < 0) {
            _uiState.update { it.copy(reconcileResult = "❌ رجاءً أدخل رصيداً صحيحاً.") }
            return
        }

        val note = _uiState.value.reconcileNote
        val slotIndex = _uiState.value.reconcileSlotIndex

        _uiState.update { it.copy(isReconciling = true, reconcileResult = null) }
        viewModelScope.launch {
            val result = deviceRepository.reconcileLine(slotIndex, targetBalance, note)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconcileResult = "✅ تمت التسوية بنجاح."
                    )
                }
                kotlinx.coroutines.delay(1500)
                _uiState.update { it.copy(showReconcileDialog = false) }
                // Refresh lines to get updated balance
                val deviceId = sessionManager.getDeviceId()
                if (deviceId != -1L) {
                    val linesResult = deviceRepository.getDeviceLines(deviceId)
                    if (linesResult.isSuccess) {
                        _uiState.update { it.copy(deviceLines = linesResult.getOrDefault(emptyMap())) }
                    }
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "فشل التسوية"
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconcileResult = "❌ $err"
                    )
                }
            }
        }
    }

    // --- وظائف حذف الخط ---

    fun openDeleteDialog(slotIndex: Int) {
        val serverCarrier = _uiState.value.deviceLines[slotIndex]?.carrier ?: ""
        val hardwareCarrier = _uiState.value.detectedSims.find { it.slotIndex == slotIndex }?.carrier ?: ""
        val lineName = com.hwnix.cash.utils.LineNameHelper.resolveLineName(serverCarrier, hardwareCarrier, slotIndex)

        _uiState.update { 
            it.copy(
                showDeleteDialog = true,
                deleteSlotIndex = slotIndex,
                deleteLineName = lineName,
                deleteResult = null
            ) 
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteLine() {
        val slotIndex = _uiState.value.deleteSlotIndex

        _uiState.update { it.copy(isDeleting = true, deleteResult = null) }
        viewModelScope.launch {
            val result = deviceRepository.deleteLine(slotIndex)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteResult = "✅ تم حذف الخط نهائياً بنجاح."
                    )
                }
                kotlinx.coroutines.delay(1500)
                _uiState.update { it.copy(showDeleteDialog = false) }
                // Refresh lines to update UI
                val deviceId = sessionManager.getDeviceId()
                if (deviceId != -1L) {
                    val linesResult = deviceRepository.getDeviceLines(deviceId)
                    if (linesResult.isSuccess) {
                        _uiState.update { it.copy(deviceLines = linesResult.getOrDefault(emptyMap())) }
                    }
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "فشل الحذف"
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteResult = "❌ $err"
                    )
                }
            }
        }
    }
}


