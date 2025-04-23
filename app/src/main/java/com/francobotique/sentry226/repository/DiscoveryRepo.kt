package com.francobotique.sentry226.repository

import android.bluetooth.BluetoothDevice

interface DiscoveryRepo {

    enum class ServiceKind {
        PROBE,
        BUOY
    }

    fun setDiscoveryTarget(kind: ServiceKind)
    fun getDiscoveryTarget(): ServiceKind
    suspend fun discoverService():String?
}
