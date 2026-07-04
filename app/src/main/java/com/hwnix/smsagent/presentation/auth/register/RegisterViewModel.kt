package com.hwnix.smsagent.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiClient
import com.hwnix.smsagent.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val sessionManager: SessionManager,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        val initialUrl = sessionManager.getBaseUrl().let { url ->
            if (url.contains("10.0.2.2") || url.isEmpty() || url.contains("localhost")) {
                "https://api-teste.hwnix.com"
            } else {
                var clean = url.trim()
                if (clean.endsWith("/")) clean = clean.substring(0, clean.length - 1)
                if (clean.endsWith("/api")) clean = clean.substring(0, clean.length - 4)
                clean.trim()
            }
        }
        _uiState.update { it.copy(serverUrl = initialUrl) }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, errorMessage = null) }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onNicknameChange(name: String) {
        _uiState.update { it.copy(nickname = name, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun register() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.fullName.isBlank() || state.nickname.isBlank() ||
            state.phone.isBlank() || state.password.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "يرجى ملء جميع الحقول المطلوبة.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val cleanedUrl = state.serverUrl.trim().removeSuffix("/")
            sessionManager.saveBaseUrl(cleanedUrl)
            ApiClient.resetClient()

            val result = registerUseCase.execute(
                fullName = state.fullName.trim(),
                nickname = state.nickname.trim(),
                phone = state.phone.trim(),
                email = state.email.trim(),
                password = state.password.trim()
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "حدث خطأ غير متوقع."
                    )
                }
            }
        }
    }
}
