package com.dual_shock.paralax.ui.dashboard

import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.model.settings.LaunchWindowLimits
import kotlin.math.abs
import kotlin.math.roundToInt

data class ViabilityResult(
    val score: Int? = null,
    val specifics: List<Pair<String, Double>>? = emptyList()
)
object ForecastMapper {
    fun calculatePercentage(
        entry: ForecastEntry,
        altWindSpeed: Double?,
        maxShear: Double?,
        settings: LaunchWindowLimits
    ): ViabilityResult {
        // Hard zeros
        println("calculatePercentage: altWind=$altWindSpeed maxShear=$maxShear maxWindAlt=${settings.maxWindSpeedAltitude} maxShearWind=${settings.maxShearWind} groundWind=${entry.wind_speed} maxGround=${settings.maxWindSpeedGround} precip=${entry.precipitation_amount} fog=${entry.fog_area_fraction} maxFog=${settings.maxFogCover}")

        altWindSpeed?.let { if (it > settings.maxWindSpeedAltitude) return ViabilityResult() }
        entry.wind_speed?.let { if (it > settings.maxWindSpeedGround) return ViabilityResult() }
        maxShear?.let { if (it > settings.maxShearWind) return ViabilityResult() }
        entry.fog_area_fraction?.let { if (it > settings.maxFogCover) return ViabilityResult() }
        entry.precipitation_amount?.let { if (it > settings.maxPrecipitation) return ViabilityResult() }

        fun scoreMax(value: Double?, max: Double): Double {
            if (value == null) return 0.5
            if (max == 0.0) return if (value == 0.0) 1.0 else 0.0
            return (1.0 - (value / max)).coerceIn(0.0, 1.0)
        }
        fun scoreMin(value: Double?, min: Double, absMax: Double): Double {
            if (value == null) return 0.5
            return ((value - min) / (absMax - min)).coerceIn(0.0, 1.0)
        }
        fun scoreMinMax(value: Double?, min: Double, max: Double): Double {
            if (value == null) return 0.5
            val mid = (min + max) / 2.0
            val range = (max - min) / 2.0
            return (1.0 - (abs(value - mid) / range)).coerceIn(0.0, 1.0)
        }

        val altWindScore     = scoreMax(altWindSpeed,                        settings.maxWindSpeedAltitude)
        val shearScore       = scoreMax(maxShear,                            settings.maxShearWind)
        val dewPointScore    = scoreMax(entry.dew_point_temperature,         settings.maxDewPoint)
        val groundWindScore  = scoreMax(entry.wind_speed,                    settings.maxWindSpeedGround)
        val cloudHighScore   = scoreMax(entry.cloud_area_fraction_high,      settings.maxCloudCoverMediumHigh)
        val cloudMedScore    = scoreMax(entry.cloud_area_fraction_medium,    settings.maxCloudCoverMediumHigh)
        val cloudLowScore    = scoreMax(entry.cloud_area_fraction_low,       settings.maxCloudCoverLow)
        val fogScore         = scoreMax(entry.fog_area_fraction,             settings.maxFogCover)
        val humidityScore    = scoreMax(entry.relative_humidity,             settings.maxHumidity)
        val temperatureScore = scoreMinMax(entry.air_temperature,            settings.minTemperature, settings.maxTemperature)
        val pressureScore    = scoreMin(entry.air_pressure_at_sea_level,     settings.minAirPressure, 1050.0)
        val specifics = listOf(
            "Wind Lo"  to groundWindScore,
                "Fog"      to fogScore,
            "Wind Hi"     to altWindScore,
                "Hum"     to humidityScore,
            "Wind Shr"   to shearScore,
                "Dew Point"    to dewPointScore,
            "Cloud hi"   to cloudHighScore,
                "Temp"  to temperatureScore,
            "Cloud me"    to cloudMedScore,
                "Prs"     to pressureScore,
            "Cloud lo"    to cloudLowScore,





        )

        val score = ((
                groundWindScore  * 0.20 +
                        altWindScore     * 0.15 +
                        shearScore       * 0.15 +
                        cloudHighScore   * 0.08 +
                        cloudMedScore    * 0.07 +
                        cloudLowScore    * 0.05 +
                        fogScore         * 0.10 +
                        humidityScore    * 0.05 +
                        dewPointScore    * 0.05 +
                        temperatureScore * 0.05 +
                        pressureScore    * 0.05
                ) * 100.0).roundToInt()

        return ViabilityResult(score, specifics)

           }
}


