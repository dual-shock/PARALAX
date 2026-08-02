package com.dual_shock.paralax.data.hour

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dual_shock.paralax.data.datastore.watchedHoursDataStore
import com.dual_shock.paralax.data.frost.FrostRepository
import com.dual_shock.paralax.data.grib.GribRepository
import com.dual_shock.paralax.data.interpol.InterpolRepository
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.locationforecast.LocationForecastRepository
import com.dual_shock.paralax.data.settings.SettingsRepository
import com.dual_shock.paralax.model.hour.HourChange
import com.dual_shock.paralax.model.hour.HourData
import com.dual_shock.paralax.model.hour.WatchedHour
import com.dual_shock.paralax.model.hour.compareWith
import com.dual_shock.paralax.model.hour.toSnapshot
import com.dual_shock.paralax.model.interpol.InterpolUnits
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.model.settings.LaunchWindowLimits
import com.dual_shock.paralax.ui.dashboard.AtmosphericLayerMapper
import com.dual_shock.paralax.ui.dashboard.ForecastMapper
import com.dual_shock.paralax.ui.dashboard.TimeZoneHelper
import java.time.Instant
import kotlin.collections.plus
import kotlin.math.abs

private const val TAG = "HourRepository"

class HourRepository (
    private val locationForecastRepo: LocationForecastRepository,
    private val interpolRepo: InterpolRepository,
    private val gribRepo: GribRepository,
    private val settingsRepo: SettingsRepository,
    private val frostRepo: FrostRepository,
    private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val watchedHoursStore: DataStore<List<WatchedHour>> = context.watchedHoursDataStore

    private val _hours = MutableStateFlow<List<HourData>>(emptyList())
    val hours: StateFlow<List<HourData>> = _hours.asStateFlow()

    private val _selectedTimestamp = MutableStateFlow<String?>(null)
    val selectedTimestamp: StateFlow<String?> = _selectedTimestamp.asStateFlow()

    private val _watchedHours = MutableStateFlow<List<WatchedHour>>(emptyList())
    val watchedHours: StateFlow<List<WatchedHour>> = _watchedHours.asStateFlow()

    private val _hourChanges = MutableStateFlow<List<HourChange>>(emptyList())
    val hourChanges: StateFlow<List<HourChange>> = _hourChanges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentSite: LaunchSite? = null
    private var currentSettings: LaunchWindowLimits = LaunchWindowLimits()

    init {
        scope.launch {
            watchedHoursStore.data.collect { watched ->
                _watchedHours.value = watched
            }
        }
        scope.launch {
            settingsRepo.settings.collect { settings ->
                currentSettings = settings
                if (_hours.value.isNotEmpty()) {
                    recomputeAllScores()
                }
            }
        }
    }

    suspend fun initializeForSite(site: LaunchSite) {
        _isLoading.value = true
        currentSite = site
        Log.d(TAG, "Initializing for site: ${site.name}")

        try {
            val forecasts = locationForecastRepo.getData(site.lat, site.lon)
            Log.d(TAG, "Fetched ${forecasts.size} forecast entries")
            val hourSlots = createHourSlotsFromForecasts(forecasts)
            _hours.value = hourSlots
            Log.d(TAG, "Created ${hourSlots.size} hour slots from actual forecast data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch forecasts", e)
            _hours.value = emptyList()
        }

        try {
            val interpolName = site.interpolName
            if (!interpolName.isNullOrBlank()) {
                val interpolData = interpolRepo.getAllData(interpolName)
                Log.d(TAG, "Fetched interpol data for ${interpolData.size} timestamps")
                mergeInterpolData(interpolData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch interpol data", e)
        }
        recomputeAllScores()
        runChangeDetection(site.id)

        externalScope.launch {
            preloadAllAtmosphericLayers()
        }

        try {
            val strikes = frostRepo.getRecentLightningStrikes(lat = site.lat, lon = site.lon, hoursBack = 2)
            val nowUtc = Instant.now().toString().take(13)
            val closestTimestamp = _hours.value
                .filter { it.utcTimestamp.take(13) >= nowUtc }
                .minByOrNull { it.utcTimestamp }?.utcTimestamp
            _hours.value = _hours.value.map { h ->
                h.copy(lightningStrikeCount = if (h.utcTimestamp == closestTimestamp) strikes.size else null)
                //h.copy(lightningStrikeCount = if (h.utcTimestamp == closestTimestamp) 3 else null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lightning", e)
        }
        _isLoading.value = false
    }

    private fun createHourSlotsFromForecasts(forecasts: List<ForecastEntry>): List<HourData> {
        return forecasts.map { forecast ->
            val utcTimestamp = forecast.time
            val utcHour = utcTimestamp.substringAfter("T").take(2).toIntOrNull() ?: 0
            val (localHour, dayOffset) = TimeZoneHelper.utcToLocal(utcTimestamp)

            HourData(
                utcTimestamp = utcTimestamp,
                hour = utcHour,
                localHour = localHour,
                dayOffset = dayOffset,
                forecastEntry = forecast,
                interpolUnits = null,
                altWindSpeed = null,
                maxShear = null,
                viabilityScore = 0,
                gribSlotTimestamp = HourData.Companion.roundToNearest3Hours(utcTimestamp),
                atmosphericLayers = null,
                shearLayers = null,
                isWatched = false,
                hasChangedSinceLastCheck = false
            )
        }
    }

    private fun mergeInterpolData(interpolData: Map<String, List<InterpolUnits>>) {
        _hours.value = _hours.value.map { hourData ->
            val matchingInterpol = interpolData.entries.find { (time, _) ->
                time.take(13) == hourData.utcTimestamp.take(13)
            }?.value

            if (matchingInterpol != null) {
                val (altWind, shear) = deriveAltitudeMetrics(matchingInterpol)
                hourData.copy(
                    interpolUnits = matchingInterpol,
                    altWindSpeed = altWind,
                    maxShear = shear
                )
            } else {
                hourData
            }
        }
    }

    private fun deriveAltitudeMetrics(interpolUnits: List<InterpolUnits>): Pair<Double?, Double?> {
        if (interpolUnits.isEmpty()) return null to null

        val sorted = interpolUnits
            .filter { it.air_pressure_ml != null }
            .sortedBy { it.air_pressure_ml }

        val altWindSpeed = sorted.firstOrNull()?.wind_speed_ml

        val maxShear = sorted.zipWithNext { lower, upper ->
            val lowerSpeed = lower.wind_speed_ml ?: 0.0
            val upperSpeed = upper.wind_speed_ml ?: 0.0
            abs(upperSpeed - lowerSpeed)
        }.maxOrNull()

        return altWindSpeed to maxShear
    }

    private fun recomputeAllScores() {
        _hours.value = _hours.value.map { hourData ->
            computeScore(hourData, currentSettings)
        }
    }

    private fun computeScore(hourData: HourData, settings: LaunchWindowLimits): HourData {
        val forecast = hourData.forecastEntry
        if (forecast == null) {
            return hourData.copy(viabilityScore = 0)
        }

        val result = ForecastMapper.calculatePercentage(
            forecast,
            hourData.altWindSpeed,
            hourData.atmosphericLayers,
            hourData.maxShear,
            settings
        )

        return hourData.copy(viabilityScore = result.score ?: 0, specifics = result.specifics ?: emptyList())
    }

    suspend fun preloadAllAtmosphericLayers() {
        val site = currentSite ?: return
        val distinctSlots = _hours.value.map { it.gribSlotTimestamp }.distinct()

        for (gribSlot in distinctSlots) {
            val alreadyLoaded = _hours.value.any { it.gribSlotTimestamp == gribSlot && it.atmosphericLayers != null }
            if (alreadyLoaded) continue

            try {
                val gribLayers = withContext(Dispatchers.IO) {
                    gribRepo.initializeForTimeIfNeeded(gribSlot)
                    gribRepo.getAtmosphericProfile(site.lat, site.lon)
                }
                if (gribLayers.isEmpty()) continue

                _hours.value = _hours.value.map { h ->
                    if (h.gribSlotTimestamp == gribSlot && h.interpolUnits?.isNotEmpty() == true) {
                        val atmosphericLayers = AtmosphericLayerMapper.mappingBasedonHpa(h.interpolUnits, gribLayers)
                        val shearLayers = AtmosphericLayerMapper.shearBetweenLayers(atmosphericLayers)
                        h.copy(atmosphericLayers = atmosphericLayers, shearLayers = shearLayers)
                    } else h
                }
                recomputeAllScores()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload GRIB for slot $gribSlot", e)
            }
        }
    }

    suspend fun selectHour(utcTimestamp: String) {
        _selectedTimestamp.value = utcTimestamp
        val site = currentSite ?: return

        val hourData = _hours.value.find { it.utcTimestamp == utcTimestamp } ?: return

        if (hourData.atmosphericLayers != null) {
            Log.d(TAG, "Atmospheric layers already loaded for $utcTimestamp")
            return
        }

        val gribSlot = hourData.gribSlotTimestamp
        Log.d(TAG, "Loading GRIB for slot $gribSlot")

        try {
            val gribLayers = withContext(Dispatchers.IO) {
                gribRepo.initializeForTimeIfNeeded(gribSlot)
                gribRepo.getAtmosphericProfile(site.lat, site.lon)
            }

            if (gribLayers.isEmpty()) {
                Log.w(TAG, "No GRIB layers returned")
                return
            }

            val interpolUnits = hourData.interpolUnits
            if (interpolUnits.isNullOrEmpty()) {
                Log.w(TAG, "No interpol data for atmospheric layer mapping")
                return
            }

            val atmosphericLayers = AtmosphericLayerMapper.mappingBasedonHpa(interpolUnits, gribLayers)
            val shearLayers = AtmosphericLayerMapper.shearBetweenLayers(atmosphericLayers)

            _hours.value = _hours.value.map { h ->
                if (h.gribSlotTimestamp == gribSlot) {
                    h.copy(atmosphericLayers = atmosphericLayers, shearLayers = shearLayers)
                } else {
                    h
                }
            }

            Log.d(TAG, "Loaded ${atmosphericLayers.size} atmospheric layers for slot $gribSlot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load GRIB/atmospheric layers", e)
        }
    }

    fun getHoursSortedByViability(): List<HourData> {
        return _hours.value
            .filter { it.forecastEntry != null }
            .sortedByDescending { it.viabilityScore }
    }

    fun getSelectedHourData(): HourData? {
        val timestamp = _selectedTimestamp.value ?: return null
        return _hours.value.find { it.utcTimestamp == timestamp }
    }

    suspend fun toggleWatched(utcTimestamp: String, siteId: Int) {
        val hourData = _hours.value.find { it.utcTimestamp == utcTimestamp } ?: return

        val currentWatched = watchedHoursStore.data.first()
        val existing = currentWatched.find {
            it.savedHourData.utcTimestamp == utcTimestamp && it.siteId == siteId
        }

        val newWatched = if (existing != null) {
            currentWatched.filter { it != existing }
        } else {
            currentWatched + WatchedHour(
                siteId = siteId,
                watchedAt = System.currentTimeMillis(),
                savedHourData = hourData.toSnapshot()
            )
        }

        watchedHoursStore.updateData { newWatched }

        _hours.value = _hours.value.map { h ->
            if (h.utcTimestamp == utcTimestamp) {
                h.copy(isWatched = existing == null)
            } else {
                h
            }
        }
        println("toggleWatched repo: existing=$existing currentWatched.size=${currentWatched.size}")
        runChangeDetection(siteId)

    }

    private fun runChangeDetection(siteId: Int) {
        println("runChangeDetection: siteId=$siteId watched=${_watchedHours}")
        val watched = _watchedHours.value.filter { it.siteId == siteId }
        val changes = mutableListOf<HourChange>()


        for (watchedHour in watched) {
            val currentHour = _hours.value.find {
                it.utcTimestamp == watchedHour.savedHourData.utcTimestamp
            }
            if (currentHour != null) {
                changes.add(watchedHour.compareWith(currentHour))
            }
        }

        _hourChanges.value = changes

        _hours.value = _hours.value.map { h ->
            val isWatched = watched.any { it.savedHourData.utcTimestamp == h.utcTimestamp }
            val hasChanged = changes.any {
                it.currentHourData.utcTimestamp == h.utcTimestamp && it.hasChanged
            }
            h.copy(isWatched = isWatched, hasChangedSinceLastCheck = hasChanged)
        }

        if (changes.isNotEmpty()) {
            Log.d(TAG, "Detected ${changes.size} significant changes in watched hours")
        }
    }

    suspend fun clearHourChanges() {
        val currentWatched = watchedHoursStore.data.first()
        val currentSiteId = currentSite?.id ?: return

        val updated = currentWatched.map { watched ->
            if (watched.siteId == currentSiteId) {
                val currentHour = _hours.value.find {
                    it.utcTimestamp == watched.savedHourData.utcTimestamp
                }
                if (currentHour != null) {
                    watched.copy(
                        savedHourData = currentHour.toSnapshot(),
                        watchedAt = System.currentTimeMillis()
                    )
                } else {
                    watched
                }
            } else {
                watched
            }
        }

        watchedHoursStore.updateData { updated }
        _hourChanges.value = emptyList()

        _hours.value = _hours.value.map { it.copy(hasChangedSinceLastCheck = false) }
    }

    fun getWatchedHoursForSite(siteId: Int): List<WatchedHour> {
        return _watchedHours.value.filter { it.siteId == siteId }
    }

    fun getAvailableDayOffsets(): List<Int> {
        return _hours.value.map { it.dayOffset }.distinct().sorted()
    }

    fun getAvailableLocalHoursForDay(dayOffset: Int): List<Int> {
        return _hours.value
            .filter { it.dayOffset == dayOffset }
            .map { it.localHour }
            .distinct()
            .sorted()
    }

    fun hourExistsAt(dayOffset: Int, localHour: Int): Boolean {
        return _hours.value.any { it.dayOffset == dayOffset && it.localHour == localHour }
    }
}
