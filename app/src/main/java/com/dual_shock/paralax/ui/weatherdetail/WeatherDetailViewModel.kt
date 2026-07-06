package com.dual_shock.paralax.ui.weatherdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dual_shock.paralax.data.hour.HourRepository
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.ui.dashboard.TimeZoneHelper

data class WeatherDetailUiState(
    val entry: ForecastEntry? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class WeatherDetailViewModel(
    private val hourRepository: HourRepository,
    private val day: Int,
    private val hour: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherDetailUiState())
    val uiState: StateFlow<WeatherDetailUiState> = _uiState.asStateFlow()

    init {
        val localHour = hour.split(":")[0].toIntOrNull() ?: 0
        val targetPrefix = TimeZoneHelper.buildUtcMatchPrefix(day - 1, localHour)
        val hourData = hourRepository.hours.value.find { it.utcTimestamp.startsWith(targetPrefix) }
        _uiState.value = WeatherDetailUiState(entry = hourData?.forecastEntry, loading = false)
    }

    companion object {
        fun Factory(
            hourRepository: HourRepository,
            day: Int,
            hour: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WeatherDetailViewModel(
                    hourRepository = hourRepository,
                    day = day,
                    hour = hour
                )
            }
        }
    }
}
