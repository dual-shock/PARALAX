package com.dual_shock.paralax.ui.dashboard

import com.dual_shock.paralax.model.grib.AtmosphericLayer
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.model.settings.LaunchWindowLimits
import com.dual_shock.paralax.ui.dashboard.WindDirectionScorer.calculateDirectionalScore
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
        atmosphericLayer: List<Layer>?,
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
        println("keyframes at score time: ${settings.keyframes.size}")

        val directionScore = atmosphericLayer?.let { layers ->
            calculateDirectionalScore(layers, settings.keyframes)
        } ?: 0.5
        println("keyframes at score time: ${settings.keyframes.size}")


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
            "Direction"  to directionScore,
            "Wind Lo"    to groundWindScore,
            "Wind Hi"    to altWindScore,
            "Wind Shr"   to shearScore,
            "Fog"        to fogScore,
            "Hum"        to humidityScore,
            "Dew Point"  to dewPointScore,
            "Cloud hi"   to cloudHighScore,
            "Cloud me"   to cloudMedScore,
            "Cloud lo"   to cloudLowScore,
            "Temp"       to temperatureScore,
            "Prs"        to pressureScore,
            "Wind dir"   to directionScore
        )

        val score = ((
                // Wind direction is the most important factor at 30%
                        directionScore   * 0.30 +

                        // Wind speeds matter less
                        groundWindScore  * 0.10 +  // 10% for ground wind speed
                        altWindScore     * 0.10 +  // 10% for altitude wind speed
                        shearScore       * 0.08 +  //  8% for wind shear

                        // Other weather factors share the remaining 42%
                        cloudHighScore   * 0.07 +
                        cloudMedScore    * 0.06 +
                        cloudLowScore    * 0.04 +
                        fogScore         * 0.09 +
                        humidityScore    * 0.05 +
                        dewPointScore    * 0.04 +
                        temperatureScore * 0.04 +
                        pressureScore    * 0.03
                // Total = 100%
                ) * 100.0).roundToInt()

        return ViabilityResult(score, specifics)
    }
}


