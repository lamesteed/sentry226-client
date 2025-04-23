package com.francobotique.sentry226.repository

import com.francobotique.sentry226.repository.data.GpsData
import kotlinx.coroutines.delay

class MockLocalRepo : LocalRepo {
    private var resultFiles: MutableList<String> = mutableListOf()

    override fun getResultFiles(): List<String> {
        //delay(100)
        return resultFiles
    }

    override fun storeResultFile(filename: String, data: ByteArray)
    {
        // delay(100)
        // add file name to the resultFiles list
        resultFiles.add(filename)
    }

    override suspend fun getGpsData(): GpsData {
        delay(100)
        return GpsData(
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 5.0f,
            accuracyUnit = "m",
            systemName = "WGS84"
        )
    }
}