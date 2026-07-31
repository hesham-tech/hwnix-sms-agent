package com.hwnix.cash.domain.usecase

import com.hwnix.cash.domain.model.AppUpdate
import com.hwnix.cash.domain.repository.DeviceRepository

class CheckUpdateUseCase(private val deviceRepository: DeviceRepository) {
    suspend fun execute(): Result<AppUpdate?> {
        return deviceRepository.checkAppUpdate()
    }
}
