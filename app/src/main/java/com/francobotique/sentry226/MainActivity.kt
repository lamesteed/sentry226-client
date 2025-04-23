package com.francobotique.sentry226

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.francobotique.sentry226.databinding.ActivityMainBinding
import com.francobotique.sentry226.repository.DiscoveryRepo
import com.francobotique.sentry226.repository.BleDiscoveryRepo
import com.francobotique.sentry226.repository.MockDiscoveryRepo
import com.francobotique.sentry226.viewmodel.MainViewModel
import com.francobotique.sentry226.viewmodel.MainViewModelFactory

const val PERMISSION_REQUEST_CODE = 1
const val INTENT_STRING_EXTRA_NAME = "deviceAddress"

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check and request permissions
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            Log.i("Permissions", "All required permissions are already granted.")
        }

        // Create instance of BleDiscoveryRepository and reference it as a DiscoveryRepo interface
        val bluetoothManager: BluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //check if Bluetooth enabled, show bubble if not and exit
        if (!bluetoothManager.adapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled. Please enable it to proceed.", Toast.LENGTH_LONG).show()
            return
        }

        //val repository = BleDiscoveryRepo(bluetoothManager.adapter.bluetoothLeScanner) as DiscoveryRepo
        val repository = MockDiscoveryRepo()
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        binding.buttonTest.setOnClickListener(this)
        binding.buttonField.setOnClickListener(this)
        binding.buttonSearch.setOnClickListener(this)
        binding.buttonConnect.setOnClickListener(this)

        // Observe the visibility of the Mode Selector Layout
        viewModel.selectorVisibility.observe(this) { visibility ->
            binding.layoutModeSelector.visibility = visibility
        }
        // Observe the visibility of the Discovery Layout
        viewModel.discoveryVisibility.observe(this) { visibility ->
            binding.layoutDiscovery.visibility = visibility
        }
        // Observe the visibility of the Connect button
        viewModel.connectVisibility.observe(this) { visibility ->
            binding.buttonConnect.visibility = visibility
        }
        // Observe the loading state
        viewModel.loading.observe(this) { loading ->
            binding.layoutDiscovery.isEnabled = !loading
            if (loading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Register OnBackPressedCallback for back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // if discovery layout is visible then hide it
                if (viewModel.discoveryVisibility.value == View.VISIBLE) {
                    viewModel.setShowDiscovery(false)
                    return
                }

                // Otherwise call the super method to handle the default back button behavior
                if (isEnabled) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hasPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION, // Required for BLE scanning
            Manifest.permission.ACCESS_COARSE_LOCATION // Required alongside fine location
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Add Android 12+ specific permissions
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Add Android 12+ specific permissions
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isEmpty()) {
                Log.i("Permissions", "All required permissions granted.")
                // Start BLE scanning or other operations
            } else {
                Log.e("Permissions", "The following permissions were denied: $deniedPermissions")
                // Handle the case where permissions are denied
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonTest -> {
                // Handle buttonTest click
                viewModel.applyTestMode()
            }
            R.id.buttonField -> {
                // Handle buttonField click
                viewModel.applyFieldMode()
            }
            R.id.buttonSearch -> {
                // Handle buttonSearch click
                viewModel.discoverService()
            }
            R.id.buttonConnect -> {
                // Handle buttonConnect click
                val startTestModeActivity = viewModel.isTestMode()

                val intent : Intent?
                if (startTestModeActivity) {
                    Log.i("MainActivity", "Starting TestModeActivity")
                    intent = Intent(this, TestModeActivity::class.java)
                } else {
                    Log.i("MainActivity", "Starting FieldModeActivity")
                    intent = Intent(this, FieldModeActivity::class.java)
                }
                intent.putExtra(INTENT_STRING_EXTRA_NAME, viewModel.getDeviceAddress())
                startActivity( intent )
            }
            else -> {
                // Handle other clicks if necessary
            }
        }
    }
}