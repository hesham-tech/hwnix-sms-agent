package com.hwnix.smsagent.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiClient
import com.hwnix.smsagent.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val sessionManager: SessionManager,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    fun onLoginInputChange(input: String) {
        _uiState.update { it.copy(loginInput = input, errorMessage = null) }
    }

    fun onPasswordInputChange(input: String) {
        _uiState.update { it.copy(passwordInput = input, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.loginInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى ملء جميع الحقول المطلوبة.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val cleanedUrl = state.serverUrl.trim().removeSuffix("/")
            sessionManager.saveBaseUrl(cleanedUrl)
            ApiClient.resetClient()

            val result = loginUseCase.execute(state.loginInput.trim(), state.passwordInput.trim())
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
