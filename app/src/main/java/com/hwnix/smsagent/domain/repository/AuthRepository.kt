package com.hwnix.smsagent.domain.repository

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<Unit>
    suspend fun register(fullName: String, nickname: String, phone: String, email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
}

