package com.francobotique.sentry226.repository

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.util.Log
import kotlinx.coroutines.delay
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import com.francobotique.sentry226.repository.DiscoveryRepo.ServiceKind
import java.util.UUID

// String literal to specify service ID
const val SERVICE_ID_PROBE : String = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
const val SERVICE_ID_BUOY : String = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"

class BleDiscoveryRepo(private val scanner: BluetoothLeScanner)
    : ScanCallback()
    , DiscoveryRepo  {

    private var foundAddress : String? = null
    private var mode : DiscoveryRepo.ServiceKind = DiscoveryRepo.ServiceKind.PROBE
    private final val _tag = "BleDiscoveryRepo"

    // ScanCallback implementation starts here
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val device = result.device
        if(device.name != null) {
            Log.i( _tag, "Found device: ${device.name} ($result)")
            foundAddress = device.address
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onBatchScanResults(results: List<ScanResult>) {
        for(result in results) {
            val device = result.device
            if(device.name != null) {
                Log.i(_tag, "Found device: ${device.name} ($result)")
                foundAddress = device.address
            }
        }
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(_tag, "Scan failed with error code: $errorCode")
    }
    // ScanCallback implementation ends here

    override fun setDiscoveryTarget(kind: ServiceKind)
    {
        mode = kind
        Log.d(_tag, "Discovery target set to $mode")
    }

    override fun getDiscoveryTarget(): ServiceKind {
        return mode
    }

    // DeviceDiscovery Repository implementation starts here
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override suspend fun discoverService(): String? {
        var serviceId = SERVICE_ID_PROBE
        if (mode == ServiceKind.BUOY) {
            serviceId = SERVICE_ID_BUOY
        }
        Log.d(_tag, "Performing Discovery of $serviceId")

        foundAddress = null

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(serviceId)))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan( listOf(scanFilter), scanSettings,  this)
        Log.d(_tag, "Scan started")

        delay(3000) // Scan for 3 seconds

        scanner.stopScan(this)
        Log.d(_tag, "Scan stopped")

        // device address or null if  device not found
        return foundAddress
    }
}