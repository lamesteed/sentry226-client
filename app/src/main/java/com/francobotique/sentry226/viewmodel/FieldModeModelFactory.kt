package com.francobotique.sentry226.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.francobotique.sentry226.repository.LocalRepo
import com.francobotique.sentry226.repository.ServiceRepo

class FieldModeModelFactory(private val serviceRepo: ServiceRepo,
                            private val localRepo : LocalRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FieldModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FieldModeViewModel(serviceRepo, localRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}