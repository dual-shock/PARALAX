package com.dual_shock.paralax.model.settings

import kotlinx.serialization.Serializable

// changed the limits to understand why the timewindows were all returning with 0 percent
@Serializable
data class LaunchWindowLimits(
    val maxWindSpeedGround: Double = 25.0,       // m/s - NREQ-2
    val maxWindSpeedAltitude: Double = 45.0,    // m/s - NREQ-3
    val maxShearWind: Double = 45.0,            // m/s - NREQ-4
    val maxCloudCoverMediumHigh: Double = 45.0, // % - NREQ-5
    val maxCloudCoverLow: Double = 20.0,         // % - NREQ-5
    val maxFogCover: Double = 0.0,              // % - NREQ-6
    val maxPrecipitation: Double = 0.0,         // mm, 0 unless recent rain - NREQ-7
    val maxHumidity: Double = 75.0,             //  % - NREQ-8
    val maxDewPoint: Double = 15.0,             // celsius - NREQ-9
    val minAirPressure: Double = 990.0,         // unchanged, not in table
    val minTemperature: Double = -10.0,         // unchanged, not in table
    val maxTemperature: Double = 35.0,          // unchanged, not in table
    val maxAltitude: Double = 5000.0,          // m - Default without permit from Luftfartstilsynet it was 10000
    val dangerZoneRadiusM: Double = 500.0      // meters - danger zone radius around landing point
)