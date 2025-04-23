package com.francobotique.sentry226.repository.data

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val accuracyUnit: String,
    val systemName: String
)
