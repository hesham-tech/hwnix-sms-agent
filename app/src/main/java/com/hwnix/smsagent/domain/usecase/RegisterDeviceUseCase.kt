package com.hwnix.smsagent.domain.usecase

import com.hwnix.smsagent.domain.repository.DeviceRepository

class RegisterDeviceUseCase(private val deviceRepository: DeviceRepository) {
    suspend fun execute(): Result<Long> {
        return deviceRepository.registerDevice()
    }
}
