package com.dual_shock.paralax.model.hour

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class HourDataSnapshot(
    val utcTimestamp: String,
    val hour: Int,
    val localHour: Int,
    val dayOffset: Int,

    // Ground forecast values (flattened from ForecastEntry)
    val windSpeed: Double?,
    val windDirection: Double?,
    val airTemperature: Double?,
    val precipitation: Double?,
    val cloudCoverHigh: Double?,
    val cloudCoverMedium: Double?,
    val cloudCoverLow: Double?,
    val fogFraction: Double?,
    val humidity: Double?,
    val dewPoint: Double?,
    val pressure: Double?,
    val lightningStrikeCount: Int?,

    // Interpol-derived altitude metrics
    val altWindSpeed: Double?,
    val maxShear: Double?,

    // Computed score
    val viabilityScore: Int
) {
    fun hasSignificantChanges(current: HourData): Boolean {
        val scoreChanged = abs(viabilityScore - current.viabilityScore) > 10
        val windChanged = abs((windSpeed ?: 0.0) - (current.forecastEntry?.wind_speed ?: 0.0)) > 3.0
        val altWindChanged = abs((altWindSpeed ?: 0.0) - (current.altWindSpeed ?: 0.0)) > 5.0
        val precipAppeared = (precipitation ?: 0.0) == 0.0 &&
            (current.forecastEntry?.precipitation_amount ?: 0.0) > 0.0
        val precipDisappeared = (precipitation ?: 0.0) > 0.0 &&
            (current.forecastEntry?.precipitation_amount ?: 0.0) == 0.0
        val lightningStrikeAppeared = (lightningStrikeCount == null || lightningStrikeCount == 0) &&
                (current.lightningStrikeCount ?: 0) > 0

        return scoreChanged || windChanged || altWindChanged || precipAppeared || precipDisappeared || lightningStrikeAppeared
    }
}

fun HourData.toSnapshot(): HourDataSnapshot = HourDataSnapshot(
    utcTimestamp = utcTimestamp,
    hour = hour,
    localHour = localHour,
    dayOffset = dayOffset,
    windSpeed = forecastEntry?.wind_speed,
    windDirection = forecastEntry?.wind_from_direction,
    airTemperature = forecastEntry?.air_temperature,
    precipitation = forecastEntry?.precipitation_amount,
    cloudCoverHigh = forecastEntry?.cloud_area_fraction_high,
    cloudCoverMedium = forecastEntry?.cloud_area_fraction_medium,
    cloudCoverLow = forecastEntry?.cloud_area_fraction_low,
    fogFraction = forecastEntry?.fog_area_fraction,
    humidity = forecastEntry?.relative_humidity,
    dewPoint = forecastEntry?.dew_point_temperature,
    pressure = forecastEntry?.air_pressure_at_sea_level,
    altWindSpeed = altWindSpeed,
    maxShear = maxShear,
    viabilityScore = viabilityScore,
    lightningStrikeCount = lightningStrikeCount
)
