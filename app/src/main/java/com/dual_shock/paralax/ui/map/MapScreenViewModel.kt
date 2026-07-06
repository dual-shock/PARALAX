package com.dual_shock.paralax.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository


data class HomeMapScreenUIState(
    val launchSites: List<LaunchSite> = emptyList()
)


class HomeMapScreenViewModel(
    private val repository: LaunchSiteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeMapScreenUIState())
    val uiState: StateFlow<HomeMapScreenUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLaunchSites()
                .collect { sites ->
                    _uiState.value = _uiState.value.copy(launchSites = sites)
                }
        }
    }

    companion object {
        fun Factory(repository: LaunchSiteRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeMapScreenViewModel(
                    repository = repository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}











