package com.dual_shock.paralax.data.interpol

import kotlinx.serialization.Serializable

@Serializable
data class InterpolResponse(
    val coverages: List<Coverage>
)

@Serializable
data class Coverage(
    val domain: Domain,
    val ranges: Ranges
)

@Serializable
data class Domain(
    val axes: Axes
)

@Serializable
data class Axes(
    val x: AxisValues,
    val y: AxisValues,
    val t: AxisValuesString,
    val z: AxisValues
)

@Serializable
data class AxisValues(
    val values: List<Double>
)

@Serializable
data class AxisValuesString(
    val values: List<String>
)



@Serializable
data class Ranges(
    val air_pressure_ml: NdArray,
    val air_temperature_ml: NdArray,
    val dewpoint_calculated_ml: NdArray,
    val wind_direction_ml: NdArray,
    val wind_speed_ml: NdArray
)

@Serializable
data class NdArray(
    val values: List<Double>
)


