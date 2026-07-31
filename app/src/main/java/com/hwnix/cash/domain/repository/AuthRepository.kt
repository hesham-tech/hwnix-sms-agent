package com.hwnix.cash.domain.repository

/* تعليق عربي مختصر: واجهة مستودع المصادقة وتجهيز الشركات وإدارة الجلسات */
interface AuthRepository {
    suspend fun login(login: String, password: String): Result<Unit>
    suspend fun register(companyName: String, fullName: String, nickname: String, phone: String, email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
}
