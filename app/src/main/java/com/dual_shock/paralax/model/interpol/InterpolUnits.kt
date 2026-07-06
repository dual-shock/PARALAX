package com.dual_shock.paralax.model.interpol

import kotlinx.serialization.Serializable

@Serializable
data class InterpolUnits(
    val air_pressure_ml: Double? = null,
    val air_temperature_ml: Double? = null,
    val dewpoint_calculated_ml: Double? = null,
    val wind_direction_ml: Double? = null,
    val wind_speed_ml: Double? = null
)
