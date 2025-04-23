package com.francobotique.sentry226.repository

import com.francobotique.sentry226.repository.data.GpsData
import com.francobotique.sentry226.repository.data.MetadataData

interface ServiceRepo {
    suspend fun connect()
    suspend fun disconnect()
    suspend fun reboot()
    suspend fun getConfiguration(): String
    suspend fun applyConfiguration(config: String)
    suspend fun getTestData(): String
    suspend fun syncTime()
    suspend fun getResultFiles(): List<String>
    suspend fun getResultFile(filename: String): ByteArray
    suspend fun getMetadata(): MetadataData
    suspend fun applyMetadata(metadata: MetadataData, gpsData: GpsData)
    suspend fun startSampling(): String
}