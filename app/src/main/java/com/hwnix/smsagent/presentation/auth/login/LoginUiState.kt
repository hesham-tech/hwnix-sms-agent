package com.hwnix.smsagent.presentation.auth.login

data class LoginUiState(
    val serverUrl: String = "",
    val loginInput: String = "",
    val passwordInput: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
