package com.dual_shock.paralax.ui.dashboard

import com.dual_shock.paralax.model.grib.AtmosphericLayer
import com.dual_shock.paralax.model.interpol.InterpolUnits
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Layer(
    val windDirection: Double,
    val windSpeed: Double,
    val altitude: Double
)

object AtmosphericLayerMapper {

    fun mappingBasedonHpa(
        interpol: List<InterpolUnits>,
        grib: List<AtmosphericLayer>
    ): List<Layer> {
        val interpolByPressure = interpol
            .filter { it.air_pressure_ml != null }
            .associateBy { it.air_pressure_ml!! }

        return grib.map { layer ->
            val nearestInterpol = interpolByPressure.minByOrNull { (hpa, _) ->
                abs(hpa - layer.pressureHpa)
            }?.value

            nearestInterpol?.let { blend(layer, it) } ?: Layer(
                windSpeed = sqrt(layer.windU.pow(2) + layer.windV.pow(2)),
                windDirection = (Math.toDegrees(atan2(-layer.windU, -layer.windV)) + 360) % 360,
                altitude = layer.altitudeM
            )
        }
    }

      fun blend(layer: AtmosphericLayer, interpol: InterpolUnits): Layer {
        val speed = interpol.wind_speed_ml
        val dir = interpol.wind_direction_ml

        val interpolU = if (speed != null && dir != null && speed >= 0 && speed < 1000) -speed * sin(Math.toRadians(dir)) else null
        val interpolV = if (speed != null && dir != null && speed >= 0 && speed < 1000) -speed * cos(Math.toRadians(dir)) else null

        val blendedU = if (interpolU != null) (layer.windU + interpolU) / 2.0 else layer.windU
        val blendedV = if (interpolV != null) (layer.windV + interpolV) / 2.0 else layer.windV

        return Layer(
            windSpeed = sqrt(blendedU.pow(2) + blendedV.pow(2)),
            windDirection = (Math.toDegrees(atan2(-blendedU, -blendedV)) + 360) % 360,
            altitude = layer.altitudeM
        )
    }
     fun shearBetweenLayers(layers: List<Layer>): List<Layer> {
        return layers.zipWithNext { lower, upper ->  // zip with next eturns n-1 layers
            val midAltitude = (lower.altitude + upper.altitude) / 2.0
            val lowerU = -lower.windSpeed * sin(Math.toRadians(lower.windDirection)) // reparamatrizes here  to get the shear
            val lowerV = -lower.windSpeed * cos(Math.toRadians(lower.windDirection))
            val upperU = -upper.windSpeed * sin(Math.toRadians(upper.windDirection))
            val upperV = -upper.windSpeed * cos(Math.toRadians(upper.windDirection))

            val midU = (lowerU + upperU) / 2.0
            val midV = (lowerV + upperV) / 2.0

            Layer(
                windSpeed = sqrt(midU.pow(2) + midV.pow(2)),
                windDirection = (Math.toDegrees(atan2(-midU, -midV)) + 360) % 360,
                altitude = midAltitude
            )
        }
    }
}