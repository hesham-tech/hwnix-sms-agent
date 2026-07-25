package com.hwnix.cash.domain.usecase

import com.hwnix.cash.domain.repository.DeviceRepository
import com.hwnix.cash.manager.sim.SimManager

class SyncSimLinesUseCase(
    private val deviceRepository: DeviceRepository,
    private val simManager: SimManager
) {
    suspend fun execute(): Result<Unit> {
        val simCards = simManager.getActiveSimCards()
        return deviceRepository.syncLines(simCards)
    }
}
