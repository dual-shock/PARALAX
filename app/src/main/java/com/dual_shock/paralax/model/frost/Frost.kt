package com.dual_shock.paralax.model.frost

import kotlinx.serialization.Serializable


@Serializable
data class Lightning(
    val Epoch: String,
    val Point: List<Double>,
    val CloudIndicator: Int,
    val PeakCurrentEstimate: Int
){
    val longitude: Double get() = Point[0]
    val latitude: Double get() = Point[1]
}