package com.francobotique.sentry226.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.francobotique.sentry226.repository.LocalRepo
import com.francobotique.sentry226.repository.ServiceRepo
import com.francobotique.sentry226.repository.data.MetadataData
import com.francobotique.sentry226.viewmodel.data.ResultListItem
import kotlinx.coroutines.launch

class FieldModeViewModel(private val serviceRepo: ServiceRepo,
                         private val localRepo: LocalRepo) : ViewModel() {
    // LiveData to observe the loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    // LiveData to observe the visibility (show/hide) of progress spinner (depends on _loading state)
    private val _progressVisibility = MediatorLiveData<Int>()
    val progressVisibility: LiveData<Int> get() = _progressVisibility


    // LiveData to observe the visibility(show/hide) of Metadata Layout
    private val _metadataVisibility = MutableLiveData<Int>()
    val metadataVisibility: LiveData<Int> get() = _metadataVisibility

    // LiveData to display toast messages (command status or errors)
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    // LiveData that flags the activity should be closed (disconnected, reboot or sampling is requested)
    private val _closeActivity = MutableLiveData<Boolean>()
    val closeActivity: LiveData<Boolean> get() = _closeActivity

    // LiveData to observe when the list of file items is updated
    private val _resultItems = MutableLiveData<List<ResultListItem>>()
    val resultItems: LiveData<List<ResultListItem>> get() = _resultItems

    private val _metadata = MutableLiveData<MetadataData>()
    val metadata: LiveData<MetadataData> get() = _metadata

    private val _tag = "FieldModeViewModel"

    init {
        // initialize progress bar visibility based on the loading value
        _progressVisibility.addSource(_loading) { loading ->
            _progressVisibility.value = if (loading) View.VISIBLE else View.GONE
        }

        _loading.value = false
        _metadataVisibility.value = View.GONE
        _closeActivity.value = false
    }

    fun connect() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Connect to the device
                serviceRepo.connect()
                // Fetch current device metadata
                _metadata.value = serviceRepo.getMetadata()
                _toastMessage.value = "Connected to device"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to connect to device: ${e.message}"
                _closeActivity.value = true
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleMetadata() {
        if (_metadataVisibility.value == View.VISIBLE) {
            _metadataVisibility.value = View.GONE
        } else {
            _metadataVisibility.value = View.VISIBLE
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _loading.value = true
            try {
                serviceRepo.disconnect()
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
                serviceRepo.reboot()
                _toastMessage.value = "Device is rebooting"
                _closeActivity.value = true
            } catch (e: Exception) {
                _toastMessage.value = "Failed to reboot device: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun getResults() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Fetch from the service
                val results = serviceRepo.getResultFiles()
                Log.i(_tag, "Device Results: $results")
                // Convert array of strings to a map of ResultListItem using the file name as key
                val resultMap = results.map { ResultListItem(it, false) }.associateBy { it.fileName }

                // Fetch from the local repository
                val localResults = localRepo.getResultFiles()
                Log.i(_tag, "Local Results: $localResults")
                // Update isDownloaded status based on local results
                localResults.forEach { localResult ->
                    resultMap[localResult]?.let {
                        it.isDownloaded = true
                    }
                }

                // Convert the map back to a list and update the LiveData
                _resultItems.value = resultMap.values.toList()

            } catch (e: Exception) {
                _toastMessage.value = "Failed to get results: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun downloadResult(filename: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Simulate downloading the result file
                // In a real implementation, you would call the repository method to download the file
                _toastMessage.value = "Downloading $filename"

                // Retrieve the file from the service
                val fileData = serviceRepo.getResultFile(filename)
                // Store file data in local repository
                localRepo.storeResultFile(filename, fileData)

                // Create a new list with the updated item
                val currentList = _resultItems.value?.toMutableList() ?: mutableListOf()
                val index = currentList.indexOfFirst { it.fileName == filename }
                if (index != -1) {
                    val updatedItem = currentList[index].copy(isDownloaded = true)
                    currentList[index] = updatedItem
                    _resultItems.value = currentList // Emit the updated list
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to download result: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startSampling() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Synchronize device time
                serviceRepo.syncTime()

                // Apply metadata and GPS data to the device
                // if metadata is null or empty, show a toast message and return
                val metadata = _metadata.value
                if ( metadata == null ) {
                    _toastMessage.value = "Metadata is not set. Please set metadata before starting sampling."
                    return@launch
                }
                serviceRepo.applyMetadata( metadata, localRepo.getGpsData() )

                // Start sampling with metadata and GPS data
                val filename = serviceRepo.startSampling()
                Log.i(_tag, "Sampling started with filename: $filename")

                _toastMessage.value = "Sampling started, Bluetooth connection will be closed"
                _closeActivity.value = true
            } catch (e: Exception) {
                _toastMessage.value = "Failed to start sampling: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun applyMetadata(metadata: MetadataData) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Apply metadata to the device
                serviceRepo.applyMetadata(metadata, localRepo.getGpsData())
                _metadata.value = metadata
                _toastMessage.value = "Metadata applied successfully"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to apply metadata: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
