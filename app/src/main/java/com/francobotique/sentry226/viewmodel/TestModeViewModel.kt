package com.francobotique.sentry226.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francobotique.sentry226.repository.ServiceRepo
import kotlinx.coroutines.launch

class TestModeViewModel(private val repository: ServiceRepo) : ViewModel() {
    // LiveData to observe the loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    // LiveData to observe the visibility (show/hide) of progress spinner (depends on _loading state)
    private val _progressVisibility = MediatorLiveData<Int>()
    val progressVisibility: LiveData<Int> get() = _progressVisibility

    // LiveData to observe the visibility(show/hide) of Calibration Layout
    private val _calibrationVisibility = MutableLiveData<Int>()
    val calibrationVisibility: LiveData<Int> get() = _calibrationVisibility

    // LiveData to display toast messages (command status or errors)
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    //LiveData that flags the activity should be closed (disconnected or reboot requested)
    private val _closeActivity = MutableLiveData<Boolean>()
    val closeActivity: LiveData<Boolean> get() = _closeActivity

    //LiveData to present calibration config
    private val _calibrationData = MutableLiveData<String>()
    val calibrationData: LiveData<String> get() = _calibrationData

    // LiveData to present probe data
    private val _probeData = MutableLiveData<String>()
    val probeData: LiveData<String> get() = _probeData

    private val _tag = "TestModeViewModel"

    init {
        // initialize progress bar visibility based on the loading value
        _progressVisibility.addSource(_loading) { loading ->
            _progressVisibility.value = if (loading) View.VISIBLE else View.GONE
        }

        _loading.value = false
        _calibrationVisibility.value = View.GONE
        _closeActivity.value = false
        _calibrationData.value = ""
        _probeData.value = ""
    }

    fun toggleCalibration() {
        if (_calibrationVisibility.value == View.VISIBLE) {
            _calibrationVisibility.value = View.GONE
        } else {
            _calibrationVisibility.value = View.VISIBLE
        }
    }

    fun connect() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.connect()
                _toastMessage.value = "Connected to device"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to connect to device: ${e.message}"
                _closeActivity.value = true
            } finally {
                _loading.value = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.disconnect()
                _toastMessage.value = "Disconnected from device"
                _closeActivity.value = true
            } catch (e: Exception) {
                _toastMessage.value = "Failed to disconnect from device: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.reboot()
                _toastMessage.value = "Device is rebooting"
                _closeActivity.value = true
            } catch (e: Exception) {
                _toastMessage.value = "Failed to reboot device: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun getCalibration() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val config = repository.getConfiguration()
                _calibrationData.value = config
            } catch (e: Exception) {
                _toastMessage.value = "Failed to get calibration: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun applyCalibration(config: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.applyConfiguration(config)
                _toastMessage.value = "Calibration applied successfully"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to apply calibration: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun readData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val data = repository.getTestData()
                _probeData.value += data
            } catch (e: Exception) {
                _toastMessage.value = "Failed to read data: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

}
