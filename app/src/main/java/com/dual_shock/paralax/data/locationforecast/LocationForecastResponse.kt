package com.dual_shock.paralax.data.locationforecast

import kotlinx.serialization.Serializable

@Serializable
data class LocationForecastResponse(
    val geometry: Geometry,
    val properties: Properties
)

@Serializable
data class Geometry(
    val coordinates: List<Double> // gives you altitude as well as x, y
)

@Serializable
data class Properties(
    val timeseries: List<Timeseries>
)

@Serializable
data class Timeseries(
    val time: String,
    val data: ForecastData
)

@Serializable
data class ForecastData(
    val instant: InstantData,  // changed: separate class for instant
    val next_1_hours: NextHours? = null,
    val next_6_hours: NextHours? = null,
    val next_12_hours: NextHours? = null
)

// instant has no summary field
@Serializable
data class InstantData(
    val details: Details  // reuse the same Details class
)

@Serializable
data class NextHours(
    val summary: Summary,
    val details: Details
)

@Serializable
data class Summary(
    val symbol_code: String
)

@Serializable
data class Details(
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


