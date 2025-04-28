package com.francobotique.sentry226.repository.data

data class MetadataData(
    val datasetName: String,
    val locationID: String,
    val locationName: String,
    val samplingStep: Double,
    val samplingIntervalMsec : Int,
    val samplingMaxDurationSec : Int,
)
