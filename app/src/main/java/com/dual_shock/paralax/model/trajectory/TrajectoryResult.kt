package com.dual_shock.paralax.model.trajectory

data class TrajectoryResult(
    val points: List<TrajectoryPoint>,
    val apogeeM: Double,
    val maxSpeedMs: Double,
)
