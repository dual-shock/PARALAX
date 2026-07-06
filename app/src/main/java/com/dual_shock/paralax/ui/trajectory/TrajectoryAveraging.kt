package com.dual_shock.paralax.ui.trajectory

import com.dual_shock.paralax.model.trajectory.TrajectoryPoint
import kotlin.math.abs

fun averageTrajectoryPoints(
    interpolPoints: List<TrajectoryPoint>,
    gribPoints: List<TrajectoryPoint>
): List<TrajectoryPoint> {
    val count = minOf(interpolPoints.size, gribPoints.size)
    val tolerance = 1e-3

    return buildList {
        for (i in 0 until count) {
            val interpol = interpolPoints[i]
            val grib = gribPoints[i]

            if (abs(interpol.time - grib.time) <= tolerance) {
                add(
                    TrajectoryPoint(
                        time = (interpol.time + grib.time) / 2.0,
                        lat = (interpol.lat + grib.lat) / 2.0,
                        lon = (interpol.lon + grib.lon) / 2.0,
                        altitude = (interpol.altitude + grib.altitude) / 2.0,
                        speed = (interpol.speed + grib.speed) / 2.0
                    )
                )
            }
        }
    }
}
