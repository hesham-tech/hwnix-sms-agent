package com.hwnix.smsagent.presentation.auth.register

data class RegisterUiState(
    val serverUrl: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
