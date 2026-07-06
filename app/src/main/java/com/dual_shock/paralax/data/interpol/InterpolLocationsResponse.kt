package com.dual_shock.paralax.data.interpol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Locations(
    val name: String? = null,
    val type: String? = null,
    val features: List<Features>
)


@Serializable
data class Features(
    val type: String? = null,
    val id: String? = null,
    val geometry: Geometries,
    val properties: Properties
)

@Serializable
data class Geometries(
    val type: String? = null,
    val coordinates: List<Double>
)

@Serializable
data class Properties(
    val name: String? = null,
    val href: String? = null,
    val altitude: String? = null,
    @SerialName("SYNOP Station Identifier") val synopId: String // Serial name here because this parameter is written with blank spaces

)