package com.dual_shock.paralax.model.hour

import com.dual_shock.paralax.model.interpol.InterpolUnits
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.ui.dashboard.Layer

data class HourData(
    val utcTimestamp: String,
    val hour: Int,
    val localHour: Int,
    val dayOffset: Int,
    val forecastEntry: ForecastEntry?,
    val interpolUnits: List<InterpolUnits>?,
    val altWindSpeed: Double?,
    val maxShear: Double?,
    val viabilityScore: Int,
    val specifics: List<Pair<String, Double>> = emptyList(),
    val gribSlotTimestamp: String,
    val atmosphericLayers: List<Layer>?,
    val shearLayers: List<Layer>?,
    val isWatched: Boolean,
    val hasChangedSinceLastCheck: Boolean,
    val lightningStrikeCount: Int? = null
) {
    companion object {
        fun empty(
            utcTimestamp: String,
            hour: Int,
            localHour: Int,
            dayOffset: Int
        ) = HourData(
            utcTimestamp = utcTimestamp,
            hour = hour,
            localHour = localHour,
            dayOffset = dayOffset,
            forecastEntry = null,
            interpolUnits = null,
            altWindSpeed = null,
            maxShear = null,
            viabilityScore = 0,
            gribSlotTimestamp = roundToNearest3Hours(utcTimestamp),
            atmosphericLayers = null,
            shearLayers = null,
            isWatched = false,
            hasChangedSinceLastCheck = false
        )

        fun roundToNearest3Hours(timestamp: String): String {
            return try {
                val hour = timestamp.substringAfter("T").take(2).toInt()
                val roundedHour = ((hour + 1) / 3) * 3
                val datePart = timestamp.substringBefore("T")
                if (roundedHour >= 24) {
                    "${datePart}T00:00:00Z"
                } else {
                    "${datePart}T${String.format("%02d", roundedHour)}:00:00Z"
                }
            } catch (e: Exception) {
                timestamp
            }
        }
    }
}
