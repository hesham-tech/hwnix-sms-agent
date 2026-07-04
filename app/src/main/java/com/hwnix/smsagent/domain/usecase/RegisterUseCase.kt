package com.hwnix.smsagent.domain.usecase

import com.hwnix.smsagent.domain.repository.AuthRepository
import com.hwnix.smsagent.domain.repository.DeviceRepository

// UseCase إنشاء حساب جديد وربط الجهاز تلقائياً
class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {
    suspend fun execute(
        fullName: String,
        nickname: String,
        phone: String,
        email: String,
        password: String
    ): Result<Unit> {
        val registerResult = authRepository.register(fullName, nickname, phone, email, password)
        if (registerResult.isFailure) {
            return Result.failure(registerResult.exceptionOrNull() ?: Exception("Unknown registration error"))
        }

        // تسجيل الجهاز بعد إنشاء الحساب الناجح
        val deviceResult = deviceRepository.registerDevice()
        if (deviceResult.isFailure) {
            return Result.failure(deviceResult.exceptionOrNull() ?: Exception("Unknown device registration error"))
        }

        return Result.success(Unit)
    }
}
