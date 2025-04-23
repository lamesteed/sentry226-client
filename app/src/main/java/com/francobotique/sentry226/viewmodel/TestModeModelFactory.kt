package com.francobotique.sentry226.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.francobotique.sentry226.repository.ServiceRepo

class TestModeModelFactory(private val repository: ServiceRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestModeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}