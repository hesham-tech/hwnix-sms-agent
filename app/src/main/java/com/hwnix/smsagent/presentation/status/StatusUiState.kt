package com.hwnix.smsagent.presentation.status

import com.hwnix.smsagent.domain.model.SimCard

data class StatusUiState(
    val connectionStatus: String = "غير متصل",
    val deviceId: String = "غير مسجل",
    val deviceUuid: String = "",
    val configVersion: String = "1",
    val isBatteryOptimized: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAutostartAvailable: Boolean = false,
    
    // التحديثات
    val showUpdateDialog: Boolean = false,
    val updateVersionName: String = "",
    val updateDownloadUrl: String = "",
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val localUpdateApk: java.io.File? = null,
    val localUpdateVersionName: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateStatusMessage: String? = null,

    
    // إعداد الخطوط
    val showSimDialog: Boolean = false,
    val detectedSims: List<SimCard> = emptyList(),
    val simPhoneInputs: Map<Int, String> = emptyMap(),
    val simCarrierInputs: Map<Int, String> = emptyMap(),
    val isSavingSims: Boolean = false,
    val simSaveResult: String? = null,
    val isFirstSetup: Boolean = false,
    val gatewayName: String = "",
    val errorMessage: String? = null
)
