package com.hwnix.smsagent.domain.usecase

import com.hwnix.smsagent.domain.repository.DeviceRepository
import com.hwnix.smsagent.manager.sim.SimManager

class SyncSimLinesUseCase(
    private val deviceRepository: DeviceRepository,
    private val simManager: SimManager
) {
    suspend fun execute(): Result<Unit> {
        val simCards = simManager.getActiveSimCards()
        return deviceRepository.syncLines(simCards)
    }
}
