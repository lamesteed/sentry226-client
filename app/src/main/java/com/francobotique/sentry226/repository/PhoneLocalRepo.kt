package com.francobotique.sentry226.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.location.Location
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.francobotique.sentry226.repository.data.GpsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class PhoneLocalRepo(private val fusedLocationClient: FusedLocationProviderClient,
                     private val contentResolver: ContentResolver) : LocalRepo {

    private val sentryDirectoryName = "Sentry226"

    private val _tag = "PhoneLocalRepo"

    fun getSentryDirectory(): File? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Legacy storage
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val sentryDir = File(downloadsDir, sentryDirectoryName)
            if (!sentryDir.exists()) {
                Log.i(_tag, "Creating directory: $sentryDir")
                sentryDir.mkdirs()
            }
            sentryDir
        } else {
            // Scoped storage (return null, as MediaStore is used for SDK 29+)
            null
        }
    }

    override fun getResultFiles(): List<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Legacy storage
            Log.i(_tag, "Getting result files from legacy storage")
            val sentryDir = getSentryDirectory()
            sentryDir?.listFiles { file -> file.extension == "csv" }?.map { it.name } ?: emptyList()
        } else {
            Log.i(_tag, "Getting result files from scoped storage")
            // Scoped storage
            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("Download/Sentry226/") // Ensure trailing slash
            val cursor = contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            val resultFiles = mutableListOf<String>()
            Log.i(_tag, "Querying MediaStore for result files")
            cursor?.use {
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                while (it.moveToNext()) {
                    resultFiles.add(it.getString(nameIndex))
                }
            }
            resultFiles
        }
    }

    override fun storeResultFile(filename: String, data: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Legacy storage
            val sentryDir = getSentryDirectory()
            val file = File(sentryDir, filename)
            FileOutputStream(file).use { it.write(data) }
        } else {
            // Scoped storage
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$sentryDirectoryName")
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it).use { outputStream ->
                    outputStream?.write(data)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getGpsData(): GpsData {
        return withContext(Dispatchers.IO) {

            val latch = CountDownLatch(1)
            lateinit var gpsData: GpsData
            var error: Exception? = null

            Log.i(_tag, "Subscribing to GPS data retrieval")

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    gpsData = GpsData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        accuracyUnit = "m",
                        systemName = "WGS84"
                    )
                    Log.i(_tag, "GPS data retrieved successfully: $gpsData")
                } else {
                    error = Exception("Failed to retrieve location: Location is null")
                }
                latch.countDown()
            }.addOnFailureListener { exception ->
                error = exception
                latch.countDown()
            }

            // Wait for the callback to complete
            if (!latch.await(5, TimeUnit.SECONDS)) {
                Log.i(_tag, "GPS data retrieval timed out")
                throw Exception("GPS data retrieval timed out")
            }

            // Throw an error if there was an issue
            if (error != null) {
                Log.i(_tag, "Error retrieving GPS data: ${error?.message}")
                throw error!!
            }

            // Return the GPS data
            gpsData
        }
    }
}