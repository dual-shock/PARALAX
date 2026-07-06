package com.dual_shock.paralax.data.locationforecast

import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


class LocationForecastRepository(private val weatherData: LocationForecastDataSource){
    suspend fun getData(lat: Double, lon: Double): List<ForecastEntry> {
        val forecasts = weatherData.getForecast(lat, lon)
        val now = Clock.System.now()
        val cutoff = now.plus(72.hours)

        return forecasts.properties.timeseries
            .filter { entry ->
                val time = Instant.parse(entry.time)
                time > now && time < cutoff
            }
            .map { entry ->
                ForecastEntry(
                    time = entry.time,
                    air_pressure_at_sea_level = entry.data.instant.details.air_pressure_at_sea_level,
                    air_temperature = entry.data.instant.details.air_temperature,
                    cloud_area_fraction = entry.data.instant.details.cloud_area_fraction,
                    cloud_area_fraction_high = entry.data.instant.details.cloud_area_fraction_high,
                    cloud_area_fraction_low = entry.data.instant.details.cloud_area_fraction_low,
                    cloud_area_fraction_medium = entry.data.instant.details.cloud_area_fraction_medium,
                    dew_point_temperature = entry.data.instant.details.dew_point_temperature,
                    fog_area_fraction = entry.data.instant.details.fog_area_fraction,
                    relative_humidity = entry.data.instant.details.relative_humidity,
                    wind_from_direction = entry.data.instant.details.wind_from_direction,
                    wind_speed = entry.data.instant.details.wind_speed,
                    precipitation_amount = entry.data.next_1_hours?.details?.precipitation_amount
                )
            }


    }
}

// 2020-06-10T13:00:00Z" Time string includes seconds

