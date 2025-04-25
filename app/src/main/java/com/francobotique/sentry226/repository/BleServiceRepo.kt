@file:Suppress("DEPRECATION")

package com.francobotique.sentry226.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import android.util.Base64
import com.francobotique.sentry226.repository.data.GpsData
import com.francobotique.sentry226.repository.data.MetadataData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


const val MAX_MTU : Int = 512
const val CHAR_NOTIFY_UUID :  String = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
const val CHAR_TX_CMD_UUID :  String = "e3223119-9445-4e96-a4a1-85358c4046a2"
const val CHAR_TX_ARGS_UUID : String = "8e0a2686-d5bc-11ef-9cd2-0242ac120002"

class BleServiceRepo(private val bluetoothDevice: BluetoothDevice,
                     serviceId: String) : ServiceRepo {
    private enum class Command{
        REBOOT,
        GETFILE,
        STOREFILE,
        TESTMODE,
        SETTIME,
        LISTFILES,
        FIELDMODE,
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var state = BluetoothProfile.STATE_DISCONNECTED
    private var mtu = 23
    private var txCmdCharacteristic: BluetoothGattCharacteristic? = null
    private var txArgsCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val serviceUuid : UUID = UUID.fromString(serviceId)

    private var statusMessage = StringBuilder(MAX_MTU * 3)

    private fun fetchStatusMessage(): String {
        val result = statusMessage.toString()
        statusMessage.setLength(0) // Reset StringBuilder (prepare for next command)
        return result
    }

    // Latches for synchronization
    private var connectionLatch = CountDownLatch(1)
    private var mtuLatch = CountDownLatch(1)
    private var serviceDiscoveryLatch = CountDownLatch(1)
    private var characteristicWriteLatch = CountDownLatch(1)
    private var commandCompleteLatch = CountDownLatch(1)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            state = newState
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(_tag, "Connected to GATT server.")
                connectionLatch.countDown()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(_tag, "Disconnected from GATT server.")
                connectionLatch.countDown() // Release latch in case of failure
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, newMtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // if MTU > 512, set to 512, otherwise set to newMtu
                mtu = if (newMtu > MAX_MTU) {
                    Log.i(_tag, "MTU is greater than 512, setting to 512.")
                    MAX_MTU
                } else {
                    Log.i(_tag, "MTU changed to $newMtu bytes.")
                    newMtu
                }

                mtuLatch.countDown()
            } else {
                Log.e(_tag, "Failed to change MTU.")
                mtuLatch.countDown()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(_tag, "Services discovered.")
                serviceDiscoveryLatch.countDown()
            } else {
                Log.e(_tag, "Failed to discover services.")
                serviceDiscoveryLatch.countDown()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic == notifyCharacteristic ) {
                val valueString = String(value)
                Log.i(_tag, "Received message: $valueString")
                if (valueString == "END_OF_DATA") {
                    Log.i(_tag, "Command completed successfully.")
                    commandCompleteLatch.countDown()
                } else {
                    statusMessage.append(valueString)
                    Log.i(_tag, "Status message appended")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(_tag, "Characteristic write successful.")
                characteristicWriteLatch.countDown()
            } else {
                Log.e(_tag, "Failed to write characteristic")
            }
        }
    }

    private val _tag = "BleServiceRepo"

    @SuppressLint("MissingPermission")
    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                // Step 1: Connect to the device
                connectionLatch = CountDownLatch(1)
                bluetoothGatt = bluetoothDevice.connectGatt(null, false, gattCallback)
                if (!connectionLatch.await(5, TimeUnit.SECONDS)) {
                    throw Exception("Connection timed out")
                }

                // Step 2: Request MTU upgrade
                mtuLatch = CountDownLatch(1)
                bluetoothGatt?.requestMtu(MAX_MTU)
                if (!mtuLatch.await(5, TimeUnit.SECONDS)) {
                    throw Exception("MTU request timed out")
                }

                // Step 3: Discover services
                serviceDiscoveryLatch = CountDownLatch(1)
                bluetoothGatt?.discoverServices()
                if (!serviceDiscoveryLatch.await(5, TimeUnit.SECONDS)) {
                    throw Exception("Service discovery timed out")
                }

                // Step 4: Subscribe to characteristic notifications
                val service = bluetoothGatt?.getService(serviceUuid)
                if (service == null) {
                    throw Exception("Service not found")
                }

                notifyCharacteristic = service.getCharacteristic(UUID.fromString(CHAR_NOTIFY_UUID))
                    ?: throw Exception("Characteristic not found")
                bluetoothGatt?.setCharacteristicNotification(notifyCharacteristic, true)
                Log.i(_tag, "Subscribed to characteristic updates.")
                statusMessage.setLength(0) // Clear the status message

                // Step 5: Preserve tx command and tx args characteristics
                txCmdCharacteristic = service.getCharacteristic(UUID.fromString(CHAR_TX_CMD_UUID))
                    ?: throw Exception("TX Command characteristic not found")
                txArgsCharacteristic = service.getCharacteristic(UUID.fromString(CHAR_TX_ARGS_UUID))
                    ?: throw Exception("TX Args characteristic not found")

            } catch (e: Exception) {
                Log.e(_tag, "Connection failed: ${e.message}")
                throw e
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    // Step 1: Initiate disconnection
                    connectionLatch = CountDownLatch(1)
                    bluetoothGatt?.disconnect()

                    // Step 2: Wait for STATE_DISCONNECTED
                    if (!connectionLatch.await(5, TimeUnit.SECONDS)) {
                        throw Exception("Disconnection timed out")
                    }

                    // Step 3: Call close()
                    bluetoothGatt?.close()
                    bluetoothGatt = null

                    // Step 4: Optional delay to ensure cleanup
                    delay(100) // 100ms delay to allow system cleanup
                    Log.i(_tag, "Disconnected and resources released.")
                } else {
                    Log.w(_tag, "Already disconnected or not connected.")
                }
            } catch (e: Exception) {
                Log.e(_tag, "Error during disconnection: ${e.message}")
                throw e
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun executeCommand(command: Command, args : String?) : String {
        return withContext(Dispatchers.IO) {

            if(state != BluetoothProfile.STATE_CONNECTED) {
                throw Exception("Device not connected")
            }

            characteristicWriteLatch = CountDownLatch(1)
            commandCompleteLatch = CountDownLatch(1)

            // 1. Write arguments in chunks to tx args characteristic (if arguments are provided)
            if (args != null) {
                Log.i(_tag, "Executing command: $command, args: $args")
                val chunks = args.chunked(mtu - 3)
                for (chunk in chunks) {
                    Log.i(_tag, "Chunk: $chunk")
                    txArgsCharacteristic?.value = chunk.toByteArray()
                    txArgsCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    bluetoothGatt?.writeCharacteristic(txArgsCharacteristic)

                    // Wait for callback to confirm command characteristic was written
                    if (!characteristicWriteLatch.await(5, TimeUnit.SECONDS)) {
                        throw Exception("Command, set args timed out")
                    }
                    characteristicWriteLatch = CountDownLatch(1)
                }
            } else {
                Log.i(_tag, "Executing command: $command")
            }

            // 2. Write command to tx command characteristic
            txCmdCharacteristic?.value = command.name.toByteArray()
            txCmdCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(txCmdCharacteristic)
            // Wait for callback to confirm command characteristic was written
            if (!characteristicWriteLatch.await(5, TimeUnit.SECONDS)) {
                throw Exception("Command timed out")
            }

            // 3. Wait for notification with text END_OF_DATA to confirm command is accepted
            if (!commandCompleteLatch.await(10, TimeUnit.SECONDS)) {
                throw Exception("Command timed out")
            }

            // 4. Read out status message
            fetchStatusMessage() // last expression within 'withContext' the block would be used as return value
        }
    }

    override suspend fun reboot() {
        // execute REBOOT command (no arguments required) and print status message
        val status = executeCommand(Command.REBOOT, null)
        Log.i(_tag, "Reboot command executed, status: $status")
    }

    override suspend fun getConfiguration(): String {
        // execute GETFILE command and return result (will contain content of sampler.cfg file)
        // arguments:
        //  filename=sampler.cfg (specify name of the file to be read from device)
        return executeCommand(Command.GETFILE, "filename=sampler.cfg")
    }

    override suspend fun applyConfiguration(config: String) {
        // execute STOREFILE command
        // prepare arguments (comma separated key-value pairs) and execute STOREFILE command
        // arguments:
        //  filename=sampler.cfg (specify name of the file to be written to device)
        //  data=<base64 encoded data> (specify content of the file to be written to device)

        val sb = StringBuilder()
        sb.append("filename=sampler.cfg,data=")
        sb.append(Base64.encodeToString(config.toByteArray(), Base64.NO_WRAP))
        val status = executeCommand(Command.STOREFILE, sb.toString())
        Log.i(_tag, "Apply config command executed, status: $status")
    }

    override suspend fun getTestData(): String {
        // execute TESTMODE command (no arguments required)
        // and return result (will contain data obtained during test run)
        return executeCommand(Command.TESTMODE, null)
    }

    override suspend fun syncTime() {
        // execute SETTIME command and print status message
        // arguments:
        //  ts=<current time in milliseconds> (specify current time in milliseconds)
        val args = "ts=${System.currentTimeMillis()}"
        val status = executeCommand(Command.SETTIME, args)
        Log.i(_tag, "Sync time command executed, status: $status")
    }

    override suspend fun getResultFiles(): List<String> {
        // execute LISTFILES command (no arguments required) result is comma separated list of files
        val files = executeCommand(Command.LISTFILES, null)

        // split the result, keep only .csv files and return as list
        return files.split(",").filter { it.endsWith(".csv") }
    }

    override suspend fun getResultFile(filename: String): ByteArray {
        // execute GETFILE command
        // arguments:
        //  filename=<filename> (specify name of the file to be read from device)
        return executeCommand(Command.GETFILE, "filename=$filename").toByteArray()
    }

    override suspend fun getMetadata(): MetadataData {
        // execute GETFILE command and return result (will contain content of metadata.cfg file)
        // arguments:
        //  filename=metadata.cfg (specify name of the file to be read from device)
        val metadata = executeCommand(Command.GETFILE, "filename=metadata.cfg")
        // parse the result and return as MetadataData object, each line is a key-value pair
        val metadataMap = mutableMapOf<String, String>()
        metadata.split("\n").forEach { line ->
            val parts = line.split("=")
            if (parts.size == 2) {
                metadataMap[parts[0].trim()] = parts[1].trim()
            }
        }
        // Create MetadataData object from the map
        val metadataData = MetadataData(
            metadataMap["DATASET_NAME"]?:"",
            metadataMap["LOCATION_ID"]?:"",
            metadataMap["LOCATION_NAME"]?:"",
            metadataMap["SAMPLING_STEP"]?.toDoubleOrNull() ?: 0.0,
            metadataMap["SAMPLING_INTERVAL_MSEC"]?.toInt() ?: 0,
        )
        Log.i(_tag, "Metadata retrieved: $metadataData")
        return metadataData
    }

    override suspend fun applyMetadata(metadata: MetadataData, gpsData: GpsData) {
        // execute STOREFILE command
        // prepare arguments (comma separated key-value pairs) and execute STOREFILE command
        // arguments:
        //  filename=metadata.cfg (specify name of the file to be written to device)
        //  data=<base64 encoded data> (specify content of the file to be written to device)

        // Prepare metadata.cfg file content
        val metadatasb = StringBuilder()
        metadatasb.append("DATASET_NAME=${metadata.datasetName}\n")
        metadatasb.append("LOCATION_ID=${metadata.locationID}\n")
        metadatasb.append("LOCATION_NAME=${metadata.locationName}\n")
        metadatasb.append("SAMPLING_STEP=${metadata.samplingStep}\n")
        metadatasb.append("SAMPLING_INTERVAL_MSEC=${metadata.samplingIntervalMsec}\n")
        metadatasb.append("GPS_LATITUDE=${gpsData.latitude}\n")
        metadatasb.append("GPS_LONGITUDE=${gpsData.longitude}\n")
        metadatasb.append("GPS_ACCURACY=${gpsData.accuracy}\n")
        metadatasb.append("GPS_ACCURACY_UNIT=${gpsData.accuracyUnit}\n")
        metadatasb.append("GPS_SYSTEM_NAME=${gpsData.systemName}\n")

        val sb = StringBuilder()
        sb.append("filename=metadata.cfg,data=")
        sb.append(Base64.encodeToString(metadatasb.toString().toByteArray(), Base64.NO_WRAP))
        val status = executeCommand(Command.STOREFILE, sb.toString())
        Log.i(_tag, "Apply metadata command executed, status: $status")
    }

    @SuppressLint("SimpleDateFormat")
    override suspend fun startSampling(): String {
        // execute FIELDMODE command(will trigger start of sampling, Bluetooth will be disabled)
        // arguments:
        //  filename=<filename> (specify name of the file to be read from device)
        val date = System.currentTimeMillis()
        val filename = java.text.SimpleDateFormat("yyyy-MM-dd'T'HHmm'.csv'").format(date)
        val status = executeCommand(Command.FIELDMODE, "filename=$filename")
        Log.i(_tag, "startSampling() called, filename: $filename, received status: $status")
        return filename
    }
}