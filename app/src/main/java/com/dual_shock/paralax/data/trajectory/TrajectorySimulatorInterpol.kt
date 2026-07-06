package com.dual_shock.paralax.data.trajectory

import android.util.Log
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.model.interpol.InterpolUnits
import com.dual_shock.paralax.model.trajectory.TrajectoryPoint
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "testing"



class TrajectorySimulator(
    rocketConfig: RocketConfig,
    val site: LaunchSite,
    val levels: List<InterpolUnits>,
    val timeStep: Double = 0.1 // one tenth of a second interval
) {
    var speedX = 0.0
    var speedY = 0.0
    var speedZ = 0.0

    var x = 0.0
    var y = 0.0
    var z = 0.0

    var windX = 0.0
    var windY = 0.0
    val dragCoefficient = rocketConfig.dragCoefficient
    val diameter = rocketConfig.diameter

    val burnRate = rocketConfig.burnTime

    val fuelMass = rocketConfig.fuelMass

    val thrust = rocketConfig.thrust
    val directionRad = Math.toRadians(rocketConfig.direction)
    val launchAngleRad = Math.toRadians(rocketConfig.launchAngle)
    val launchLatRad = Math.toRadians(site.lat)

    var currentTime = 0.0
    var currentMass = rocketConfig.dryMass + rocketConfig.fuelMass


    var density = 0.0

    var lastZ = 0.0
    var passedApogee = false
    var firstChuteDeployed = false
    var secondChuteDeployed = false

    val firstChuteReleaseHeight = rocketConfig.firstChuteReleaseHeight
    val firstChuteTerminalVelocity = rocketConfig.firstChuteTerminalVelocity
    val firstChuteAtApogee = rocketConfig.firstChuteAtApogee
    val secondChuteReleaseHeight = rocketConfig.secondChuteReleaseHeight
    val secondChuteTerminalVelocity = rocketConfig.secondChuteTerminalVelocity

    fun pressureToAltitude(pressure_hpa: Double): Double {
        return 44330 * (1 - (pressure_hpa / 1013.25).pow(1.0 / 5.255)) //standard equation
    }

    fun findClosestLevel(levels: List<InterpolUnits>, currentAltitude: Double): InterpolUnits {
        return levels.minByOrNull { level ->
            val alt = pressureToAltitude(level.air_pressure_ml ?: 1013.25)
            abs(alt - currentAltitude)
        } ?: levels.last()
    }

    fun doStep(interpolUnit: InterpolUnits): TrajectoryPoint {
        // wind components from speed and direction
        windX = -(interpolUnit.wind_speed_ml ?: 0.0) * sin(Math.toRadians(interpolUnit.wind_direction_ml ?: 0.0))
        windY = -(interpolUnit.wind_speed_ml ?: 0.0) * cos(Math.toRadians(interpolUnit.wind_direction_ml ?: 0.0))

        // calculating air density from pressure and temp
        val pressure_pa = (interpolUnit.air_pressure_ml ?: 0.0) * 100
        val temperature_k = (interpolUnit.air_temperature_ml ?: 0.0) + 273.15
        val density = pressure_pa / (287.05 * temperature_k)

        if (currentTime < 0.2) {
            Log.d(TAG, "Interpol doStep: pressure=${interpolUnit.air_pressure_ml}, temp=${interpolUnit.air_temperature_ml}, density=$density, windSpeed=${interpolUnit.wind_speed_ml}, windDir=${interpolUnit.wind_direction_ml}")
        }

        // speed minus wind parameters
        val relVx = speedX - windX
        val relVy = speedY - windY
        val relVz = speedZ

        // drag
        val (dragX, dragY, dragZ) = calculateDrag(relVx, relVy, relVz, density, dragCoefficient, diameter)

        // thrust, check if fuel is still burning
        val isBurning = currentTime < burnRate
        val thrustX = if (isBurning) thrust * cos(launchAngleRad) * sin(directionRad) else 0.0
        val thrustY = if (isBurning) thrust * cos(launchAngleRad) * cos(directionRad) else 0.0
        val thrustZ = if (isBurning) thrust * sin(launchAngleRad) else 0.0

        // gravity
        val gravityZ = -9.81 * currentMass

        // acceleration
        val ax = (thrustX + dragX) / currentMass
        val ay = (thrustY + dragY) / currentMass
        val az = (thrustZ + dragZ + gravityZ) / currentMass

        // parachute deployment logic
        if (!passedApogee && z < lastZ && currentTime > 0.5) {
            passedApogee = true
            Log.d(TAG, "Apogee reached at z=${z.toInt()}m, t=${currentTime}s")
            if (firstChuteAtApogee && firstChuteTerminalVelocity > 0) {
                firstChuteDeployed = true
                Log.d(TAG, "First chute deployed at apogee")
            }
        }

        if (passedApogee) {
            if (!firstChuteDeployed && !firstChuteAtApogee && firstChuteTerminalVelocity > 0 && z <= firstChuteReleaseHeight) {
                firstChuteDeployed = true
                Log.d(TAG, "First chute deployed at z=${z.toInt()}m")
            }
            if (!secondChuteDeployed && secondChuteTerminalVelocity > 0 && z <= secondChuteReleaseHeight) {
                secondChuteDeployed = true
                Log.d(TAG, "Second chute deployed at z=${z.toInt()}m")
            }
        }

        val activeTerminalVelocity = when {
            secondChuteDeployed -> secondChuteTerminalVelocity
            firstChuteDeployed -> firstChuteTerminalVelocity
            else -> null
        }

        // update velocity - either from parachute terminal velocity or physics
        if (activeTerminalVelocity != null) {
            speedX = windX
            speedY = windY
            speedZ = -activeTerminalVelocity
        } else {
            speedX += ax * timeStep
            speedY += ay * timeStep
            speedZ += az * timeStep
        }

        lastZ = z

        // update position
        x += speedX * timeStep
        y += speedY * timeStep
        z += speedZ * timeStep

        // burn fuel
        if (isBurning) {
            val massFlowRate = fuelMass / burnRate
            currentMass -= massFlowRate * timeStep
        }

        // convert to lat/lon
        val lat = site.lat + (y / 111320) // convert meters to degrees equation
        val lon = site.lon + (x / (111320 * cos(launchLatRad))) //convert meters to degrees for lon

        val currentSpeed = sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ)


        return TrajectoryPoint(lat, lon, z, currentTime, currentSpeed)
    }

    fun doCalculation(): List<TrajectoryPoint> {
        Log.d(TAG, "Interpol doCalculation: ${levels.size} levels provided")
        Log.d(TAG, "  Parachutes: 1st atApogee=$firstChuteAtApogee, height=$firstChuteReleaseHeight, vel=$firstChuteTerminalVelocity | 2nd height=$secondChuteReleaseHeight, vel=$secondChuteTerminalVelocity")
        levels.take(3).forEachIndexed { i, lvl ->
            Log.d(TAG, "  Level[$i]: pressure=${lvl.air_pressure_ml}, temp=${lvl.air_temperature_ml}, wind=${lvl.wind_speed_ml}")
        }

        val points = mutableListOf<TrajectoryPoint>()
        while (z >= 0.0) {
            val level = findClosestLevel(levels, z)
            points.add(doStep(level))
            currentTime += timeStep
        }
        Log.d(TAG, "Interpol simulation complete: ${points.size} points, apogee=${points.maxOfOrNull { it.altitude }?.toInt()}m")
        return points
    }

    fun calculateDrag(
        speedX: Double,
        speedY: Double,
        speedZ: Double,
        airDensity: Double,
        dragCoefficient: Double,
        diameter_m: Double
    ): Triple<Double, Double, Double> {
        val speed = sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ)
        if (speed == 0.0) return Triple(0.0, 0.0, 0.0)
        val crossSection = PI * (diameter_m / 2).pow(2)
        val dragMagnitude = 0.5 * airDensity * speed * speed * dragCoefficient * crossSection
        return Triple( //better suited for 3D modelling
            -dragMagnitude * speedX / speed,
            -dragMagnitude * speedY / speed,
            -dragMagnitude * speedZ / speed
        )
    }
}






