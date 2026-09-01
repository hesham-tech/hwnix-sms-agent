package com.hwnix.cash.presentation.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.hwnix.cash.data.remote.ApiClient
import com.hwnix.cash.manager.sim.SimManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var validationJob: Job? = null
    private val simManager = SimManager(context)

    init {
        discoverData()
    }

    private fun discoverData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true) }
            val sims = simManager.getActiveSimCards()
            val senders = getRecentSenders()
            val isDual = sims.size >= 2

            val sessionManager = com.hwnix.cash.data.local.SessionManager(context)
            val deviceId = sessionManager.getDeviceId()

            // 1. القيم الافتراضية الأولية من شرائح الجهاز المحلية (Local SIM Manager)
            var line1Carrier = sims.getOrNull(0)?.carrier?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) } ?: ""
            var line1Name = line1Carrier.ifBlank { "الخط الأول" }
            var line1Phone = sims.getOrNull(0)?.phoneNumber?.let { sessionManager.cleanPhoneNumber(it) } ?: ""

            var line2Carrier = if (isDual) sims.getOrNull(1)?.carrier?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) } ?: "" else ""
            var line2Name = if (isDual) line2Carrier.ifBlank { "الخط الثاني" } else ""
            var line2Phone = if (isDual) sims.getOrNull(1)?.phoneNumber?.let { sessionManager.cleanPhoneNumber(it) } ?: "" else ""

            // 2. جلب بيانات الخطوط المحفوظة من السيرفر إن وجدت (Priority 1)
            try {
                if (deviceId > 0) {
                    val response = ApiClient.getService().getDeviceLines(deviceId)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.get("status")?.asBoolean == true && body.has("data")) {
                            val linesArray = body.getAsJsonArray("data")
                            if (linesArray != null && linesArray.size() > 0) {
                                linesArray.forEach { element ->
                                    val obj = element.asJsonObject
                                    val slotIndex = obj.get("slot_index")?.asInt ?: 0
                                    val sCarrier = obj.get("carrier")?.asString?.trim() ?: ""
                                    val sLineName = obj.get("line_name")?.asString?.trim() ?: ""
                                    val sPhone = obj.get("phone_number")?.asString?.trim() ?: ""
                                    
                                    val serverName = when {
                                        sLineName.isNotBlank() && !sLineName.equals("Unknown", ignoreCase = true) -> sLineName
                                        sCarrier.isNotBlank() && !sCarrier.equals("Unknown", ignoreCase = true) -> sCarrier
                                        else -> ""
                                    }

                                    if (slotIndex == 0) {
                                        if (serverName.isNotBlank()) line1Name = serverName
                                        if (sPhone.isNotBlank()) line1Phone = sessionManager.cleanPhoneNumber(sPhone)
                                    } else if (slotIndex == 1) {
                                        if (serverName.isNotBlank()) line2Name = serverName
                                        if (sPhone.isNotBlank()) line2Phone = sessionManager.cleanPhoneNumber(sPhone)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _uiState.update { 
                it.copy(
                    isDiscovering = false,
                    availableSims = sims,
                    recentSenders = senders,
                    isDualSim = isDual,
                    line1Name = line1Name,
                    line1Phone = line1Phone,
                    line2Name = line2Name,
                    line2Phone = line2Phone,
                    selectedSimPhone = line1Phone.ifBlank { line2Phone },
                    selectedSender = senders.firstOrNull() ?: ""
                )
            }
        }
    }

    fun retryDiscoverSims() {
        discoverData()
    }

    @SuppressLint("Range")
    private fun getRecentSenders(): List<String> {
        val senders = mutableSetOf<String>()
        val uri = Uri.parse("content://sms/inbox")
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("address", "date"),
                "date > ?",
                arrayOf(thirtyDaysAgo.toString()),
                "date DESC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndex("address"))
                    if (!address.isNullOrBlank() && address.any { char -> char.isLetter() }) {
                        senders.add(address)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return senders.toList().sorted()
    }

    fun onLine1NameChange(name: String) {
        _uiState.update { it.copy(line1Name = name) }
    }

    fun onLine1PhoneChange(phone: String) {
        _uiState.update { 
            val updated = it.copy(line1Phone = phone)
            if (it.selectedSimPhone.isBlank() || it.selectedSimPhone == it.line1Phone) {
                updated.copy(selectedSimPhone = phone)
            } else updated
        }
    }

    fun onLine2NameChange(name: String) {
        _uiState.update { it.copy(line2Name = name) }
    }

    fun onLine2PhoneChange(phone: String) {
        _uiState.update { it.copy(line2Phone = phone) }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 4) {
            // تزامن القيمة الافتراضية للخط المحدد في الخطوة الثانية إذا كان فارغاً
            if (current == 1 && _uiState.value.selectedSimPhone.isBlank()) {
                val state = _uiState.value
                val firstPhone = state.line1Phone.ifBlank { state.line2Phone }
                _uiState.update { it.copy(selectedSimPhone = firstPhone, currentStep = current + 1) }
            } else {
                _uiState.update { it.copy(currentStep = current + 1) }
            }
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 1) {
            _uiState.update { it.copy(currentStep = current - 1) }
        }
    }

    fun onWalletNameChange(name: String) {
        _uiState.update { it.copy(walletName = name) }
        debounceValidation()
    }

    fun onSimPhoneChange(phone: String) {
        _uiState.update { it.copy(selectedSimPhone = phone) }
        debounceValidation()
    }

    fun onSenderChange(sender: String) {
        _uiState.update { it.copy(selectedSender = sender) }
        debounceValidation()
    }

    private fun debounceValidation() {
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            _uiState.update { it.copy(isValidationLoading = true, validationSuccess = null, validationMessage = "") }
            delay(800) // Debounce delay
            
            val state = _uiState.value
            if (state.walletName.isBlank() || state.selectedSimPhone.isBlank() || state.selectedSender.isBlank()) {
                _uiState.update { 
                    it.copy(
                        isValidationLoading = false,
                        validationSuccess = false,
                        validationMessage = "يرجى ملء جميع الحقول"
                    )
                }
                return@launch
            }

            try {
                val body = JsonObject().apply {
                    addProperty("wallet_name", state.walletName)
                    addProperty("sim_phone", state.selectedSimPhone)
                    addProperty("sender", state.selectedSender)
                    addProperty("device_android_id", android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                    addProperty("device_name", android.os.Build.MODEL)
                }
                val response = ApiClient.getService().validateOnboarding(body)
                if (response.isSuccessful) {
                    val json = response.body()
                    val isValid = json?.get("is_valid")?.asBoolean ?: true
                    val message = json?.get("message")?.asString ?: "جاهز"
                    _uiState.update { 
                        it.copy(
                            isValidationLoading = false,
                            validationSuccess = isValid,
                            validationMessage = message
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isValidationLoading = false,
                            validationSuccess = false,
                            validationMessage = "الاسم أو البيانات غير صالحة"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isValidationLoading = false,
                        validationSuccess = false,
                        validationMessage = "فشل التحقق من الاتصال"
                    )
                }
            }
        }
    }

    fun onLimitChange(dailyWithdraw: String, dailyDeposit: String, monthlyWithdraw: String, monthlyDeposit: String) {
        _uiState.update { 
            it.copy(
                dailyWithdrawLimit = dailyWithdraw,
                dailyDepositLimit = dailyDeposit,
                monthlyWithdrawLimit = monthlyWithdraw,
                monthlyDepositLimit = monthlyDeposit
            )
        }
    }

    fun submitOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = "") }
            try {
                val state = _uiState.value
                val body = JsonObject().apply {
                    addProperty("wallet_name", state.walletName)
                    addProperty("sim_phone", state.selectedSimPhone)
                    addProperty("sender", state.selectedSender)
                    addProperty("device_android_id", android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                    addProperty("device_name", android.os.Build.MODEL)
                    addProperty("daily_withdraw_limit", state.dailyWithdrawLimit.toDoubleOrNull() ?: 0.0)
                    addProperty("daily_deposit_limit", state.dailyDepositLimit.toDoubleOrNull() ?: 0.0)
                    addProperty("monthly_withdraw_limit", state.monthlyWithdrawLimit.toDoubleOrNull() ?: 0.0)
                    addProperty("monthly_deposit_limit", state.monthlyDepositLimit.toDoubleOrNull() ?: 0.0)
                }
                val response = ApiClient.getService().completeOnboarding(body)
                if (response.isSuccessful) {
                    val sessionManager = com.hwnix.cash.data.local.SessionManager(context)
                    sessionManager.markSetupComplete()
                    sessionManager.setWalletMissing(false)
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                    // تشغيل المزامنة فور الانتهاء من إعداد المحفظة والخط
                    try {
                        com.hwnix.cash.core.di.ServiceLocator.syncEngine.performFullSync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val errorObj = response.errorBody()?.string()
                    val errorMsg = if (errorObj != null && errorObj.contains("message")) {
                        com.google.gson.JsonParser.parseString(errorObj).asJsonObject.get("message")?.asString ?: "فشل في حفظ المحفظة"
                    } else {
                        "فشل في حفظ المحفظة"
                    }
                    _uiState.update { it.copy(isSubmitting = false, submitError = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, submitError = "حدث خطأ أثناء الاتصال بالخادم") }
            }
        }
    }
}
