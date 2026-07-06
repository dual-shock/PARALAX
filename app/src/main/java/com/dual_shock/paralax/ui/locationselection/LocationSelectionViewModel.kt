package com.dual_shock.paralax.ui.locationselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.interpol.InterpolRepository
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class GribFidelity { GRIB2, GRIB1, OUTSIDE_RANGE }

data class LocationSelectionUIState(
    val name: String = "",
    val lat: Double = 63.0,
    val lon: Double = 10.0,
    val editSiteId: Int? = null,
    val isEditMode: Boolean = false,
    val originalName: String = "",
    val originalLat: Double = 63.0,
    val originalLon: Double = 10.0,
    val closestInterpolStation: String? = null,
    val closestInterpolDistanceKm: Double? = null,
    val isSearchingInterpol: Boolean = false,
    val gribFidelity: GribFidelity? = null,
    val altitude: Int? = null,
    val hasChanges: Boolean = false,
    val originalsLoaded: Boolean = false,
    val initialPositioningDone: Boolean = false
)

class LocationSelectionViewModel(
    private val repository: LaunchSiteRepository,
    private val interpolRepository: InterpolRepository,
    private val editSiteId: Int?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSelectionUIState(
        editSiteId = editSiteId,
        isEditMode = editSiteId != null
    ))
    val uiState: StateFlow<LocationSelectionUIState> = _uiState.asStateFlow()

    init {
        if (editSiteId != null) {
            viewModelScope.launch {
                val sites = repository.getLaunchSite(editSiteId).first()
                sites.firstOrNull()?.let { site ->
                    _uiState.value = _uiState.value.copy(
                        name = site.name,
                        lat = site.lat,
                        lon = site.lon,
                        altitude = site.altitude,
                        originalName = site.name,
                        originalLat = site.lat,
                        originalLon = site.lon,
                        gribFidelity = calculateGribFidelity(site.lat, site.lon),
                        originalsLoaded = true
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            name = name,
            hasChanges = checkHasChanges(name = name)
        )
    }

    fun updateLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            lat = lat,
            lon = lon,
            hasChanges = checkHasChanges(lat = lat, lon = lon)
        )
    }

    fun updateAltitude(altitude: Int?) {
        _uiState.value = _uiState.value.copy(altitude = altitude)
    }

    fun onCameraMoving() {
        _uiState.value = _uiState.value.copy(
            isSearchingInterpol = true,
            altitude = null,
            gribFidelity = null
        )
    }

    fun onMapIdle() {
        val state = _uiState.value
        _uiState.value = state.copy(
            gribFidelity = calculateGribFidelity(state.lat, state.lon),
            isSearchingInterpol = true
        )
        searchInterpolStation(state.lat, state.lon)
    }

    fun onInitialPositioningComplete(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            lat = lat,
            lon = lon,
            originalLat = lat,
            originalLon = lon,
            initialPositioningDone = true,
            hasChanges = false
        )
    }

    private fun searchInterpolStation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val tempSite = LaunchSite(
                name = "temp",
                lat = lat,
                lon = lon,
                interpolName = null,
                isDefault = false
            )
            val closest = interpolRepository.linkClosestInterpolLocation(tempSite, 2.0)
            val distanceKm = closest?.let { haversineDistance(lat, lon, it.lat, it.lon) }
            _uiState.value = _uiState.value.copy(
                closestInterpolStation = closest?.name,
                closestInterpolDistanceKm = distanceKm,
                isSearchingInterpol = false
            )
        }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun checkHasChanges(
        name: String = _uiState.value.name,
        lat: Double = _uiState.value.lat,
        lon: Double = _uiState.value.lon
    ): Boolean {
        val state = _uiState.value
        if (state.isEditMode && !state.initialPositioningDone) return false
        val latChanged = abs(lat - state.originalLat) > 0.0001
        val lonChanged = abs(lon - state.originalLon) > 0.0001
        return name != state.originalName || latChanged || lonChanged
    }

    private fun calculateGribFidelity(lat: Double, lon: Double): GribFidelity {
        val inGrib2 = lat in 55.35..64.25 && lon in -1.45..14.51
        if (inGrib2) return GribFidelity.GRIB2
        val inGrib1 = lat in 40.0..90.0 && lon in -30.0..60.0
        if (inGrib1) return GribFidelity.GRIB1
        return GribFidelity.OUTSIDE_RANGE
    }

    fun submitLocation(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.name.ifBlank { "%.2f, %.2f".format(state.lat, state.lon) }

            if (state.isEditMode && state.editSiteId != null) {
                repository.update(state.editSiteId, name, state.lat, state.lon, state.altitude)
                repository.clearInterpolName(state.editSiteId)
                val tempSite = LaunchSite(
                    name = name,
                    lat = state.lat,
                    lon = state.lon,
                    interpolName = null,
                    isDefault = false
                )
                val closest = interpolRepository.linkClosestInterpolLocation(tempSite, 2.0)
                val closestName = closest?.name
                if (closestName != null) {
                    repository.insertInterpol(state.editSiteId, closestName)
                }
                val sites = repository.getLaunchSites().first()
                val editedIndex = sites.indexOfFirst { it.id == state.editSiteId }
                onComplete(editedIndex.coerceAtLeast(0))
            } else {
                val site = LaunchSite(
                    name = name,
                    lat = state.lat,
                    lon = state.lon,
                    altitude = state.altitude,
                    interpolName = null,
                    isDefault = false
                )
                val siteId = repository.insert(site)
                val closest = interpolRepository.linkClosestInterpolLocation(site, 2.0)
                println("closest for ${site.name}: $closest")
                val closestName = closest?.name
                if (closestName != null) {
                    repository.insertInterpol(siteId.toInt(), closestName)
                }
                val sites = repository.getLaunchSites().first()
                val insertedIndex = sites.indexOfFirst { it.name == name && it.lat == state.lat && it.lon == state.lon }
                onComplete(insertedIndex.coerceAtLeast(0))
            }
        }
    }

    fun deleteLocation(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.editSiteId != null) {
                repository.deleteById(state.editSiteId)
                onComplete()
            }
        }
    }

    companion object {
        fun Factory(repository: LaunchSiteRepository, interpolRepository: InterpolRepository, editSiteId: Int?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LocationSelectionViewModel(repository, interpolRepository, editSiteId)
            }
        }
    }
}
