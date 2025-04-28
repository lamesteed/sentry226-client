package com.francobotique.sentry226.repository

import android.annotation.SuppressLint
import android.util.Log
import com.francobotique.sentry226.repository.data.GpsData
import com.francobotique.sentry226.repository.data.MetadataData
import kotlinx.coroutines.delay

class MockServiceRepo : ServiceRepo {

    private val _tag = "MockServiceRepo"
    private var _metadata: MetadataData = MetadataData(
        datasetName = "MyTestDataset",
        locationID = "TestLocationID",
        locationName = "TestLocationName",
        samplingStep = 1.0,
        samplingIntervalMsec = 5000,
        samplingMaxDurationSec = 300
    )
    private var _gpsData: GpsData = GpsData(
        latitude = 48.8566,
        longitude = 2.3522,
        accuracy = 5.0f,
        accuracyUnit = "m",
        systemName = "WGS84"
    )

    private var _mockConfig = """
        KEY_1=VALUE_1
        KEY_2=VALUE_2
        KEY_3=VALUE_3
        KEY_4=VALUE_4
    """.trimIndent()

    private var _testData = """
               
        Test Run #1
        ---------
        temp=22.5
        pressure=1
        conductivity=11        
    """.trimIndent()

    override suspend fun connect() {
        Log.i(_tag, "connect() called")
    }

    override suspend fun disconnect() {
        Log.i(_tag, "disconnect() called")
    }

    override suspend fun reboot() {
        Log.i(_tag, "reboot() called")
    }

    override suspend fun getConfiguration(): String {
        // Simulate a delay for fetching configuration
        // return multiline string of key=value pairs
        Log.i(_tag, "getConfiguration() called")
        delay(500) // Simulate network delay
        return _mockConfig
    }

    override suspend fun applyConfiguration(config: String) {
        Log.i(_tag, "applyConfiguration() called")
        delay(500)
        _mockConfig=config
    }

    override suspend fun getTestData(): String {
        Log.i(_tag, "getTestData() called")
        delay(2000) // Simulate network delay
        return _testData
    }

    override suspend fun syncTime() {
        Log.i(_tag, "syncTime() called")
        delay(1000) // Simulate network delay
    }

    override suspend fun getResultFiles(): List<String> {
        Log.i(_tag, "getResultFiles() called")
        delay(1000) // Simulate network delay
        return listOf(
            "2025-04-17T2100.csv",
            "2025-04-17T2200.csv",
            "2025-04-17T2300.csv",
            "2025-04-15T1100.csv",
            "2025-04-15T1200.csv",
            "2025-04-15T1300.csv",
            "2025-04-12T1100.csv",
            "2025-04-12T1200.csv",
            "2025-04-12T1300.csv",
            "2025-03-01T1100.csv",
            "2025-03-01T1200.csv",
            "2025-03-01T1300.csv"
            )
    }

    override suspend fun getResultFile(filename: String): ByteArray {
        Log.i(_tag, "getResultFile() called for $filename")
        delay(1000) // Simulate network delay
        // Simulate file content
        val fileContent = """
            File: $filename
            Content: This is a mock result file.
            Data: 123, 456, 789
        """.trimIndent()
        return fileContent.toByteArray()
    }

    override suspend fun getMetadata(): MetadataData {
        Log.i(_tag, "getMetadata() called")
        delay(300) // Simulate network delay
        return _metadata
    }

    override suspend fun applyMetadata(metadata: MetadataData, gpsData: GpsData) {
        Log.i(_tag, "applyMetadata() called with metadata: $metadata and gpsData: $gpsData")
        delay(1000) // Simulate network delay
        _metadata = metadata
        _gpsData = gpsData
    }

    @SuppressLint("SimpleDateFormat")
    override suspend fun startSampling(): String {
        //Generate results filename in YYYY-MM-DDTHHMM.csv format
        val date = System.currentTimeMillis()
        val filename = java.text.SimpleDateFormat("yyyy-MM-dd'T'HHmm'.csv'").format(date)
        Log.i(_tag, "startSampling() called, filename: $filename")
        delay(1000) // Simulate network delay
        return filename
    }
}