package com.dual_shock.paralax.data.trajectory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dual_shock.paralax.data.grib.GribRepository
import com.dual_shock.paralax.model.grib.AtmosphericLayer
import com.dual_shock.paralax.model.trajectory.TrajectoryPoint
import com.dual_shock.paralax.model.trajectory.TrajectoryResult
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class TrajectorySimulatorGrib(
    private val gribRepository: GribRepository,
    private val rocketConfig: RocketConfig
) {
    val gribState = gribRepository.state

    companion object {
        const val TAG = "testing"
    }


    private var atmosphericGrid: Map<Pair<Double, Double>, List<AtmosphericLayer>>? = null
    private var gridPoints: List<Pair<Double, Double>> = emptyList()

    suspend fun calculate(
        lat: Double,
        lon: Double,
        config: RocketConfig
    ): TrajectoryResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "GribTrajectoryCalculator.calculate at ($lat, $lon)")
        Log.d(TAG, "  Rocket: ${config.dryMass}kg dry, ${config.fuelMass}kg fuel, ${config.thrust}N thrust")
        Log.d(TAG, "  Parachutes: 1st atApogee=${config.firstChuteAtApogee}, height=${config.firstChuteReleaseHeight}, vel=${config.firstChuteTerminalVelocity} | 2nd height=${config.secondChuteReleaseHeight}, vel=${config.secondChuteTerminalVelocity}")

        buildAtmosphericGrid(lat, lon)

        val points = runSimulation(lat, lon, config)

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Trajectory calculation took ${elapsed}ms, ${points.size} points")

        TrajectoryResult(
            points = points,
            apogeeM = points.maxOfOrNull { it.altitude } ?: 0.0,
            maxSpeedMs = points.maxOfOrNull { it.speed } ?: 0.0
        )
    }

    private suspend fun buildAtmosphericGrid(centerLat: Double, centerLon: Double) {
        val grid = mutableMapOf<Pair<Double, Double>, List<AtmosphericLayer>>()

        val centerProfile = gribRepository.getAtmosphericProfile(centerLat, centerLon)
        grid[Pair(centerLat, centerLon)] = centerProfile
        Log.d(TAG, "Center profile: ${centerProfile.size} layers")

        val step = 0.1
        for (dLat in -1..1) {
            for (dLon in -1..1) {
                if (dLat == 0 && dLon == 0) continue
                val pLat = centerLat + dLat * step
                val pLon = centerLon + dLon * step
                try {
                    val profile = gribRepository.getAtmosphericProfile(pLat, pLon)
                    if (profile.isNotEmpty()) {
                        grid[Pair(pLat, pLon)] = profile
                    }
                } catch (e: Exception) {
                }
            }
        }

        atmosphericGrid = grid
        gridPoints = grid.keys.toList()
        Log.d(TAG, "Built atmospheric grid with ${grid.size} points")
    }

    private fun runSimulation(
        startLat: Double,
        startLon: Double,
        config: RocketConfig
    ): List<TrajectoryPoint> {
        val timeStep = 0.1
        val points = mutableListOf<TrajectoryPoint>()

        var x = 0.0
        var y = 0.0
        var z = 0.0
        var lastZ = 0.0

        var vx = 0.0
        var vy = 0.0
        var vz = 0.0

        var t = 0.0
        var mass = config.dryMass + config.fuelMass

        var passedApogee = false
        var firstChuteDeployed = false
        var secondChuteDeployed = false

        val launchLatRad = Math.toRadians(startLat)
        val launchAngleRad = Math.toRadians(config.launchAngle)
        val directionRad = Math.toRadians(config.direction)
        val crossSection = PI * (config.diameter / 2).pow(2)

        points.add(TrajectoryPoint(startLat, startLon, 0.0, 0.0, 0.0))

        var stepCount = 0
        var loggedOnce = false
        while (z >= 0.0 || t < 0.5) {
            val currentLat = startLat + (y / 111320.0)
            val currentLon = startLon + (x / (111320.0 * cos(launchLatRad)))

            val layer = getAtmosphericLayerAt(currentLat, currentLon, z)

            val windU = layer?.windU ?: 0.0
            val windV = layer?.windV ?: 0.0
            val density = layer?.airDensity ?: 1.225

            if (!loggedOnce && t > 0.5) {
                Log.d(TAG, "Sample at t=${t}: z=${z.toInt()}m, layer=${layer?.pressureHpa}hPa, density=$density, windU=$windU, windV=$windV")
                loggedOnce = true
            }

            val relVx = vx - windU
            val relVy = vy - windV
            val relVz = vz
            val relSpeed = sqrt(relVx * relVx + relVy * relVy + relVz * relVz)

            val dragMag = if (relSpeed > 0) {
                0.5 * density * relSpeed * relSpeed * config.dragCoefficient * crossSection
            } else 0.0

            val dragX = if (relSpeed > 0) -dragMag * relVx / relSpeed else 0.0
            val dragY = if (relSpeed > 0) -dragMag * relVy / relSpeed else 0.0
            val dragZ = if (relSpeed > 0) -dragMag * relVz / relSpeed else 0.0

            val isBurning = t < config.burnTime
            val thrustX = if (isBurning) config.thrust * cos(launchAngleRad) * sin(directionRad) else 0.0
            val thrustY = if (isBurning) config.thrust * cos(launchAngleRad) * cos(directionRad) else 0.0
            val thrustZ = if (isBurning) config.thrust * sin(launchAngleRad) else 0.0

            val gravityZ = -9.81 * mass

            val ax = (thrustX + dragX) / mass
            val ay = (thrustY + dragY) / mass
            val az = (thrustZ + dragZ + gravityZ) / mass

            vx += ax * timeStep
            vy += ay * timeStep
            vz += az * timeStep

            x += vx * timeStep
            y += vy * timeStep
            z += vz * timeStep

            if (!passedApogee && z < lastZ && t > 0.5) {
                passedApogee = true
                Log.d(TAG, "Apogee reached at z=${z.toInt()}m, t=${t}s")
                if (config.firstChuteAtApogee) {
                    firstChuteDeployed = true
                    Log.d(TAG, "First chute deployed at apogee")
                }
            }

            if (passedApogee) {
                if (!firstChuteDeployed && !config.firstChuteAtApogee && config.firstChuteTerminalVelocity > 0  && z <= config.firstChuteReleaseHeight) {
                    firstChuteDeployed = true
                    Log.d(TAG, "First chute deployed at z=${z.toInt()}m")
                }
                if (!secondChuteDeployed && config.secondChuteTerminalVelocity > 0 && z <= config.secondChuteReleaseHeight) {
                    secondChuteDeployed = true
                    Log.d(TAG, "Second chute deployed at z=${z.toInt()}m")
                }
            }

            val activeTerminalVelocity = when {
                secondChuteDeployed -> config.secondChuteTerminalVelocity
                firstChuteDeployed -> config.firstChuteTerminalVelocity
                else -> null
            }

            if (activeTerminalVelocity != null) {
                vz = -activeTerminalVelocity
                vx = windU
                vy = windV
            }

            lastZ = z

            if (isBurning) {
                val massFlowRate = config.fuelMass / config.burnTime
                mass -= massFlowRate * timeStep
            }

            t += timeStep
            stepCount++

            val speed = sqrt(vx * vx + vy * vy + vz * vz)
            val lat = startLat + (y / 111320.0)
            val lon = startLon + (x / (111320.0 * cos(launchLatRad)))

            // Record every step for smooth rendering
            points.add(TrajectoryPoint(lat, lon, z.coerceAtLeast(0.0), t, speed))

            if (t > 600) break
        }

        Log.d(TAG, "Simulation complete: ${points.size} points, apogee=${points.maxOfOrNull { it.altitude }?.toInt()}m")
        return points
    }

    private fun getAtmosphericLayerAt(lat: Double, lon: Double, altitude: Double): AtmosphericLayer? {
        val grid = atmosphericGrid ?: return null
        if (grid.isEmpty()) return null

        val nearest = gridPoints.minByOrNull { (pLat, pLon) ->
            val dLat = pLat - lat
            val dLon = pLon - lon
            dLat * dLat + dLon * dLon
        } ?: return null

        val layers = grid[nearest] ?: return null
        if (layers.isEmpty()) return null

        return layers.minByOrNull { abs(it.altitudeM - altitude) }
    }
}