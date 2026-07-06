package com.dual_shock.paralax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.settings.SettingsRepository
import com.dual_shock.paralax.model.settings.LaunchWindowLimits


class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LaunchWindowLimits()
    )

    // To save all at once on submit
    fun saveSettings(limits: LaunchWindowLimits) {
        viewModelScope.launch {
            repository.updateData(limits)
        }
    }
    companion object {
        fun Factory(repository: SettingsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(repository = repository)
            }
        }
    }
}
