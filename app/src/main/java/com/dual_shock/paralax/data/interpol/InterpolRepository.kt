package com.dual_shock.paralax.data.interpol

import kotlinx.coroutines.flow.first
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.model.interpol.InterpolUnits


class InterpolRepository(private val interpolData: InterpolDataSource, private val interpolDao: InterpolDao){
    suspend fun getData(location: String, time: String): List<InterpolUnits> {
        val raw = interpolData.getData(location)
        println("DEBUG: looking for time = $time")
        println("DEBUG: available times = ${raw.coverages.map { it.domain.axes.t.values[0] }}")


        val coverage = raw.coverages.firstOrNull { coverage ->
            coverage.domain.axes.t.values[0].take(13) == time.take(13) // hour from screen trajectory includes seconds so we match based on the 13 first characters only
        } ?: throw Exception("No coverage found for time: $time")



        val ranges = coverage.ranges
        println("ranges ${ranges.air_pressure_ml.values}")



        return ranges.air_pressure_ml.values.indices.map { i ->
            InterpolUnits(
                air_pressure_ml = ranges.air_pressure_ml.values[i].takeIf { it > 0 },
                air_temperature_ml = ranges.air_temperature_ml.values[i].takeIf { it > -9000 },
                dewpoint_calculated_ml = ranges.dewpoint_calculated_ml.values[i].takeIf { it > -9000 && it < 9000 },
                wind_direction_ml = ranges.wind_direction_ml.values[i].takeIf { it > -9000 },
                wind_speed_ml = ranges.wind_speed_ml.values[i].takeIf { it >= 0 && it < 1000 }
            )
        }
    }
    suspend fun getAllData(location: String): Map<String, List<InterpolUnits>> {
        val raw = interpolData.getData(location)
        return raw.coverages.associate { coverage ->
            val time = coverage.domain.axes.t.values[0]
            val ranges = coverage.ranges
            val units = ranges.air_pressure_ml.values.indices.map { i ->
                InterpolUnits(
                    air_pressure_ml = ranges.air_pressure_ml.values[i].takeIf { it > 0 },
                    air_temperature_ml = ranges.air_temperature_ml.values[i].takeIf { it > -9000 },
                    dewpoint_calculated_ml = ranges.dewpoint_calculated_ml.values[i].takeIf { it > -9000 && it < 9000 },
                    wind_direction_ml = ranges.wind_direction_ml.values[i].takeIf { it > -9000 },
                    wind_speed_ml = ranges.wind_speed_ml.values[i].takeIf { it >= 0 && it < 1000 }
                )
            }
            time to units
        }
    }


    suspend fun getLocations() {

        val cached = interpolDao.getInterpolLocations().first()
        if (cached.isNotEmpty()) return //check if the table is already populated

        val raw = interpolData.getLocations()
        val features = raw.features

        val locations = features.map { i ->
            InterpolLocation(
                id  = 0,
             name = i.id,
             lon = i.geometry.coordinates[0],
             lat = i.geometry.coordinates[1],
            )
            }
        interpolDao.insertAll(locations)

    }

    suspend fun linkClosestInterpolLocation(site: LaunchSite, delta: Double): InterpolLocation? {
        return interpolDao.getClosestInterpolLocation(site.lat, site.lon, delta)
    }
}