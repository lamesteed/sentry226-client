package com.francobotique.sentry226.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francobotique.sentry226.repository.DiscoveryRepo
import kotlinx.coroutines.launch

class MainViewModel(private val repository: DiscoveryRepo) : ViewModel() {

    // LiveData to observe the visibility of the Mode Selector Layout
    private val _selectorVisibility = MutableLiveData<Int>()
    val selectorVisibility: LiveData<Int> get() = _selectorVisibility

    // LiveData to observe the visibility of the Discovery Layout
    private val _discoveryVisibility = MutableLiveData<Int>()
    val discoveryVisibility: LiveData<Int> get() = _discoveryVisibility

    // LiveData to observe the visibility of the Connect button
    // This uses a MediatorLiveData to observe the device address
    // and automatically update the visibility accordingly
    private val _connectVisibility = MediatorLiveData<Int>()
    val connectVisibility: LiveData<Int> get() = _connectVisibility

    // LiveData to observe the visibility of the Connect button
    private val _deviceAddress = MutableLiveData<String?>()

    // LiveData to observe the loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading


    private val _tag = "MainViewModel"

    init {

        // initialize the connect button visibility based on the device address value
        _connectVisibility.addSource(_deviceAddress) { address ->
            _connectVisibility.value = if (address.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Initialize the visibility states
        _selectorVisibility.value = View.VISIBLE
        _discoveryVisibility.value = View.GONE
        _deviceAddress.value = null
        _loading.value = false
    }

    fun applyTestMode() {
        repository.setDiscoveryTarget( DiscoveryRepo.ServiceKind.PROBE )
        setShowDiscovery(true)
    }

    fun applyFieldMode() {
        repository.setDiscoveryTarget( DiscoveryRepo.ServiceKind.BUOY )
        setShowDiscovery(true)
    }

    fun setShowDiscovery(show: Boolean) {
        // Set the visibility of both layouts based on the show parameter
        _deviceAddress.value = null
        if (show) {
            _selectorVisibility.value = View.GONE
            _discoveryVisibility.value = View.VISIBLE
        } else {
            _selectorVisibility.value = View.VISIBLE
            _discoveryVisibility.value = View.GONE
        }
    }

    fun discoverService() {
        _loading.value = true
        viewModelScope.launch {
            try {
                _deviceAddress.value = repository.discoverService()
            } catch (e: Exception) {
                // Handle the exception (e.g., show an error message)
                Log.e(_tag, "Discovery failed: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    fun isTestMode(): Boolean {
        return repository.getDiscoveryTarget() == DiscoveryRepo.ServiceKind.PROBE
    }

    fun getDeviceAddress(): String? {
        return _deviceAddress.value
    }
}