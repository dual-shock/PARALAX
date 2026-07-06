package com.dual_shock.paralax.ui.trajectorysetup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dual_shock.paralax.Screen
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository

data class TrajectorySetupScreenUIState(
    val site: LaunchSite? = null,
    val hasGrib2: Boolean = false,
    val hasInterpol: Boolean = false
)

class TrajectorySetupViewModel(
    private val repository: LaunchSiteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val launchsite = savedStateHandle.toRoute<Screen.LaunchSiteDetail>()
    private val _uiState = MutableStateFlow(TrajectorySetupScreenUIState())
    val uiState: StateFlow<TrajectorySetupScreenUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLaunchSites()
                .collect { sites ->
                    val site = sites.find { it.id == launchsite.id }
                    _uiState.value = _uiState.value.copy(
                        site = site,
                        hasGrib2 = site?.let { isInGrib2Coverage(it.lat, it.lon) } ?: false,
                        hasInterpol = site?.interpolName != null
                    )
                }
        }
    }

    private fun isInGrib2Coverage(lat: Double, lon: Double): Boolean {
        return lat in 55.35..64.25 && lon in -1.45..14.51
    }

    companion object {
        fun Factory(repository: LaunchSiteRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TrajectorySetupViewModel(
                    repository = repository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}