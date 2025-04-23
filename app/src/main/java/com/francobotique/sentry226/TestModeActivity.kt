package com.francobotique.sentry226

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.francobotique.sentry226.databinding.ActivityTestModeBinding
import com.francobotique.sentry226.repository.BleServiceRepo
import com.francobotique.sentry226.repository.MockServiceRepo
import com.francobotique.sentry226.repository.SERVICE_ID_PROBE
import com.francobotique.sentry226.viewmodel.TestModeModelFactory
import com.francobotique.sentry226.viewmodel.TestModeViewModel

class TestModeActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityTestModeBinding
    private lateinit var viewModel: TestModeViewModel

    private val _tag = "TestModeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestModeBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.testmode)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve bluetooth device address from intent
        val deviceAddress = intent.getStringExtra(INTENT_STRING_EXTRA_NAME)
        Log.i(_tag, "Starting Activity, Received device address: $deviceAddress")


        // Create bluetooth device object from address
        val bluetoothManager: BluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothDevice = bluetoothManager.adapter.getRemoteDevice(deviceAddress)
        val repository = BleServiceRepo(bluetoothDevice, SERVICE_ID_PROBE )

        //val repository = MockServiceRepo()

        val factory = TestModeModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TestModeViewModel::class.java]

        // Observe the visibility of the calibration layout
        viewModel.calibrationVisibility.observe(this) { visibility ->
            binding.layoutCalibration.visibility = visibility
        }
        // Observe the visibility of the progress bar
        viewModel.progressVisibility.observe(this) { visibility ->
            binding.progressBar.visibility = visibility
        }
        // Observe loading state (disables/enables entire layout)
        viewModel.loading.observe(this) { loading ->
            binding.testmode.isEnabled = !loading
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
        //Observe calibration data and set it to the EditText
        viewModel.calibrationData.observe(this) { data ->
            binding.editTextConfig.setText(data)
        }
        //Observe probe data and set it to the TextView
        viewModel.probeData.observe(this) { data ->
            binding.textViewProbeData.text = data
        }

        binding.textViewProbeData.movementMethod = ScrollingMovementMethod()
        binding.buttonToggleCalibration.setOnClickListener(this)
        binding.buttonDisconnect.setOnClickListener(this)
        binding.buttonReboot.setOnClickListener(this)
        binding.buttonGetConfig.setOnClickListener(this)
        binding.buttonApplyConfig.setOnClickListener(this)
        binding.buttonReadData.setOnClickListener(this)

        viewModel.connect()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.buttonToggleCalibration -> {
                Log.i(_tag, "Calibration button clicked")
                viewModel.toggleCalibration()
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
            R.id.buttonGetConfig -> {
                Log.i(_tag, "Get Config button clicked")
                // invoke get config from device
                viewModel.getCalibration()
            }
            R.id.buttonApplyConfig -> {
                Log.i(_tag, "Apply Config button clicked")
                // invoke apply config from device
                val config = binding.editTextConfig.text.toString()
                viewModel.applyCalibration(config)
            }
            R.id.buttonReadData -> {
                Log.i(_tag, "Read Data button clicked")
                // invoke get test data from device
                viewModel.readData()
            }
            else -> {
                Log.w(_tag, "Unknown button clicked")
            }
        }
    }
}