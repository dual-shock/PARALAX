package com.dual_shock.paralax.model.locationforecast

import kotlinx.serialization.Serializable

@Serializable
data class ForecastEntry(
    val time: String,
    val air_pressure_at_sea_level: Double? = null,
    val air_temperature: Double? = null,
    val cloud_area_fraction: Double? = null,
    val cloud_area_fraction_high: Double? = null,
    val cloud_area_fraction_low: Double? = null,
    val cloud_area_fraction_medium: Double? = null,
    val dew_point_temperature: Double? = null,
    val fog_area_fraction: Double? = null,
    val relative_humidity: Double? = null,
    val wind_from_direction: Double? = null,
    val wind_speed: Double? = null,
    val precipitation_amount: Double? = null
)
