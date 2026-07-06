package com.dual_shock.paralax.data.grib

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.dual_shock.paralax.data.locationforecast.LocationForecastRepository
import com.dual_shock.paralax.model.grib.AtmosphericLayer
import com.dual_shock.paralax.model.grib.GribMessageMeta
import com.dual_shock.paralax.model.grib.GribParam
import com.dual_shock.paralax.model.grib.GridDefinition
import com.dual_shock.paralax.model.grib.grib1ParamToEnum
import com.dual_shock.paralax.model.grib.grib2ParamToEnum
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import java.io.File
import kotlin.collections.plus
import kotlin.math.cos
import kotlin.math.sin

private const val Rd = 287.05

class GribRepository(
    private val dataSource: GribDataSource,
    private val locationForecastRepo: LocationForecastRepository
) {
    private var grib1Bytes: ByteArray? = null
    private var grib1Grid: GridDefinition? = null
    private var grib1Messages: List<GribMessageMeta>? = null

    private var grib2File: File? = null
    private var grib2Ready: Boolean = false
    private var grib2Region: List<Grib2Decoder.Grib2RegionRecord>? = null

    private var loadedGrib1Time: String? = null
    private var loadedGrib2Time: String? = null

    sealed class GribState {
        object Loading : GribState()
        object Ready : GribState()
        data class Error(val message: String) : GribState()
    }

    private val _state = MutableStateFlow<GribState>(GribState.Loading)
    val state: StateFlow<GribState> = _state.asStateFlow()

    fun getLoadedTime(): String? = loadedGrib2Time ?: loadedGrib1Time

    suspend fun initialize(targetTime: String? = null) = withContext(Dispatchers.IO) {
        try {
            _state.value = GribState.Loading
            grib2Region = null
            Log.d(TAG, "Initializing GRIB repository for time=$targetTime")

            val dlStart1 = System.currentTimeMillis()
            val result1 = dataSource.getGrib1Bytes(targetTime)
            val (grid1, msgs1) = Grib1Decoder.index(result1.bytes)
            grib1Bytes = result1.bytes
            grib1Grid = grid1
            grib1Messages = msgs1
            loadedGrib1Time = result1.actualTime
            Log.d(TAG, "GRIB1 ready: ${msgs1.size} messages for ${result1.actualTime} in ${System.currentTimeMillis() - dlStart1}ms")

            val dlStart2 = System.currentTimeMillis()
            val result2 = dataSource.getGrib2File(targetTime)
            grib2File = result2.file
            grib2Ready = true
            loadedGrib2Time = result2.actualTime
            Log.d(TAG, "GRIB2 ready: file=${result2.file.name} for ${result2.actualTime} in ${System.currentTimeMillis() - dlStart2}ms")

            _state.value = GribState.Ready
        } catch (e: Exception) {
            Log.e(TAG, "GRIB initialization failed", e)
            _state.value = GribState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun initializeForTimeIfNeeded(targetTime: String) = withContext(Dispatchers.IO) {
        val targetRounded = roundToNearest3Hours(targetTime)
        val currentRounded = loadedGrib2Time?.let { roundToNearest3Hours(it) }
            ?: loadedGrib1Time?.let { roundToNearest3Hours(it) }

        if (currentRounded == targetRounded) {
            Log.d(TAG, "GRIB already loaded for closest 3-hour slot $targetRounded, skipping")
            return@withContext
        }

        Log.d(TAG, "GRIB time changed: current=$currentRounded, target=$targetRounded, reinitializing")
        initialize(targetTime)
    }

    private fun roundToNearest3Hours(timeString: String): String {
        return try {
            val hour = timeString.substringAfter("T").take(2).toInt()
            val roundedHour = ((hour + 1) / 3) * 3
            val datePart = timeString.substringBefore("T")
            if (roundedHour >= 24) {
                "${datePart}T00:00:00Z"
            } else {
                "${datePart}T${String.format("%02d", roundedHour)}:00:00Z"
            }
        } catch (e: Exception) {
            timeString
        }
    }

    suspend fun getAtmosphericProfile(lat: Double, lon: Double): List<AtmosphericLayer> {
        val useGrib2 = shouldUseGrib2(lat, lon)

        val gribLayers = if (useGrib2) {
            extractFromGrib2(lat, lon)
        } else {
            extractFromGrib1(lat, lon)
        }

        val groundLayer = getGroundLayer(lat, lon)
        val allLayers = if (groundLayer != null) {
            listOf(groundLayer) + gribLayers
        } else {
            gribLayers
        }
        return allLayers.sortedBy { it.altitudeM }
    }

    private fun shouldUseGrib2(lat: Double, lon: Double): Boolean {
        val inside = lat in GRIB2_LAT_MIN..GRIB2_LAT_MAX && lon in GRIB2_LON_MIN..GRIB2_LON_MAX
        val ready = grib2Ready && grib2File != null
        return inside && ready
    }

    private fun extractFromGrib2(lat: Double, lon: Double): List<AtmosphericLayer> {
        val file = grib2File ?: error("GRIB2 file not loaded")

        val records = grib2Region ?: run {
            val r = Grib2Decoder.decodeRegion(file.absolutePath, lat, lon, patchRadius = 1)
            grib2Region = r
            Log.d(TAG, "GRIB2 region decoded: ${r.size} records")
            r
        }

        val gridPoints = records.map { Pair(it.lat, it.lon) }.distinct()
        val (nearLat, nearLon) = gridPoints.minBy { (glat, glon) ->
            val dlat = glat - lat
            val dlon = glon - lon
            dlat * dlat + dlon * dlon
        }

        val byLevel = mutableMapOf<Int, MutableMap<GribParam, Double>>()
        for (record in records) {
            if (record.lat != nearLat || record.lon != nearLon) continue
            val param = grib2ParamToEnum(record.paramCategory, record.paramNumber)
            if (param == GribParam.UNKNOWN) continue
            byLevel.getOrPut(record.levelHpa) { mutableMapOf() }[param] = record.value
        }

        val layers = byLevel.mapNotNull { (hpa, params) ->
            val u = params[GribParam.U_WIND] ?: return@mapNotNull null
            val v = params[GribParam.V_WIND] ?: return@mapNotNull null
            val t = params[GribParam.TEMPERATURE] ?: return@mapNotNull null

            val pressurePa = hpa * 100.0
            val alt = pressureToAltitude(pressurePa)
            val density = pressurePa / (Rd * t)

            AtmosphericLayer(hpa, alt, u, v, t, density,)
        }.sortedBy { it.altitudeM }

        return layers
    }

    private fun extractFromGrib1(lat: Double, lon: Double): List<AtmosphericLayer> {
        val grid = grib1Grid ?: error("GRIB1 not loaded")
        val messages = grib1Messages ?: error("GRIB1 not indexed")
        val bytes = grib1Bytes ?: error("GRIB1 not loaded")

        val gridIdx = Grib1Decoder.gridIndex(grid, lat, lon)

        val byLevel = mutableMapOf<Int, MutableMap<GribParam, Double>>()
        for (msg in messages) {
            val param = grib1ParamToEnum(msg.paramNumber)
            if (param == GribParam.UNKNOWN) continue
            val value = Grib1Decoder.extractValue(bytes, msg, gridIdx)
            byLevel.getOrPut(msg.pressureLevelHpa) { mutableMapOf() }[param] = value
        }

        return byLevel.mapNotNull { (hpa, params) ->
            val u = params[GribParam.U_WIND] ?: return@mapNotNull null
            val v = params[GribParam.V_WIND] ?: return@mapNotNull null
            val t = params[GribParam.TEMPERATURE] ?: return@mapNotNull null
            val pressurePa = hpa * 100.0
            val alt = pressureToAltitude(pressurePa)
            val density = pressurePa / (Rd * t)
            AtmosphericLayer(hpa, alt, u, v, t, density,)
        }.sortedBy { it.altitudeM }
    }

    private suspend fun getGroundLayer(lat: Double, lon: Double): AtmosphericLayer? {
        return try {
            val forecasts = locationForecastRepo.getData(lat, lon)
            val entry = forecasts.firstOrNull() ?: return null
            forecastEntryToLayer(entry)
        } catch (e: Exception) {
            null
        }
    }

    private fun forecastEntryToLayer(entry: ForecastEntry): AtmosphericLayer? {
        val windSpeed = entry.wind_speed ?: return null
        val windDir = entry.wind_from_direction ?: return null
        val temp = entry.air_temperature ?: return null
        val pressure = entry.air_pressure_at_sea_level ?: return null

        val dirRad = Math.toRadians(windDir)
        val u = -windSpeed * sin(dirRad)
        val v = -windSpeed * cos(dirRad)

        val tempK = temp + 273.15
        val pressurePa = pressure * 100.0
        val density = pressurePa / (Rd * tempK)

        return AtmosphericLayer(
            pressureHpa = pressure.toInt(),
            altitudeM = 0.0,
            windU = u,
            windV = v,
            temperatureK = tempK,
            airDensity = density,
        )
    }

    companion object {
        private const val TAG = "testing"

        // Southern Norway GRIB2 coverage (from isobaricgrib/1.0 API docs)
        private const val GRIB2_LAT_MIN = 55.35
        private const val GRIB2_LAT_MAX = 64.25
        private const val GRIB2_LON_MIN = -1.45
        private const val GRIB2_LON_MAX = 14.51
        fun pressureToAltitude(pressurePa: Double): Double {
            val P0 = 101325.0
            val T0 = 288.15
            val g = 9.80665
            val L = 0.0065
            return (T0 / L) * (1.0 - Math.pow(pressurePa / P0, (Rd * L) / g))
        }
    }
}