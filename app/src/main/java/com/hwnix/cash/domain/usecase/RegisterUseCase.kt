package com.hwnix.cash.domain.usecase

import com.hwnix.cash.domain.repository.AuthRepository
import com.hwnix.cash.domain.repository.DeviceRepository

/* تعليق عربي مختصر: حالة استخدام تسجيل شركة سحابية جديدة وتأمين حزمة التجهيز الأولي */
class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(
        companyName: String,
        fullName: String,
        nickname: String,
        phone: String,
        email: String,
        password: String
    ): Result<Unit> {
        return authRepository.register(companyName, fullName, nickname, phone, email, password)
    }
}
