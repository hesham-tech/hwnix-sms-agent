package com.hwnix.cash.presentation.auth.register

data class RegisterUiState(
    val serverUrl: String = "https://bill-api.hwnix.com/api/",
    val companyName: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
