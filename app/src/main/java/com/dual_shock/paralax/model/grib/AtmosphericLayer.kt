package com.dual_shock.paralax.model.grib

data class AtmosphericLayer(
    val pressureHpa: Int,
    val altitudeM: Double,
    val windU: Double,
    val windV: Double,
    val temperatureK: Double,
    val airDensity: Double
)
