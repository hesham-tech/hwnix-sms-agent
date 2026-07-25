package com.hwnix.cash.domain.usecase

import com.hwnix.cash.domain.repository.DeviceRepository

class RegisterDeviceUseCase(private val deviceRepository: DeviceRepository) {
    suspend fun execute(): Result<Long> {
        return deviceRepository.registerDevice()
    }
}
