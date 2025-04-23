package com.francobotique.sentry226

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.francobotique.sentry226.databinding.ActivityFieldModeBinding
import com.francobotique.sentry226.repository.MockLocalRepo
import com.francobotique.sentry226.repository.MockServiceRepo
import com.francobotique.sentry226.repository.PhoneLocalRepo
import com.francobotique.sentry226.viewmodel.FieldModeModelFactory
import com.francobotique.sentry226.viewmodel.FieldModeViewModel
import com.google.android.gms.location.LocationServices

class FieldModeActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityFieldModeBinding
    private lateinit var viewModel: FieldModeViewModel
    private lateinit var resultViewAdapter: ResultViewAdapter

    private val _tag = "FieldModeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFieldModeBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fieldmode)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve bluetooth device address from intent
        val deviceAddress = intent.getStringExtra(INTENT_STRING_EXTRA_NAME)
        Log.i(_tag, "Starting Activity, Received device address: $deviceAddress")

        // Create bluetooth device object from address
        // val bluetoothManager: BluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        // val bluetoothDevice = bluetoothManager.adapter.getRemoteDevice(deviceAddress)
        // val repository = BleServiceRepo(bluetoothDevice, SERVICE_ID_PROBE )

        val localRepo = PhoneLocalRepo(
            LocationServices.getFusedLocationProviderClient(this),
            this.contentResolver)

        val serviceRepo = MockServiceRepo()
        //val localRepo = MockLocalRepo()
        val factory = FieldModeModelFactory(serviceRepo, localRepo)
        viewModel = ViewModelProvider(this, factory)[FieldModeViewModel::class.java]


        // Initialize the adapter with an empty list
        resultViewAdapter = ResultViewAdapter(emptyList()) { fileItem ->
            viewModel.downloadResult(fileItem.fileName)
        }
        binding.recyclerViewFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFiles.adapter = resultViewAdapter

        // Observe the file items and update the adapter
        viewModel.resultItems.observe(this) { fileItems ->
            resultViewAdapter.updateData(fileItems)
        }

        // Observe the visibility of the metadata layout
        viewModel.metadataVisibility.observe(this) { visibility ->
            binding.layoutMetadata.visibility = visibility
        }
        // Observe the visibility of the progress bar
        viewModel.progressVisibility.observe(this) { visibility ->
            binding.progressBar.visibility = visibility
        }
        // Observe loading state (disables/enables entire layout)
        viewModel.loading.observe(this) { loading ->
            binding.fieldmode.isEnabled = !loading
        }
        // Observer error messages and show toast
        viewModel.toastMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
        // Observer that closes activity if requested by viewmodel logic
        viewModel.closeActivity.observe(this) { close ->
            if (close) {
                Log.i(_tag, "Closing activity as requested by ViewModel")
                finish()
            }
        }

        binding.buttonToggleMetadata.setOnClickListener(this)
        binding.buttonDisconnect.setOnClickListener(this)
        binding.buttonReboot.setOnClickListener(this)
        binding.buttonGetResults.setOnClickListener(this)
        binding.buttonStartSampling.setOnClickListener(this)

        viewModel.connect()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.buttonToggleMetadata -> {
                Log.i(_tag, "Calibration button clicked")
                viewModel.toggleMetadata()
            }
            R.id.buttonDisconnect -> {
                Log.i(_tag, "Disconnect button clicked")
                // invoke disconnect from device and finish activity
                viewModel.disconnect()
            }
            R.id.buttonReboot -> {
                Log.i(_tag, "Reboot button clicked")
                // invoke reboot from device
                viewModel.reboot()
            }
            R.id.buttonGetResults -> {
                Log.i(_tag, "Get Results button clicked")
                // invoke get results from device
                viewModel.getResults()
            }
            R.id.buttonStartSampling -> {
                Log.i(_tag, "Start Sampling button clicked")
                // invoke start sampling from device
                viewModel.startSampling()
            }
            else -> {
                Log.w(_tag, "Unknown button clicked")
            }
        }
    }
}