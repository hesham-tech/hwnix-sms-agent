package com.hwnix.cash.utils

object LineNameHelper {
    fun resolveLineName(serverCarrier: String?, hardwareCarrier: String?, slotIndex: Int): String {
        val server = serverCarrier ?: ""
        val hardware = hardwareCarrier ?: ""
        return if (server.isNotBlank() && !server.equals("Unknown", ignoreCase = true)) {
            server
        } else if (hardware.isNotBlank() && !hardware.equals("Unknown", ignoreCase = true)) {
            hardware
        } else {
            "شريحة ${slotIndex + 1}"
        }
    }
}