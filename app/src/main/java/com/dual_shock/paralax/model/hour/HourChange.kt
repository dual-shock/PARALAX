package com.dual_shock.paralax.model.hour

data class HourChange(
    val watchedHour: WatchedHour,
    val currentHourData: HourData,
    val scoreChange: Int,
    val windSpeedChange: Double?,
    val altWindSpeedChange: Double?,
    val precipitationAppeared: Boolean,
    val precipitationDisappeared: Boolean,
    val hasChanged: Boolean
)

fun WatchedHour.compareWith(current: HourData): HourChange {
    val saved = savedHourData
    return HourChange(
        watchedHour = this,
        currentHourData = current,
        scoreChange = current.viabilityScore - saved.viabilityScore,
        windSpeedChange = (current.forecastEntry?.wind_speed ?: 0.0) - (saved.windSpeed ?: 0.0),
        altWindSpeedChange = (current.altWindSpeed ?: 0.0) - (saved.altWindSpeed ?: 0.0),
        precipitationAppeared = (saved.precipitation ?: 0.0) == 0.0 &&
                (current.forecastEntry?.precipitation_amount ?: 0.0) > 0.0,
        precipitationDisappeared = (saved.precipitation ?: 0.0) > 0.0 &&
                (current.forecastEntry?.precipitation_amount ?: 0.0) == 0.0,
        hasChanged = saved.hasSignificantChanges(current)
    )
}
