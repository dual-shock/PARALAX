package com.dual_shock.paralax.ui.trajectory
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dual_shock.paralax.Screen
import com.dual_shock.paralax.data.grib.GribRepository
import com.dual_shock.paralax.data.interpol.InterpolRepository
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import com.dual_shock.paralax.data.trajectory.TrajectorySimulator
import com.dual_shock.paralax.data.trajectory.TrajectorySimulatorGrib
import com.dual_shock.paralax.model.trajectory.TrajectoryPoint
import com.dual_shock.paralax.ui.dashboard.TimeZoneHelper
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfig
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfigurationsViewModel
import com.dual_shock.paralax.ui.rocketconfigurations.toDomain

data class TrajectoryUIState(
    val site: LaunchSite? = null,
    val trajectoryPoints: List<TrajectoryPoint> = emptyList(),
    val gribStatus: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val mode: TrajectoryMode? = null,
    val hasInterpol: Boolean = false,
    val hasGrib: Boolean = false
)

class TrajectoryViewModel(
    private val repository: LaunchSiteRepository,
    private val rocketViewModel: RocketConfigurationsViewModel, // should modify to pass uiState instead of whole model not fo this branch though
    private val interpolRepository: InterpolRepository,
    private val gribRepository: GribRepository,
    initialMode: TrajectoryMode,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun createGribCalculator(config: RocketConfig) = TrajectorySimulatorGrib(
        gribRepository = gribRepository,
        rocketConfig = config
    )

    private val _mode = MutableStateFlow(initialMode)
    private val _uiState = MutableStateFlow(TrajectoryUIState(mode = initialMode))
    val uiState: StateFlow<TrajectoryUIState> = _uiState.asStateFlow()

    private val siteID = savedStateHandle.toRoute<Screen.Trajectory>().id

    private fun getTimeString(): String {
        val day = savedStateHandle.toRoute<Screen.Trajectory>().day
        val hour = savedStateHandle.toRoute<Screen.Trajectory>().hour
        val localHour = hour.replace(":00", "").toIntOrNull() ?: 0
        return TimeZoneHelper.buildUtcTimestamp(day - 1, localHour)
    }


    init {
        Log.d("testing", "DEBUG: init started")
        viewModelScope.launch {
            val site = loadSite() ?: return@launch
            val hasGrib = _uiState.value.hasGrib
            val hasInterpol = _uiState.value.hasInterpol
            combine(_mode, rocketViewModel.uiState) { mode, rocketState -> Pair(mode, rocketState) }
                .collectLatest { (mode, rocketState) ->
                    _uiState.value = _uiState.value.copy(
                        mode = mode,
                        trajectoryPoints = emptyList(),
                        gribStatus = null
                    )
                    val canLoadMode = when (mode) {
                        is TrajectoryMode.Grib -> hasGrib
                        is TrajectoryMode.Interpol -> hasInterpol
                        is TrajectoryMode.Average -> hasGrib && hasInterpol
                    }
                    if (canLoadMode) {
                        when (mode) {
                            is TrajectoryMode.Grib -> observeGribStatus(site)
                            is TrajectoryMode.Interpol -> loadInterpol(site)
                            is TrajectoryMode.Average -> loadAverage(site)
                        }
                    }
                }
        }
    }

    fun setMode(mode: TrajectoryMode) {
        _mode.value = mode
    }

    private fun isInGrib1Coverage(lat: Double, lon: Double): Boolean {
        return lat in 40.0..90.0 && lon in -30.0..60.0
    }

    private suspend fun loadSite(): LaunchSite? {
        Log.d("testing", "DEBUG: loading site with id=$siteID")
        return try {
            val sites = repository.getLaunchSite(siteID).first()
            val site = sites.firstOrNull()
            if (site == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Site not found")
                return null
            }
            val hasGrib = isInGrib1Coverage(site.lat, site.lon)
            val hasInterpol = site.interpolName != null
            _uiState.value = _uiState.value.copy(
                site = site,
                isLoading = false,
                hasInterpol = hasInterpol,
                hasGrib = hasGrib
            )
            site
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load: ${e.message}")
            Log.d("testing", "DEBUG: loadSite error = ${e.message}")
            null
        }
    }

    private suspend fun getInterpolPoints(site: LaunchSite): List<TrajectoryPoint> {
        val interpolName = site.interpolName
            ?: throw IllegalStateException("No Interpol data for this site")

        val config = rocketViewModel.uiState.value.toDomain()
        val levels = interpolRepository.getData(interpolName, getTimeString())

        val validLevels = levels.filter { it.air_pressure_ml != null && it.air_temperature_ml != null }
        if (validLevels.size < 3) {
            throw IllegalStateException("Interpol data unavailable for this location (outside MEPS coverage)")
        }

        return withContext(Dispatchers.Default) {
            TrajectorySimulator(config, site, validLevels).doCalculation()
        }
    }


    private suspend fun loadInterpol(site: LaunchSite) {
        try {
            val points = getInterpolPoints(site)
            if (_mode.value is TrajectoryMode.Interpol) {
                _uiState.value = _uiState.value.copy(
                    trajectoryPoints = points,
                    error = null
                )
            }
        } catch (e: Exception) {
            if (_mode.value is TrajectoryMode.Interpol) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load Interpol trajectory"
                )
            }
        }
    }

    private suspend fun getGribPoints(site: LaunchSite): List<TrajectoryPoint> {
        val config = rocketViewModel.uiState.value.toDomain()
        val calculator = createGribCalculator(config)

        val state = calculator.gribState
            .onEach { state ->
                _uiState.value = _uiState.value.copy(
                    gribStatus = when (state) {
                        is GribRepository.GribState.Loading -> "Loading GRIB data..."
                        is GribRepository.GribState.Ready -> "GRIB data ready"
                        is GribRepository.GribState.Error -> "GRIB error: ${state.message}"
                    }
                )
            }
            .first { it is GribRepository.GribState.Ready || it is GribRepository.GribState.Error }

        if (state is GribRepository.GribState.Error) {
            throw IllegalStateException("GRIB error: ${state.message}")
        }

        val result = calculator.calculate(site.lat, site.lon, config)
        return result.points
    }



    private suspend fun observeGribStatus(site: LaunchSite) {
        try {
            _uiState.value = _uiState.value.copy(gribStatus = "Loading GRIB data...")
            val points = getGribPoints(site)
            if (_mode.value is TrajectoryMode.Grib) {
                _uiState.value = _uiState.value.copy(
                    trajectoryPoints = points,
                    gribStatus = "GRIB trajectory ready",
                    error = null
                )
            }
        } catch (e: Exception) {
            if (_mode.value is TrajectoryMode.Grib) {
                _uiState.value = _uiState.value.copy(
                    gribStatus = null,
                    error = e.message ?: "Failed to load GRIB trajectory"
                )
            }
        }
    }

    private suspend fun loadAverage(site: LaunchSite) {
        try {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                gribStatus = "Calculating average trajectory..."
            )

            val interpolPoints = getInterpolPoints(site)
            val gribPoints = getGribPoints(site)
            val averagePoints = averageTrajectoryPoints(interpolPoints, gribPoints)

            if (_mode.value is TrajectoryMode.Average) {
                _uiState.value = _uiState.value.copy(
                    trajectoryPoints = averagePoints,
                    gribStatus = "Average trajectory ready",
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            if (_mode.value is TrajectoryMode.Average) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to calculate average trajectory"
                )
            }
        }
    }


    companion object {
        fun Factory(
            repository: LaunchSiteRepository,
            interpolRepository: InterpolRepository,
            rocketViewModel: RocketConfigurationsViewModel,
            gribRepository: GribRepository,  // swap gribCalculator for gribRepository
            initialMode: TrajectoryMode
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                TrajectoryViewModel(
                    repository = repository,
                    rocketViewModel = rocketViewModel,
                    interpolRepository = interpolRepository,
                    gribRepository = gribRepository,
                    initialMode = initialMode,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
    }

