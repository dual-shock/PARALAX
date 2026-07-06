package com.dual_shock.paralax.model.hour

import kotlinx.serialization.Serializable

@Serializable
data class WatchedHour(
    val siteId: Int,
    val watchedAt: Long,
    val savedHourData: HourDataSnapshot
)
