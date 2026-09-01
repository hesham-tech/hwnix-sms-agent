package com.hwnix.cash.presentation.status

import com.hwnix.cash.domain.model.SimCard

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
    
    val deviceLines: Map<Int, com.hwnix.cash.domain.model.LineData> = emptyMap(),

    // إعداد الخطوط
    val showSimDialog: Boolean = false,
    val detectedSims: List<SimCard> = emptyList(),
    val simPhoneInputs: Map<Int, String> = emptyMap(),
    val simCarrierInputs: Map<Int, String> = emptyMap(),
    val isSavingSims: Boolean = false,
    val simSaveResult: String? = null,
    val isFirstSetup: Boolean = false,
    val gatewayName: String = "",
    val errorMessage: String? = null,
    
    // تسوية الأرصدة
    val showReconcileDialog: Boolean = false,
    val reconcileSlotIndex: Int = -1,
    val reconcileTargetBalance: String = "",
    val reconcileNote: String = "",
    val isReconciling: Boolean = false,
    val reconcileResult: String? = null,
    val reconcileLineName: String = "",
    val reconcileBookBalance: Double = 0.0,
    val reconcileActualBalance: Double = 0.0,
    val reconcileDiff: Double = 0.0,
    
    // حذف الخطوط
    val showDeleteDialog: Boolean = false,
    val deleteSlotIndex: Int = 0,
    val deleteLineName: String = "",
    val isDeleting: Boolean = false,
    val deleteResult: String? = null
)
