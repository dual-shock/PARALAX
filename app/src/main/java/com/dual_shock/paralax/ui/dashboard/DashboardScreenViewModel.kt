package com.dual_shock.paralax.ui.dashboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.hour.HourRepository
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import com.dual_shock.paralax.model.hour.HourChange
import com.dual_shock.paralax.model.hour.HourData
import com.dual_shock.paralax.model.hour.WatchedHour
import com.dual_shock.paralax.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale


data class DashboardUIState(
    val launchSites: List<LaunchSite> = emptyList(),
    val selectedSiteIndex: Int = 0,
    val selectedDay: Int = 1,
    val selectedBaseHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val selectedSliceIndex: Int = 0,
    val nowHourLabel: String = String.format("%02d:00 (now)", Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
    val mode: DashboardMode? = DashboardMode.Upcoming,
    val selectedTimestamp: String? = null,
    val hours: List<HourData> = emptyList(),
    val hourChanges: List<HourChange> = emptyList(),
    val watchedHours: List<WatchedHour> = emptyList(),
    val isLoading: Boolean = true
) {
    val effectiveDay: Int get() = selectedDay + selectedBaseHour / 24
    val effectiveHour: Int get() = selectedBaseHour % 24
    val utcTimestamp: String get() = TimeZoneHelper.buildUtcTimestamp(effectiveDay - 1, effectiveHour)

    val selectedHour: HourData? get() =
        selectedTimestamp?.let { ts -> hours.find { it.utcTimestamp == ts } }

    val upcomingHours: List<HourData> get() =
        hours.sortedBy { it.utcTimestamp }

    val bestHours: List<HourData> get() =
        hours.filter { it.forecastEntry != null }
            .sortedByDescending { it.viabilityScore }
            .take(6)


    val currentSlice: List<HourData> get() {
        val slices = listOf(0 to 6, 6 to 12, 12 to 18, 18 to 24)
        val (sliceStart, sliceEnd) = slices.getOrElse(selectedSliceIndex) { 0 to 6 }
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNowSlice = selectedDay == 1 && currentHour in sliceStart until sliceEnd
        val effectiveStart = if (isNowSlice) currentHour else sliceStart

        return upcomingHours
            .filter { it.dayOffset == selectedDay - 1 }
            .filter { it.localHour in effectiveStart until sliceEnd }
    }

}

class DashboardScreenViewModel(
    private val repository: LaunchSiteRepository,
    private val settingsViewModel: SettingsViewModel,
    private val hourRepository: HourRepository,
    private val savedStateHandle: SavedStateHandle,
    initialMode: DashboardMode
) : ViewModel() {

    private val _mode = MutableStateFlow(initialMode)
    private val _uiState = MutableStateFlow(DashboardUIState(mode = initialMode))
    val uiState: StateFlow<DashboardUIState> = _uiState.asStateFlow()
    private var selectHourJob: Job? = null
    private var lastFetchHour: String? = null


    val settings = settingsViewModel.settings

    init {
        viewModelScope.launch {
            repository.getLaunchSites().collect { sites ->
                _uiState.update { it.copy(launchSites = sites) }
                val site = sites.getOrNull(_uiState.value.selectedSiteIndex)
                if (site != null) fetchForecast(site)
            }
        }

        viewModelScope.launch {
            hourRepository.hours.collect { hours ->
                _uiState.update { it.copy(hours = hours) }
            }
        }

        viewModelScope.launch {
            hourRepository.hourChanges.collect { changes ->
                _uiState.update { it.copy(hourChanges = changes) }
            }
        }

        viewModelScope.launch {
            hourRepository.watchedHours.collect { watched ->
                _uiState.update { it.copy(watchedHours = watched) }
            }
        }

        viewModelScope.launch {
            hourRepository.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoading = loading) }
            }
        }
    }


    fun selectSite(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSiteIndex = index)
        val site = _uiState.value.launchSites.getOrNull(index)
        if (site != null) fetchForecast(site)
        hourRepository.getWatchedHoursForSite(_uiState.value.selectedSiteIndex)
        logSelection("selectSite")
    }
    fun selectDay(day: Int) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hour = if (day == 1) currentHour else 0
        val label = if (day == 1) {
            String.format("%02d:00 (now)", currentHour)
        } else {
            "00:00"
        }

        _uiState.value = _uiState.value.copy(
            selectedDay = day,
            selectedBaseHour = hour,
            selectedSliceIndex = sliceIndexForHour(hour),
            nowHourLabel = label
        )

        logSelection("selectDay")
    }


    private fun sliceIndexForHour(hour: Int): Int {
        return (hour.coerceIn(0, 23) / 6).coerceIn(0, 3)
    }

    fun selectSpecificHour(hour: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val current = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val isNow = _uiState.value.selectedDay == 1 && safeHour == current

        _uiState.value = _uiState.value.copy(
            selectedBaseHour = safeHour,
            selectedSliceIndex = sliceIndexForHour(safeHour),
            nowHourLabel = if (isNow) {
                String.format("%02d:00 (now)", safeHour)
            } else {
                String.format("%02d:00", safeHour)
            }
        )

        logSelection("selectSpecificHour")
    }


    fun selectHour(hour: HourData) {
        selectHourJob?.cancel()
        _uiState.update { it.copy(selectedTimestamp = hour.utcTimestamp) }
        viewModelScope.launch {
            try {
                hourRepository.selectHour(hour.utcTimestamp)
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to load GRIB", e)
            }
        }
    }

    fun selectHourDebounced(hour: HourData) {
        _uiState.update { it.copy(selectedTimestamp = hour.utcTimestamp) }
        selectHourJob?.cancel()
        selectHourJob = viewModelScope.launch {
            delay(300)
            try {
                hourRepository.selectHour(hour.utcTimestamp)
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to load GRIB", e)
            }
        }
    }

    fun selectSlice(index: Int, startHour: Int) {
        _uiState.update {
            it.copy(
                selectedSliceIndex = index,
                selectedBaseHour = startHour,
            )
        }
        logSelection("selectSlice")
    }

    fun fetchForecast(site: LaunchSite) {
        viewModelScope.launch {
            try {
                hourRepository.initializeForSite(site)
                lastFetchHour = Instant.now().toString().take(13)
                Log.d("DashboardVM", "HourRepository initialized for site: ${site.name}")
            } catch (e: Exception) {
                Log.e("DashboardVM", "HourRepository init failed", e)
            }
        }
    }

    fun refreshIfStale() {
        val currentHour = Instant.now().toString().take(13)
        if (lastFetchHour != currentHour) {
            val site = _uiState.value.launchSites.getOrNull(_uiState.value.selectedSiteIndex)
            if (site != null) {
                Log.d("DashboardVM", "Data stale (last=$lastFetchHour, now=$currentHour), refreshing")
                fetchForecast(site)
            }
        }
    }


    private fun logSelection(trigger: String) {
        val state = _uiState.value
        val site = state.launchSites.getOrNull(state.selectedSiteIndex)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, state.effectiveDay - 1)
        val hour = state.effectiveHour
        val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.time)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            dayOfMonth in 11..13 -> "th"
            dayOfMonth % 10 == 1 -> "st"
            dayOfMonth % 10 == 2 -> "nd"
            dayOfMonth % 10 == 3 -> "rd"
            else -> "th"
        }
        Log.d("testing", "[$trigger] location=${site?.name ?: "none"}, day=${state.selectedDay}, effectiveDay=${state.effectiveDay}, time=$monthName $dayOfMonth$suffix ${String.format("%02d:00", hour)}")
    }

    fun getFormattedHour(): String {
        return String.format("%02d:00", _uiState.value.effectiveHour)
    }

    suspend fun toggleWatched(hour: HourData){
        val site = _uiState.value.launchSites.getOrNull(_uiState.value.selectedSiteIndex) ?: return
        println("toggleWatched called: timestamp= ${hour.utcTimestamp}, siteId=${site.id} ")
        val hourData = _uiState.value.hours.find { it.utcTimestamp == hour.utcTimestamp  } ?: run {
            println("No hourData found for $hour.utcTimestamp ")
            return
        }

        hourRepository.toggleWatched(hour.utcTimestamp, site.id)
    }

    suspend fun toggleWatchedTimestamp(timestamp: String) {
        val site = _uiState.value.launchSites.getOrNull(_uiState.value.selectedSiteIndex) ?: return
        println("toggleWatched called: timestamp=$timestamp siteId=${site.id}")
        hourRepository.toggleWatched(timestamp,site.id )
    }


    fun setMode(mode: DashboardMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    companion object {
        fun Factory(
            repository: LaunchSiteRepository,
            settingsViewModel: SettingsViewModel,
            hourRepository: HourRepository,
            initialMode: DashboardMode.Upcoming
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardScreenViewModel(
                    repository = repository,
                    settingsViewModel = settingsViewModel,
                    hourRepository = hourRepository,
                    savedStateHandle = createSavedStateHandle(),
                    initialMode = initialMode
                )
            }
        }
    }
}