package com.francobotique.sentry226.repository

import kotlinx.coroutines.delay

class MockDiscoveryRepo : DiscoveryRepo {
    private var discoveryTarget: DiscoveryRepo.ServiceKind = DiscoveryRepo.ServiceKind.PROBE

    override fun setDiscoveryTarget(kind: DiscoveryRepo.ServiceKind) {
        discoveryTarget = kind
    }

    override fun getDiscoveryTarget(): DiscoveryRepo.ServiceKind {
        return discoveryTarget
    }

    override suspend fun discoverService(): String? {
        // Simulate BLE service discovery with a delay
        delay(2000) // 2 seconds
        return "AA:BB:CC:DD:EE:FF" // Simulated device address
    }
}