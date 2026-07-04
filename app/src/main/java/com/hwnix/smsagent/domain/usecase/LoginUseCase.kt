package com.hwnix.smsagent.domain.usecase

import com.hwnix.smsagent.domain.repository.AuthRepository
import com.hwnix.smsagent.domain.repository.DeviceRepository

// UseCase تسجيل الدخول وربط الجهاز تلقائياً
class LoginUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {
    suspend fun execute(login: String, password: String): Result<Unit> {
        val loginResult = authRepository.login(login, password)
        if (loginResult.isFailure) {
            return Result.failure(loginResult.exceptionOrNull() ?: Exception("Unknown login error"))
        }

        // تسجيل الجهاز بعد الدخول الناجح
        val deviceResult = deviceRepository.registerDevice()
        if (deviceResult.isFailure) {
            return Result.failure(deviceResult.exceptionOrNull() ?: Exception("Unknown device registration error"))
        }

        return Result.success(Unit)
    }
}
