package com.dual_shock.paralax.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.GenericStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dual_shock.paralax.R
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.model.hour.HourChange
import com.dual_shock.paralax.model.hour.HourData
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.ui.map.MapStyleViewModel
import com.dual_shock.paralax.ui.theme.Cream
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme
import com.dual_shock.paralax.ui.theme.viabilityColor
import com.dual_shock.paralax.ui.weatherdetail.WeatherDetailsPopup
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


internal fun formatDayLabel(dayOffset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, dayOffset)
    val dom = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.time)
    val dayName = when (dayOffset) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH) ?: ""
    }
    return "$dom. $monthName ($dayName)"
}

@Composable
fun styledText(text: String, baseColor: Color) = buildAnnotatedString {
    val regex = Regex("""\([^)]*\)""")
    var lastEnd = 0
    val dimmedColor = if (baseColor == appColors.onPrimary) appColors.onPrimary.copy(alpha = 0.6f) else appColors.onSurfaceVariant
    regex.findAll(text).forEach { match ->
        withStyle(SpanStyle(color = baseColor)) {
            append(text.substring(lastEnd, match.range.first))
        }
        withStyle(SpanStyle(color = dimmedColor)) {
            append(match.value)
        }
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length) {
        withStyle(SpanStyle(color = baseColor)) {
            append(text.substring(lastEnd))
        }
    }
}
@Composable
fun OtherHourSlotCard(
    hourData: HourData,
    isSelected: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    onWatchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = viabilityColor(hourData.viabilityScore))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d:00", hourData.localHour),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = appColors.onSurface
            )
            Text(
                text = "${hourData.viabilityScore}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = appColors.onSurface
            )
            if (hourData.hasChangedSinceLastCheck) {
                Icon(
                    painter = painterResource(R.drawable.warning),
                    contentDescription = "Changed",
                    tint = appColors.viabilityBad,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onWatchClick) {
                Icon(
                    painter = painterResource(
                        if (hourData.isWatched) R.drawable.visibility
                        else R.drawable.visibility_off
                    ),
                    contentDescription = if (hourData.isWatched) "Unwatch" else "Watch",
                    tint = if (hourData.isWatched) themeColor else appColors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.arrow_down else R.drawable.arrow_right
                ),
                contentDescription = null,
                tint = appColors.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HourSlotCard(
    modifier: Modifier = Modifier,
    rank: Int,
    hourData: HourData,
    themeColor: Color,
    onClick: () -> Unit,
) {
    val forecast = hourData.forecastEntry ?: return

    val dateStr = formatDayLabel(hourData.dayOffset)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val circleColor = viabilityColor(hourData.viabilityScore)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.5.dp, circleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#$rank", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                    }
                    Column {
                        Text(String.format("%02d:00 - %02d:00", hourData.localHour, (hourData.localHour + 1) % 24), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(dateStr, fontSize = 12.sp, color = appColors.onSurfaceVariant)
                    }
                }
                val borderColor = if (isDarkTheme) circleColor.copy(red = circleColor.red * 0.5f, green = circleColor.green * 0.5f, blue = circleColor.blue * 0.5f) else circleColor.copy(red = circleColor.red * 0.7f, green = circleColor.green * 0.7f, blue = circleColor.blue * 0.7f)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${hourData.viabilityScore}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.onPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = appColors.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherStat(iconRes = R.drawable.air, value = "%.1f".format(forecast.wind_speed ?: 0.0), unit = "m/s")
                WeatherStat(iconRes = R.drawable.cloud, value = "${(forecast.cloud_area_fraction ?: 0.0).toInt()}", unit = "%")
                WeatherStat(iconRes = R.drawable.rainy, value = "%.1f".format(forecast.precipitation_amount ?: 0.0), unit = "mm")
                WeatherStat(iconRes = R.drawable.thermometer_add, value = "${(forecast.air_temperature ?: 0.0).toInt()}", unit = "°C")
            }

            // Score breakdown
            if (hourData.specifics.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = appColors.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val displaySpecifics = hourData.specifics + ("Tot" to (hourData.viabilityScore / 100.0))
                    displaySpecifics.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (label, score) ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = appColors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.width(64.dp),
                                        maxLines = 1
                                    )
                                    LinearProgressIndicator(
                                        progress = { score.toFloat() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = scoreColor(score),
                                        trackColor = appColors.onSurface.copy(alpha = 0.1f)
                                    )
                                    Text(
                                        text = "${(score * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.onSurface,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PastHourItemCard(
    modifier: Modifier = Modifier,
    pastHour: HourChange,
    themeColor: Color,
    lightningCount: Int?,
    isExpired: Boolean,
    hoursUntilExpiry: Long?,
    onClick: () -> Unit,
    onWatchClick: () -> Unit
) {
    val dateStr = formatDayLabel(pastHour.watchedHour.savedHourData.dayOffset)
    val changed = pastHour.hasChanged

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (changed) 0.dp else 2.dp),
        border = if (changed) BorderStroke(1.dp, themeColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isExpired -> appColors.surfaceVariant
                changed -> appColors.warning.copy(alpha = 0.1f)
                else -> appColors.surface
            }
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Top row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        String.format("%02d:00 - %02d:00", pastHour.watchedHour.savedHourData.localHour, (pastHour.watchedHour.savedHourData.localHour + 1) % 24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isExpired) appColors.onSurfaceVariant else appColors.onSurface
                    )
                    Text(dateStr, fontSize = 12.sp, color = appColors.onSurfaceVariant)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isExpired -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(appColors.expired)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.schedule),
                                    contentDescription = null,
                                    tint = appColors.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("Expired", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.onPrimary)
                            }
                        }
                        changed -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(themeColor)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.change),
                                    contentDescription = null,
                                    tint = appColors.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("Changed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.onPrimary)
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("No changes", fontSize = 11.sp, color = appColors.onSurfaceVariant)
                                if (hoursUntilExpiry != null) {
                                    Text(
                                        "Expires in ${hoursUntilExpiry}h",
                                        fontSize = 10.sp,
                                        color = if (hoursUntilExpiry <= 3) appColors.warning else appColors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val circleColor = if (isExpired) appColors.expired else viabilityColor(pastHour.currentHourData.viabilityScore)
                    val borderColor = if (isDarkTheme) circleColor.copy(red = circleColor.red * 0.5f, green = circleColor.green * 0.5f, blue = circleColor.blue * 0.5f) else circleColor.copy(red = circleColor.red * 0.7f, green = circleColor.green * 0.7f, blue = circleColor.blue * 0.7f)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .border(1.5.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val score = if (changed) pastHour.currentHourData.viabilityScore
                        else pastHour.watchedHour.savedHourData.viabilityScore
                        Text("$score%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.onPrimary)
                    }
                }
            }

            // Score delta
            if (changed && pastHour.scoreChange != 0) {
                val deltaColor = if (pastHour.scoreChange > 0) appColors.viabilityGood else appColors.viabilityBad
                val arrow = if (pastHour.scoreChange > 0) "▲" else "▼"
                Text(
                    "$arrow ${abs(pastHour.scoreChange)}% score change",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaColor
                )
            }

            HorizontalDivider(color = appColors.onSurface.copy(alpha = 0.08f))

            // Weather stats
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat(
                    iconRes = R.drawable.air,
                    value = if (changed) "%.1f".format(pastHour.currentHourData.forecastEntry?.wind_speed ?: 0.0)
                    else "%.1f".format(pastHour.watchedHour.savedHourData.windSpeed ?: 0.0),
                    unit = "m/s",
                    changed = changed && (pastHour.windSpeedChange?.let { abs(it) > 0.0 } == true)
                )
                WeatherStat(
                    iconRes = R.drawable.cloud,
                    value = if (changed) "${(pastHour.currentHourData.forecastEntry?.cloud_area_fraction_medium ?: 0.0).toInt()}"
                    else "${(pastHour.watchedHour.savedHourData.cloudCoverMedium ?: 0.0).toInt()}",
                    unit = "%"
                )
                WeatherStat(
                    iconRes = R.drawable.rainy,
                    value = if (changed) "%.1f".format(pastHour.currentHourData.forecastEntry?.precipitation_amount ?: 0.0)
                    else "%.1f".format(pastHour.watchedHour.savedHourData.precipitation ?: 0.0),
                    unit = "mm",
                    changed = pastHour.precipitationAppeared || pastHour.precipitationDisappeared
                )
                WeatherStat(
                    iconRes = R.drawable.thermometer_add,
                    value = if (changed) "${(pastHour.currentHourData.forecastEntry?.air_temperature ?: 0.0).toInt()}"
                    else "${(pastHour.watchedHour.savedHourData.airTemperature ?: 0.0).toInt()}",
                    unit = "°C"
                )
                WeatherStat(
                    iconRes = R.drawable.bolt,
                    value = lightningCount?.let { "$it" } ?: "N/A",
                    unit = ""
                )
            }

        }
    }
}

@Composable
private fun WeatherStat(
    iconRes: Int,
    value: String,
    unit: String,
    changed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (changed) appColors.warning else appColors.onSurfaceVariant
        )
        Text(
            "$value$unit",
            fontSize = 12.sp,
            color = if (changed) appColors.warning else appColors.onSurfaceVariant
        )
    }
}

@Composable
fun ScoreInfoLink(
    themeColor: Color,
    specifics: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, appColors.primary),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "How scores work",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.onSurface
                    )
                }
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.arrow_down else R.drawable.arrow_right
                    ),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        "Each hour score is a weighted composition of:",
                        fontSize = 10.sp,
                        color = appColors.onSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    specifics.forEach { (label, weight) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = appColors.onSurfaceVariant,
                                modifier = Modifier.width(105.dp),
                                textAlign = TextAlign.End
                            )
                            LinearProgressIndicator(
                                progress = { weight.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = themeColor,
                                trackColor = themeColor.copy(alpha = 0.15f)
                            )
                            Text(
                                text = "${(weight * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Text(
                        "If wind speed, precipitation or fog coverage exceed the limits set in the app settings, the overall viability score is set to 0",
                        fontSize = 10.sp,
                        color = appColors.onSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}


@Composable
private fun scoreColor(score: Double) = when {
    score < 0.3 -> appColors.viabilityBad
    score < 0.6 -> appColors.viabilityMedium
    else        -> appColors.viabilityGood
}

@Composable
fun DashboardScreen(
    onNavigateToSettings: (location: String, day: Int, hour: String) -> Unit,
    onNavigateToForecast: (location: String, day: Int, hour: String) -> Unit,
    onNavigateToTrajectory: (id: Int, location: String, day: Int, hour: String, viabilityScore: Int) -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToAddLocation: () -> Unit = {},
    onNavigateToEditLocation: (Int) -> Unit = {},
    onSelectTimeWindow: (Int, Int) -> Unit,
    mapStyleViewModel: MapStyleViewModel,
    selectSiteIndex: Int? = null,
    viewModel: DashboardScreenViewModel,
    onToggleTheme: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStyle by mapStyleViewModel.selectedStyle.collectAsState()

    val themeColor = appColors.primary
    val selectedSite = uiState.launchSites.getOrNull(uiState.selectedSiteIndex)
    val scope = rememberCoroutineScope()

    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var lightningAcknowledged by remember { mutableStateOf(false) }
    var showLayersPopup by remember { mutableStateOf(false) }
    var showWeatherDetailsPopup by remember { mutableStateOf(false) }
    var heatStripWindowStart by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(
        initialPage = uiState.mode?.pageIndex ?: 0,
        pageCount = { 3 }
    )

    val closestHour = uiState.hours.firstOrNull { it.lightningStrikeCount != null }
    val showLightningOverlay = (closestHour?.lightningStrikeCount ?: 0) > 0 && !lightningAcknowledged

    val scoreWeights = remember {
        listOf(
            "Wind Ground" to 0.20,
            "Wind Aloft" to 0.15,
            "Wind Shear" to 0.15,
            "Cloud Hi" to 0.08,
            "Cloud Med" to 0.07,
            "Cloud Low" to 0.05,
            "Fog" to 0.10,
            "Humidity" to 0.05,
            "Dew Point" to 0.05,
            "Temperature" to 0.05,
            "Pressure" to 0.05
        )
    }

    LaunchedEffect(closestHour?.lightningStrikeCount) { lightningAcknowledged = false }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshIfStale()
        onPauseOrDispose { }
    }

    LaunchedEffect(selectSiteIndex, uiState.launchSites.size) {
        if (selectSiteIndex != null && selectSiteIndex < uiState.launchSites.size) {
            viewModel.selectSite(selectSiteIndex)
        }
    }

    LaunchedEffect(uiState.hours) {
        if (uiState.hours.isNotEmpty() && uiState.selectedHour == null) {
            viewModel.selectHour(uiState.hours.first())
        }
    }

    LaunchedEffect(uiState.mode) {
        val targetPage = uiState.mode?.pageIndex ?: 0
        if (pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val newMode = DashboardMode.fromPageIndex(pagerState.settledPage)
        if (uiState.mode != newMode) {
            viewModel.setMode(newMode)
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(10.0)
            center(Point.fromLngLat(10.0, 63.0))
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(selectedSite) {
        selectedSite?.let { site ->
            mapViewportState.easeTo(
                CameraOptions.Builder()
                    .zoom(10.0)
                    .center(Point.fromLngLat(site.lon, site.lat))
                    .build()
            )
        }
    }

    if (showLayersPopup) {
        uiState.selectedHour?.let { hour ->
            StaggeredAtmosphericTable(
                atmosphericLayers = hour.atmosphericLayers ?: emptyList(),
                shearLayers = hour.shearLayers ?: emptyList(),
                locationName = selectedSite?.name ?: "",
                dateLabel = formatDayLabel(hour.dayOffset),
                hour = String.format(Locale.US, "%02d:00", hour.localHour),
                onDismiss = { showLayersPopup = false }
            )
        }
    }

    if (showWeatherDetailsPopup) {
        uiState.selectedHour?.let { hour ->
            hour.forecastEntry?.let { entry ->
                WeatherDetailsPopup(
                    entry = entry,
                    lightningCount = hour.lightningStrikeCount,
                    location = selectedSite?.name ?: "",
                    dateLabel = formatDayLabel(hour.dayOffset),
                    hour = String.format(Locale.US, "%02d:00", hour.localHour),
                    viabilityScore = hour.viabilityScore,
                    onDismiss = { showWeatherDetailsPopup = false }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            DashboardHeader(
                themeColor = themeColor,
                selectedName = selectedSite?.name,
                sites = uiState.launchSites,
                locationDropdownExpanded = locationDropdownExpanded,
                onLocationToggle = { locationDropdownExpanded = !locationDropdownExpanded },
                onLocationDismiss = { locationDropdownExpanded = false },
                onLocationSelect = {
                    viewModel.selectSite(it)
                    locationDropdownExpanded = false
                },
                onAddLocation = { onNavigateToAddLocation() },
                onEditLocation = { onNavigateToEditLocation(it) },
                onSettingsClick = {
                    onNavigateToSettings(
                        selectedSite?.name ?: "none",
                        uiState.effectiveDay,
                        viewModel.getFormattedHour()
                    )
                },
                onToggleTheme = onToggleTheme,
                isLoading = uiState.isLoading
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(appColors.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                // Mode selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        .border(1.dp, themeColor.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        DashboardMode.Upcoming to "Upcoming",
                        DashboardMode.Best to "Best",
                        DashboardMode.Watched to "Watched"
                    ).forEach { (mode, label) ->
                        val active = uiState.mode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) themeColor else Color.Transparent)
                                .clickable { viewModel.setMode(mode) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) appColors.onPrimary else appColors.onPrimaryMuted,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Map + Focused Hour Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapCard(
                        selectedSite = selectedSite,
                        selectedStylePreset = selectedStyle.preset,
                        mapViewportState = mapViewportState,
                        onNavigateToMap = onNavigateToMap,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                    )
                    FocusedHourStrip(
                        hour = uiState.selectedHour,
                        themeColor = themeColor,
                        isWatched = uiState.selectedHour?.isWatched == true,
                        onToggleWatch = {
                            uiState.selectedHour?.let { hour ->
                                scope.launch {
                                    viewModel.toggleWatchedTimestamp(hour.utcTimestamp)
                                }
                            }
                        },
                        onOpenDetails = { showWeatherDetailsPopup = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Mode-specific content (swipeable)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                ) { page ->
                    when (page) {
                        0 -> {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val isLandscape = maxWidth > maxHeight
                                val windowedHours = uiState.hours
                                    .drop(heatStripWindowStart)
                                    .take(6)

                                if (isLandscape) {
                                    val scrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeatStripSelector(
                                            hours = uiState.hours,
                                            windowStart = heatStripWindowStart,
                                            selectedHour = uiState.selectedHour,
                                            onWindowChange = { newStart ->
                                                if (newStart != heatStripWindowStart) {
                                                    heatStripWindowStart = newStart
                                                    uiState.hours.getOrNull(newStart)
                                                        ?.let { viewModel.selectHourDebounced(it) }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        SixHourTable(
                                            hours = windowedHours,
                                            selectedHour = uiState.selectedHour,
                                            themeColor = themeColor,
                                            onSelectHour = { viewModel.selectHour(it) },
                                            onShearClick = { hour ->
                                                viewModel.selectHour(hour)
                                                showLayersPopup = true
                                            },
                                            isLandscape = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeatStripSelector(
                                            hours = uiState.hours,
                                            windowStart = heatStripWindowStart,
                                            selectedHour = uiState.selectedHour,
                                            onWindowChange = { newStart ->
                                                if (newStart != heatStripWindowStart) {
                                                    heatStripWindowStart = newStart
                                                    uiState.hours.getOrNull(newStart)
                                                        ?.let { viewModel.selectHourDebounced(it) }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        SixHourTable(
                                            hours = windowedHours,
                                            selectedHour = uiState.selectedHour,
                                            themeColor = themeColor,
                                            onSelectHour = { viewModel.selectHour(it) },
                                            onShearClick = { hour ->
                                                viewModel.selectHour(hour)
                                                showLayersPopup = true
                                            },
                                            isLandscape = false,
                                            modifier = Modifier.fillMaxWidth().weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            val bestHours = uiState.bestHours
                            var selectedHour by remember { mutableStateOf<String?>(null) }


                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                item {
                                    Column {
                                        ScoreInfoLink(
                                            themeColor = themeColor,
                                            specifics = scoreWeights
                                        )
                                        Text(
                                            text = "Ideal launch windows next ${uiState.hours.size} hours:",
                                            color = appColors.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp).offset(y = 2.dp)
                                        )
                                    }
                                }

                                itemsIndexed(bestHours, key = { _, it -> it.utcTimestamp }) { index, hourData ->
                                    val isSelected = selectedHour == hourData.utcTimestamp

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        HourSlotCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            rank = index + 1,
                                            hourData = hourData,
                                            themeColor = themeColor,
                                            onClick = {
                                                selectedHour = if (isSelected) null else hourData.utcTimestamp
                                                viewModel.selectHour(hourData)
                                                onSelectTimeWindow(hourData.dayOffset + 1, hourData.localHour)
                                            },

                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            val hours = uiState.hourChanges

                            if (hours.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "Select time windows by clicking them, then click the eye in the bottom right of the selected hour above to watch it. You will be notified if anything changes in this time window.",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                var selectedHour by remember { mutableStateOf<String?>(null) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(
                                        hours,
                                        key = { it.watchedHour.savedHourData.utcTimestamp }
                                    ) { w ->
                                        val isSelected = selectedHour == w.watchedHour.savedHourData.utcTimestamp
                                        val now = Instant.now()
                                        val hourInstant = runCatching {
                                            Instant.parse(w.watchedHour.savedHourData.utcTimestamp)
                                        }.getOrNull()
                                        val isExpired = hourInstant?.isBefore(now) == true
                                        val hoursUntilExpiry = hourInstant?.let {
                                            val diff = Duration.between(now, it)
                                            if (diff.isNegative) null else diff.toHours()
                                        }
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            PastHourItemCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                pastHour = w,
                                                isExpired = isExpired,
                                                hoursUntilExpiry = hoursUntilExpiry,
                                                lightningCount = w.watchedHour.savedHourData.lightningStrikeCount,
                                                themeColor = themeColor,
                                                onClick = {
                                                    selectedHour = if (isSelected) null
                                                    else w.watchedHour.savedHourData.utcTimestamp
                                                    viewModel.selectHour(w.currentHourData)
                                                    onSelectTimeWindow(
                                                        w.watchedHour.savedHourData.dayOffset + 1,
                                                        w.watchedHour.savedHourData.localHour
                                                    )
                                                },
                                                onWatchClick = {
                                                    scope.launch {
                                                        viewModel.toggleWatchedTimestamp(
                                                            w.watchedHour.savedHourData.utcTimestamp
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                // Bottom buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDarkTheme = isDarkTheme
                    val forecastButtonColor = if (isDarkTheme) themeColor else appColors.onPrimaryMuted
                    OutlinedButton(
                        onClick = {
                            onNavigateToForecast(
                                selectedSite?.name ?: "none",
                                uiState.effectiveDay,
                                viewModel.getFormattedHour()
                            )
                        },
                        modifier = Modifier
                            .weight(0.45f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(start = 10.dp, end = 4.dp),
                        border = BorderStroke(1.dp, forecastButtonColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = forecastButtonColor)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lists),
                            contentDescription = null,
                            tint = forecastButtonColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Full forecast",
                            color = forecastButtonColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            selectedSite?.let { site ->
                                val hour = uiState.selectedHour
                                val day = if (hour != null) hour.dayOffset + 1 else uiState.effectiveDay
                                val hourStr = if (hour != null)
                                    String.format(Locale.US, "%02d:00", hour.localHour)
                                else viewModel.getFormattedHour()
                                onNavigateToTrajectory(site.id, site.name, day, hourStr, hour?.viabilityScore ?: 0)
                            }
                        },
                        modifier = Modifier
                            .weight(0.55f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(start = 10.dp, end = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.orbit),
                            contentDescription = null,
                            tint = appColors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        val displayHour = uiState.selectedHour?.let {
                            String.format(Locale.US, "%02d:00", it.localHour)
                        } ?: viewModel.getFormattedHour()
                        Text(
                            text = "Simulation · $displayHour",
                            color = appColors.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Lightning overlay
        if (showLightningOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(appColors.onSurface.copy(alpha = 0.5f))
                    .clickable { lightningAcknowledged = true },
                contentAlignment = Alignment.Center
            ) {
                LightningWarningBanner(
                    strikeCount = closestHour?.lightningStrikeCount ?: 0,
                    onAcknowledge = { lightningAcknowledged = true }
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    themeColor: Color,
    selectedName: String?,
    sites: List<LaunchSite>,
    locationDropdownExpanded: Boolean,
    onLocationToggle: () -> Unit,
    onLocationDismiss: () -> Unit,
    onLocationSelect: (Int) -> Unit,
    onAddLocation: () -> Unit,
    onEditLocation: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    isLoading: Boolean = false
) {
    var rotation by remember { mutableStateOf(45f) }
    val primaryColor = appColors.primary
    val backgroundColor = appColors.background
    val isDarkTheme = isDarkTheme
    val headerContentColor = appColors.onPrimary  // text/icons stay dark in both modes
    val headerOverlayColor = if (isDarkTheme) Color.White else appColors.onPrimaryOverlay  // white bg in dark mode

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to backgroundColor,
                            0.75f to primaryColor,
                            1f to primaryColor
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .offset(x = 4.dp)
                .clickable {
                    rotation += 180f
                    onToggleTheme()
                }
        ) {
            Icon(
                painterResource(if (isDarkTheme) R.drawable.app_logo_cyan else R.drawable.app_logo_orange),
                contentDescription = "Toggle theme",
                tint = Color.Unspecified,
                modifier = Modifier
                    .requiredSize(60.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Cream.copy(alpha = 0.325f),
                                    Cream.copy(alpha = 0.25f),
                                    Cream.copy(alpha = 0.125f),
                                    Cream.copy(alpha = 0.075f),
                                    Cream.copy(alpha = 0.025f),
                                    Cream.copy(alpha = 0.005f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 1.0f
                            )
                        )
                    }
            )
        }

        Box {
            OutlinedButton(
                onClick = onLocationToggle,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = headerContentColor,
                    containerColor = headerOverlayColor.copy(alpha = 0.15f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(headerContentColor.copy(alpha = 0.5f))),
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Icon(painterResource(R.drawable.my_location), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(selectedName ?: "Select location", color = headerContentColor)
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(if (locationDropdownExpanded) R.drawable.arrow_down else R.drawable.arrow_right), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            DropdownMenu(
                expanded = locationDropdownExpanded,
                onDismissRequest = onLocationDismiss,
                modifier = Modifier.Companion
                    .background(appColors.surface)
                    .border(1.dp, appColors.outline),
                shape = RoundedCornerShape(0.dp),
                containerColor = appColors.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(painterResource(R.drawable.add_location), contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                                Text("Add location", fontWeight = FontWeight.Bold, color = themeColor)
                            }
                        },
                        onClick = { onAddLocation(); onLocationDismiss() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(color = appColors.outline, thickness = 1.dp)
                    sites.forEachIndexed { index, site ->
                        DropdownMenuItem(
                            text = {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(site.name, color = appColors.onSurface)
                                    if (!site.isDefault) {
                                        Icon(
                                            painterResource(R.drawable.edit_location),
                                            contentDescription = "Edit",
                                            tint = appColors.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp).clickable { onEditLocation(site.id) }
                                        )
                                    }
                                }
                            },
                            onClick = { onLocationSelect(index); onLocationDismiss() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Box(contentAlignment = Alignment.CenterEnd) {
            if (isLoading) {
                var spinnerRotation by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(16)
                        spinnerRotation = (spinnerRotation + 6f) % 360f
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.loading),
                    contentDescription = "Loading",
                    tint = headerContentColor,
                    modifier = Modifier
                        .offset(x = (-44).dp)
                        .size(20.dp)
                        .rotate(spinnerRotation)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(painterResource(R.drawable.settings), contentDescription = "Settings", tint = headerContentColor)
            }
        }
    }
}

@Composable
fun MapCard(
    selectedSite: LaunchSite?,
    selectedStylePreset: String,
    mapViewportState: MapViewportState,
    onNavigateToMap: () -> Unit,
    modifier: Modifier
) {
    if (selectedSite != null) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.dp, appColors.onSurface.copy(alpha = 0.3f))
        ) {
            Box {
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState,
                    style = { GenericStyle(style = Style.STANDARD) },
                    scaleBar = {},
                    logo = {},
                    attribution = {}
                ) {
                    MapEffect(selectedStylePreset, selectedSite.id) { mapView ->
                        mapView.mapboxMap.getStyle { style ->
                            style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(selectedStylePreset))
                            style.setStyleImportConfigProperty("basemap", "theme", Value.valueOf("faded"))
                        }
                    }
                }
                Box(Modifier.matchParentSize().clickable { onNavigateToMap() })
                ZoomControls(
                    mapViewportState = mapViewportState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 4.dp),
                    buttonSize = 24.dp
                )
            }
        }
    } else {
        Box(modifier.background(appColors.outline, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("No location selected")
        }
    }
}

@Composable
fun ZoomControls(
    mapViewportState: MapViewportState,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("+" to 1.0, "−" to -1.0).forEach { (label, delta) ->
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(appColors.surface.copy(alpha = 0.9f))
                    .clickable {
                        scope.launch {
                            mapViewportState.easeTo(
                                CameraOptions.Builder()
                                    .zoom((mapViewportState.cameraState?.zoom ?: 10.0) + delta)
                                    .build()
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = (buttonSize.value * 0.5f).sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.onSurface
                )
            }
        }
    }
}

@Composable
private fun FocusedHourStrip(
    hour: HourData?,
    themeColor: Color,
    isWatched: Boolean,
    onToggleWatch: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayOffset = hour?.dayOffset ?: 0
    val localHour = hour?.localHour ?: 0
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
    val dayLabel = if (dayOffset == 0) "Today"
    else SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.time)
    val pct = hour?.viabilityScore ?: 0
    val verdict = when {
        pct >= 65 -> "Good launch window"
        pct >= 40 -> "Marginal — check details"
        else -> "Not recommended"
    }
    Row(
        modifier = modifier
            .background(appColors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, appColors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score circle with outer ring for breathing room
        val circleColor = hour?.let { viabilityColor(it.viabilityScore) } ?: appColors.outline
        val innerBorderColor = if (isDarkTheme) circleColor.copy(red = circleColor.red * 0.5f, green = circleColor.green * 0.5f, blue = circleColor.blue * 0.5f) else circleColor.copy(red = circleColor.red * 0.7f, green = circleColor.green * 0.7f, blue = circleColor.blue * 0.7f)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, appColors.onSurface.copy(alpha = 0.08f), CircleShape)
                .padding(1.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(1.dp, innerBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$pct",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = appColors.onPrimary
            )
        }
        // Right side: text + bottom row with details button and eye
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "$dayLabel · ${String.format(Locale.US, "%02d:00", localHour)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.onSurface,
                maxLines = 1,
                lineHeight = 14.sp
            )
            Text(
                verdict,
                fontSize = 10.sp,
                color = appColors.onSurfaceVariant,
                maxLines = 1,
                lineHeight = 11.sp
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                ) {
                    Text("Details ›", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isWatched) themeColor.copy(alpha = 0.15f) else appColors.surface)
                        .border(1.dp, appColors.outlineVariant, CircleShape)
                        .clickable { onToggleWatch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isWatched) R.drawable.visibility else R.drawable.visibility_off
                        ),
                        contentDescription = if (isWatched) "Unwatch" else "Watch",
                        tint = if (isWatched) themeColor else appColors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatStripSelector(
    hours: List<HourData>,
    windowStart: Int,
    selectedHour: HourData?,
    onWindowChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hours.isEmpty()) return

    val isDark = isDarkTheme
    val stripLineColor = if (isDark) appColors.background else appColors.background           // graph lines, separators, day text
    val stripBorderColor = if (isDark) appColors.onBackground else Color.Black    // selector border, indicator outer

    val currentWindowStart by rememberUpdatedState(windowStart)

    val segments = remember(hours) {
        buildList {
            hours.forEachIndexed { idx, hour ->
                if (idx > 0 && hour.dayOffset != hours[idx - 1].dayOffset) {
                    add(null)
                }
                add(hour)
            }
        }
    }

    val totalSegments = segments.size
    val density = LocalDensity.current
    var stripWidthPx by remember { mutableStateOf(1f) }

    fun hourIndexToSegmentIndex(hourIdx: Int): Int {
        var segIdx = 0
        var hourCount = 0
        for (seg in segments) {
            if (seg != null) {
                if (hourCount == hourIdx) return segIdx
                hourCount++
            }
            segIdx++
        }
        return segIdx
    }

    val windowSegmentStart = hourIndexToSegmentIndex(windowStart)
    val windowSegmentEnd = hourIndexToSegmentIndex((windowStart + 5).coerceAtMost(hours.lastIndex))
    val windowSegmentSpan = windowSegmentEnd - windowSegmentStart + 1

    val totalWeight = segments.sumOf { if (it == null) 0.3 else 1.0 }.toFloat()

    fun segmentWeightBefore(idx: Int): Float {
        var w = 0f
        for (i in 0 until idx) {
            w += if (segments[i] == null) 0.3f else 1f
        }
        return w
    }

    val startWeight = segmentWeightBefore(windowSegmentStart)
    val spanWeight = (windowSegmentStart..windowSegmentEnd).sumOf {
        if (segments.getOrNull(it) == null) 0.3 else 1.0
    }.toFloat()

    // Outer container without clipping so selector can extend outside
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .onSizeChanged { stripWidthPx = it.width.toFloat() }
    ) {
        // The actual strip with clipping
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .background(appColors.outlineVariant.copy(alpha = 0.3f))
                .pointerInput(totalSegments, hours.size) {
                    fun offsetToHourIndex(xOffset: Float): Int {
                        val segIdx = ((xOffset / size.width) * totalSegments).toInt()
                            .coerceIn(0, totalSegments - 1)
                        var hourIdx = 0
                        for (i in 0 until segIdx) {
                            if (segments[i] != null) hourIdx++
                        }
                        return hourIdx
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // Immediately jump to touch position
                        val hourIdx = offsetToHourIndex(down.position.x)
                        val newStart = (hourIdx - 3).coerceIn(0, (hours.size - 6).coerceAtLeast(0))
                        onWindowChange(newStart)

                        var accumulatedDrag = 0f
                        val avgCellWidthPx = stripWidthPx / hours.size

                        do {
                            val event = awaitPointerEvent()
                            val dragAmount = event.changes.firstOrNull()?.let {
                                if (it.pressed) {
                                    val delta = it.position.x - it.previousPosition.x
                                    it.consume()
                                    delta
                                } else null
                            } ?: break

                            accumulatedDrag += dragAmount
                            val deltaHours = (accumulatedDrag / avgCellWidthPx).toInt()
                            if (deltaHours != 0) {
                                val updatedStart = (currentWindowStart + deltaHours)
                                    .coerceIn(0, (hours.size - 6).coerceAtLeast(0))
                                onWindowChange(updatedStart)
                                accumulatedDrag -= deltaHours * avgCellWidthPx
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Row(Modifier.fillMaxSize()) {
                segments.forEachIndexed { idx, seg ->
                    if (idx > 0 && seg != null && segments.getOrNull(idx - 1) != null) {
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .fillMaxHeight()
                                .background(if (isDark) appColors.onBackground.copy(alpha = 0.50f) else Color.Gray.copy(alpha = 0.4f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(if (seg == null) 0.3f else 1f)
                            .fillMaxHeight()
                    ) {
                        if (seg != null) {
                            val scoreFraction = seg.viabilityScore / 100f
                            val fillFraction = 0.33f + (scoreFraction * 0.67f)
                            // Dimmed color fills entire bar
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(viabilityColor(seg.viabilityScore).copy(alpha = 0.45f))
                            )
                            // Solid color from bottom up
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillFraction)
                                    .align(Alignment.BottomCenter)
                                    .background(viabilityColor(seg.viabilityScore))
                            )
                            // Line at boundary
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillFraction)
                                    .align(Alignment.BottomCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .background(stripLineColor)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(stripLineColor)
                            )
                        }
                    }
                }
            }

            // Overlay for selected window - slightly brighter
            if (stripWidthPx > 1f && hours.size >= 6) {
                val overlayStartX = with(density) { (stripWidthPx * startWeight / totalWeight).toDp() }
                val overlayWidth = with(density) { (stripWidthPx * spanWeight / totalWeight).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = overlayStartX)
                        .width(overlayWidth)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }

            // Day labels - only for days with more than 8 hours
            val dayBoundaries = remember(hours) {
                val boundaries = mutableListOf<Triple<Int, Int, Int>>() // (dayOffset, startHourIndex, hourCount)
                if (hours.isEmpty()) return@remember boundaries
                var currentDay = hours[0].dayOffset
                var startIdx = 0
                var count = 0
                hours.forEachIndexed { idx, hour ->
                    if (hour.dayOffset != currentDay) {
                        boundaries.add(Triple(currentDay, startIdx, count))
                        currentDay = hour.dayOffset
                        startIdx = idx
                        count = 1
                    } else {
                        count++
                    }
                }
                boundaries.add(Triple(currentDay, startIdx, count))
                boundaries
            }

            dayBoundaries.forEach { (dayOffset, startHourIdx, hourCount) ->
                if (hourCount > 8) {
                    val segIdx = hourIndexToSegmentIndex(startHourIdx)
                    val labelWeight = segmentWeightBefore(segIdx)
                    val labelX = with(density) { (stripWidthPx * labelWeight / totalWeight).toDp() }

                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                    }
                    val dayLabel = if (dayOffset == 0) "Today"
                    else SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.time)

                    Text(
                        text = dayLabel,
                        color = if (isDark) Color.White else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(1f, 1f),
                                blurRadius = 8f
                            )
                        ),
                        modifier = Modifier
                            .offset(x = labelX + 6.dp, y = 38.dp)
                    )
                }
            }

            Text(
                text = "${hours.size}hrs",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(1f, 1f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
            )
        }

        // Selector box - outside the clipped strip so it can extend beyond
        val offsetX = with(density) { (stripWidthPx * startWeight / totalWeight).toDp() }
        val boxWidth = with(density) { (stripWidthPx * spanWeight / totalWeight).toDp() }

        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(boxWidth)
                .fillMaxHeight()
                .border(2.dp, stripBorderColor, RoundedCornerShape(6.dp))
        )

        // Selected hour indicator - on top of everything
        selectedHour?.let { sel ->
            val selIdx = hours.indexOf(sel)
            if (selIdx >= 0) {
                val selSegIdx = hourIndexToSegmentIndex(selIdx)
                val selStartWeight = segmentWeightBefore(selSegIdx)
                val selCellWeight = if (segments.getOrNull(selSegIdx) == null) 0.3f else 1f
                val leftBarX = with(density) { (stripWidthPx * selStartWeight / totalWeight).toDp() }
                val rightBarX = with(density) { (stripWidthPx * (selStartWeight + selCellWeight) / totalWeight).toDp() }

                // Left edge: accent | contrast
                Box(
                    modifier = Modifier
                        .offset(x = leftBarX)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(stripBorderColor)
                )
                Box(
                    modifier = Modifier
                        .offset(x = leftBarX + 2.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(stripLineColor)
                )
                // Right edge: accent | contrast
                Box(
                    modifier = Modifier
                        .offset(x = rightBarX - 4.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(stripLineColor)
                )
                Box(
                    modifier = Modifier
                        .offset(x = rightBarX - 2.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(stripBorderColor)
                )
            }
        }
    }
}

@Composable
private fun SixHourTable(
    hours: List<HourData>,
    selectedHour: HourData?,
    themeColor: Color,
    onSelectHour: (HourData) -> Unit,
    onShearClick: (HourData) -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.wrapContentHeight() else Modifier.fillMaxHeight())
            .background(appColors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, appColors.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Header row with icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surfaceVariant.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TIME", Modifier.width(52.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
            Spacer(Modifier.width(32.dp))
            Row(Modifier.weight(0.90f), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.air), null, tint = appColors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                Text(" WIND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
            }
            Row(Modifier.weight(0.65f), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.drop), null, tint = appColors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                Text(" RAIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
            }
            Row(Modifier.weight(0.65f), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.cloud), null, tint = appColors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                Text(" CLOUD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
            }
            Row(Modifier.width(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(" ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
                Icon(painterResource(R.drawable.bolt), null, tint = appColors.onSurfaceVariant, modifier = Modifier.size(12.dp))
            }
            Text("SHEAR", Modifier.width(44.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = appColors.onSurfaceVariant)
        }

        hours.forEachIndexed { index, hour ->
            val isSelected = selectedHour?.utcTimestamp == hour.utcTimestamp
            val hourLightning = hour.lightningStrikeCount ?: 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.padding(vertical = 10.dp) else Modifier.weight(1f))
                    .background(if (isSelected) themeColor.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelectHour(hour) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .then(if (isLandscape) Modifier.height(48.dp) else Modifier.fillMaxHeight())
                        .background(if (isSelected) themeColor else Color.Transparent)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.width(52.dp)) {
                        Text(
                            String.format(Locale.US, "%02d:00", hour.localHour),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.onSurface,
                            lineHeight = 14.sp
                        )
                        Text(
                            if (hour.dayOffset == 0) "today" else "+${hour.dayOffset}d",
                            fontSize = 9.sp,
                            color = appColors.onSurfaceVariant,
                            lineHeight = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier.width(36.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val circleColor = viabilityColor(hour.viabilityScore)
                        val borderColor = if (isDarkTheme) circleColor.copy(red = circleColor.red * 0.5f, green = circleColor.green * 0.5f, blue = circleColor.blue * 0.5f) else circleColor.copy(red = circleColor.red * 0.7f, green = circleColor.green * 0.7f, blue = circleColor.blue * 0.7f)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(1.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${hour.viabilityScore}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.onPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val windDir = hour.forecastEntry?.wind_from_direction ?: 0.0
                        val compass = when {
                            windDir >= 337.5 || windDir < 22.5 -> "N"
                            windDir < 67.5 -> "NE"
                            windDir < 112.5 -> "E"
                            windDir < 157.5 -> "SE"
                            windDir < 202.5 -> "S"
                            windDir < 247.5 -> "SW"
                            windDir < 292.5 -> "W"
                            else -> "NW"
                        }
                        Text(compass, fontSize = 10.sp, color = appColors.onSurfaceVariant)
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = appColors.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer { rotationZ = (windDir + 90).toFloat() }
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${(hour.forecastEntry?.wind_speed ?: 0.0).roundToInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.onSurface
                        )
                        Text(" m/s", fontSize = 10.sp, color = appColors.onSurfaceVariant)
                    }

                    Row(Modifier.weight(0.65f), verticalAlignment = Alignment.CenterVertically) {
                        val precip = hour.forecastEntry?.precipitation_amount ?: 0.0
                        Text(
                            "${"%.1f".format(Locale.US, precip)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (precip > 0) appColors.rainHighlight else appColors.onSurface
                        )
                        Text(" mm", fontSize = 10.sp, color = appColors.onSurfaceVariant)
                    }

                    Row(Modifier.weight(0.55f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${(hour.forecastEntry?.cloud_area_fraction ?: 0.0).roundToInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.onSurface
                        )
                        Text("%", fontSize = 10.sp, color = appColors.onSurfaceVariant)
                    }

                    Text(
                        "$hourLightning",
                        modifier = Modifier.width(28.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hourLightning > 0) appColors.viabilityBad else appColors.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.1f))
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable { onShearClick(hour) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.compare_arrows),
                            contentDescription = "View shear table",
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (index < hours.lastIndex) {
                HorizontalDivider(color = appColors.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun DayAndForecastRow(
    selectedDayLabel: String,
    dayLabels: List<String>,
    themeColor: Color,
    dayDropdownExpanded: Boolean,
    onDayToggle: () -> Unit,
    onDayDismiss: () -> Unit,
    onDaySelect: (Int) -> Unit,
    onForecastClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = onDayToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = appColors.onSurface),
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Icon(painterResource(R.drawable.today), contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(styledText(selectedDayLabel, appColors.onSurface), maxLines = 1, softWrap = false)
                Spacer(Modifier.weight(1f))
                Icon(painterResource(if (dayDropdownExpanded) R.drawable.arrow_down else R.drawable.arrow_right), contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            DropdownMenu(
                expanded = dayDropdownExpanded,
                onDismissRequest = onDayDismiss,
                modifier = Modifier.Companion
                    .background(appColors.surface)
                    .border(1.dp, appColors.outline),
                shape = RoundedCornerShape(0.dp),
                containerColor = appColors.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(styledText(label, appColors.onSurface)) },
                            onClick = { onDaySelect(index + 1) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                        if (index < dayLabels.lastIndex) {
                            HorizontalDivider(color = appColors.outline, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
        Button(onClick = onForecastClick, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
            Text("72hr Forecast")
        }
    }
}




@Composable
fun WeatherCard(
    forecast: ForecastEntry?,
    lightningStrikeCount: Int?,
    timeLabel: String,
    onClick: () -> Unit,
    onShearClick: () -> Unit,
    modifier: Modifier
) {
    val conditions = listOf(
        Triple(R.drawable.air, forecast?.wind_speed?.let { "${it.roundToInt()} m/s" } ?: "--", "Wind"),
        Triple(R.drawable.rainy, forecast?.precipitation_amount?.let { "${"%.1f".format(it)} mm" } ?: "--", "Precip."),
        Triple(R.drawable.cloud, forecast?.cloud_area_fraction?.let { "${it.roundToInt()}%" } ?: "--", "Cloud"),
        Triple(R.drawable.bolt, lightningStrikeCount?.let { "$it" } ?: "N/A", "Lightning"),
        Triple(R.drawable.compare_arrows, "Table", "Shear")
    )

    Card(
        modifier = modifier.clickable { onClick() },
        // ...rest unchanged
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weather details for $timeLabel", style = MaterialTheme.typography.labelLarge, color = appColors.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    conditions.forEachIndexed { i, (icon, value, label) ->
                        WeatherMiniCard(
                            iconRes = icon,
                            value = value,
                            label = label,
                            onClick = if (i == conditions.lastIndex) onShearClick else null
                        )
                    }
                }
            }
            Text(
                text = "<click to expand>",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
    }
}


@Composable
fun WeatherMiniCard(iconRes: Int, value: String, label: String, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        modifier = Modifier.width(80.dp).then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(painterResource(iconRes), contentDescription = label, modifier = Modifier.size(28.dp), tint = appColors.primary)
            Text(value, style = MaterialTheme.typography.bodySmall, color = appColors.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onSurfaceVariant)
        }
    }
}


@Composable
fun LightningWarningBanner(strikeCount: Int, onAcknowledge: () -> Unit) {
    val dangerColor = appColors.viabilityBad
    Card(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = dangerColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.bolt),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = appColors.onPrimary
                )
                Text(
                    "Lightning detected nearby",
                    color = appColors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                "$strikeCount strikes in the last 2 hours. Not safe to launch.",
                color = appColors.onPrimary.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(containerColor = appColors.surface),
            ) {
                Text("proceed anyway", color = dangerColor)
            }
        }
    }
}