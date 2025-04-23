package com.francobotique.sentry226.repository

import com.francobotique.sentry226.repository.data.GpsData

interface LocalRepo {
    fun getResultFiles(): List<String>
    fun storeResultFile(filename: String, data: ByteArray)
    suspend fun getGpsData(): GpsData
}