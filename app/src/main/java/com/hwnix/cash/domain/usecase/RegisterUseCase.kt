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
        val registerResult = authRepository.register(companyName, fullName, nickname, phone, email, password)
        if (registerResult.isFailure) return registerResult

        // تسجيل الجهاز وربط البوابة تلقائياً بعد إنشاء الحساب الناجح
        val deviceResult = deviceRepository.registerDevice()
        if (deviceResult.isFailure) {
            android.util.Log.w("RegisterUseCase", "Device auto registration warning: ${deviceResult.exceptionOrNull()?.message}")
        }

        return Result.success(Unit)
    }
}
