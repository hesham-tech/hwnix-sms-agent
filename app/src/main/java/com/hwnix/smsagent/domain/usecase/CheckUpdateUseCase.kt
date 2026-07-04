package com.hwnix.smsagent.domain.usecase

import com.hwnix.smsagent.domain.model.AppUpdate
import com.hwnix.smsagent.domain.repository.DeviceRepository

class CheckUpdateUseCase(private val deviceRepository: DeviceRepository) {
    suspend fun execute(): Result<AppUpdate?> {
        return deviceRepository.checkAppUpdate()
    }
}
