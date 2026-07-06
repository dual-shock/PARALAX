package com.dual_shock.paralax.ui.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dual_shock.paralax.R
import com.dual_shock.paralax.model.hour.HourData
import com.dual_shock.paralax.ui.dashboard.DashboardScreenViewModel
import com.dual_shock.paralax.ui.dashboard.formatDayLabel
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme
import com.dual_shock.paralax.ui.theme.viabilityColor

enum class SortOption { TIME, VIABILITY, WIND_SPEED, CLOUD_COVERAGE, RAIN, TEMPERATURE, HUMIDITY }

@Suppress("UNUSED_PARAMETER")
@Composable
fun ForecastScreen(
    location: String,
    day: Int,
    hour: String,
    onNavigateBack: () -> Unit,
    onSelectTimeWindow: (Int, Int) -> Unit,
    dashboardViewModel: DashboardScreenViewModel
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val hours = uiState.hours.filter { it.forecastEntry != null }

    // Filter state - range sliders (ranges match settings screen min/max values)
    var scoreRange by remember { mutableStateOf(10f..100f) }
    var windRange by remember { mutableStateOf(0f..20f) }
    var cloudRange by remember { mutableStateOf(0f..90f) }
    var rainRange by remember { mutableStateOf(0f..30f) }
    var tempRange by remember { mutableStateOf(4f..30f) }
    var humidityRange by remember { mutableStateOf(50f..90f) }
    var filtersExpanded by remember { mutableStateOf(false) }

    // Sort state
    var sortBy by remember { mutableStateOf(SortOption.WIND_SPEED) }
    var sortAscending by remember { mutableStateOf(false) }

    val filteredAndSorted = remember(
        hours, windRange, cloudRange, scoreRange, rainRange, tempRange, humidityRange, sortBy, sortAscending
    ) {
        hours
            .filter { hourData ->
                val forecast = hourData.forecastEntry ?: return@filter false
                val wind = forecast.wind_speed ?: 0.0
                val cloud = forecast.cloud_area_fraction ?: 0.0
                val score = hourData.viabilityScore.toFloat()
                val rain = forecast.precipitation_amount ?: 0.0
                val temp = forecast.air_temperature ?: 0.0
                val humidity = forecast.relative_humidity ?: 0.0

                val windOk = wind >= windRange.start && wind <= windRange.endInclusive
                val cloudOk = cloud >= cloudRange.start && cloud <= cloudRange.endInclusive
                val scoreOk = score >= scoreRange.start && score <= scoreRange.endInclusive
                val rainOk = rain >= rainRange.start && rain <= rainRange.endInclusive
                val tempOk = temp >= tempRange.start && temp <= tempRange.endInclusive
                val humidityOk = humidity >= humidityRange.start && humidity <= humidityRange.endInclusive

                windOk && cloudOk && scoreOk && rainOk && tempOk && humidityOk
            }
            .let { filtered ->
                when (sortBy) {
                    SortOption.TIME -> if (sortAscending) filtered.sortedBy { it.utcTimestamp }
                    else filtered.sortedByDescending { it.utcTimestamp }
                    SortOption.VIABILITY -> if (sortAscending) filtered.sortedBy { it.viabilityScore }
                    else filtered.sortedByDescending { it.viabilityScore }
                    SortOption.WIND_SPEED -> if (sortAscending) filtered.sortedBy { it.forecastEntry?.wind_speed ?: Double.MAX_VALUE }
                    else filtered.sortedByDescending { it.forecastEntry?.wind_speed ?: 0.0 }
                    SortOption.CLOUD_COVERAGE -> if (sortAscending) filtered.sortedBy { it.forecastEntry?.cloud_area_fraction ?: Double.MAX_VALUE }
                    else filtered.sortedByDescending { it.forecastEntry?.cloud_area_fraction ?: 0.0 }
                    SortOption.RAIN -> if (sortAscending) filtered.sortedBy { it.forecastEntry?.precipitation_amount ?: Double.MAX_VALUE }
                    else filtered.sortedByDescending { it.forecastEntry?.precipitation_amount ?: 0.0 }
                    SortOption.TEMPERATURE -> if (sortAscending) filtered.sortedBy { it.forecastEntry?.air_temperature ?: Double.MAX_VALUE }
                    else filtered.sortedByDescending { it.forecastEntry?.air_temperature ?: 0.0 }
                    SortOption.HUMIDITY -> if (sortAscending) filtered.sortedBy { it.forecastEntry?.relative_humidity ?: Double.MAX_VALUE }
                    else filtered.sortedByDescending { it.forecastEntry?.relative_humidity ?: 0.0 }
                }
            }
    }

    val primaryColor = appColors.primary
    val backgroundColor = appColors.background
    val isDark = isDarkTheme
    val headerContentColor = appColors.onPrimary
    val headerOverlayColor = if (isDark) Color.White else appColors.onPrimaryOverlay

    Column(Modifier.fillMaxSize().background(appColors.background)) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to backgroundColor,
                                0.55f to primaryColor,
                                1f to primaryColor
                            ),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f)
                        )
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = headerContentColor,
                    containerColor = headerOverlayColor.copy(alpha = 0.15f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(headerContentColor.copy(alpha = 0.5f))),
                contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Text("Dashboard", color = headerContentColor)
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(R.drawable.arrow_right), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sort bubble
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(appColors.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "Sort",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.onSurface
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortChip(
                    label = "Time",
                    selected = sortBy == SortOption.TIME,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.TIME) sortAscending = !sortAscending
                        else { sortBy = SortOption.TIME; sortAscending = true }
                    }
                )
                SortChip(
                    label = "Viability",
                    selected = sortBy == SortOption.VIABILITY,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.VIABILITY) sortAscending = !sortAscending
                        else { sortBy = SortOption.VIABILITY; sortAscending = false }
                    }
                )
                SortChip(
                    label = "Wind",
                    selected = sortBy == SortOption.WIND_SPEED,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.WIND_SPEED) sortAscending = !sortAscending
                        else { sortBy = SortOption.WIND_SPEED; sortAscending = true }
                    }
                )
                SortChip(
                    label = "Cloud",
                    selected = sortBy == SortOption.CLOUD_COVERAGE,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.CLOUD_COVERAGE) sortAscending = !sortAscending
                        else { sortBy = SortOption.CLOUD_COVERAGE; sortAscending = true }
                    }
                )
                SortChip(
                    label = "Rain",
                    selected = sortBy == SortOption.RAIN,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.RAIN) sortAscending = !sortAscending
                        else { sortBy = SortOption.RAIN; sortAscending = true }
                    }
                )
                SortChip(
                    label = "Temp",
                    selected = sortBy == SortOption.TEMPERATURE,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.TEMPERATURE) sortAscending = !sortAscending
                        else { sortBy = SortOption.TEMPERATURE; sortAscending = true }
                    }
                )
                SortChip(
                    label = "Humidity",
                    selected = sortBy == SortOption.HUMIDITY,
                    ascending = sortAscending,
                    themeColor = primaryColor,
                    onClick = {
                        if (sortBy == SortOption.HUMIDITY) sortAscending = !sortAscending
                        else { sortBy = SortOption.HUMIDITY; sortAscending = true }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filter bubble (collapsible)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(appColors.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filtersExpanded = !filtersExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.onSurface
                )
                Icon(
                    painterResource(if (filtersExpanded) R.drawable.arrow_up else R.drawable.arrow_down),
                    contentDescription = if (filtersExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp),
                    tint = appColors.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    FilterRangeSlider(
                        label = "Score",
                        range = scoreRange,
                        onRangeChange = { scoreRange = it },
                        valueRange = 0f..100f,
                        unit = "%",
                        themeColor = primaryColor
                    )
                    FilterRangeSlider(
                        label = "Wind",
                        range = windRange,
                        onRangeChange = { windRange = it },
                        valueRange = 0f..100f,
                        unit = " m/s",
                        themeColor = primaryColor
                    )
                    FilterRangeSlider(
                        label = "Cloud",
                        range = cloudRange,
                        onRangeChange = { cloudRange = it },
                        valueRange = 0f..100f,
                        unit = "%",
                        themeColor = primaryColor
                    )
                    FilterRangeSlider(
                        label = "Rain",
                        range = rainRange,
                        onRangeChange = { rainRange = it },
                        valueRange = 0f..30f,
                        unit = " mm",
                        themeColor = primaryColor
                    )
                    FilterRangeSlider(
                        label = "Temp",
                        range = tempRange,
                        onRangeChange = { tempRange = it },
                        valueRange = -30f..60f,
                        unit = "°C",
                        themeColor = primaryColor
                    )
                    FilterRangeSlider(
                        label = "Humid",
                        range = humidityRange,
                        onRangeChange = { humidityRange = it },
                        valueRange = 0f..100f,
                        unit = "%",
                        themeColor = primaryColor
                    )
                }
            }
        }

        // Results count
        Text(
            "${filteredAndSorted.size} launch windows matching filters",
            style = MaterialTheme.typography.labelMedium,
            color = appColors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).offset(y = 2.dp)
        )

        // Hour list or empty/loading state
        when {
            hours.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            filteredAndSorted.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No launch windows match your filters",
                        color = appColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredAndSorted) { index, hourData ->
                        HourItemCard(
                            hourData = hourData,
                            themeColor = appColors.primary,
                            onClick = {
                                dashboardViewModel.selectHour(hourData)
                                onSelectTimeWindow(hourData.dayOffset + 1, hourData.localHour)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRangeSlider(
    label: String,
    range: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    themeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = appColors.onSurface,
            maxLines = 1,
            modifier = Modifier.width(48.dp).padding(end = 6.dp),
            textAlign = TextAlign.End
        )
        RangeSlider(
            value = range,
            onValueChange = onRangeChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f).widthIn(max = 140.dp).height(16.dp),
            startThumb = {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .background(themeColor, RoundedCornerShape(2.dp))
                )
            },
            endThumb = {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .background(themeColor, RoundedCornerShape(2.dp))
                )
            },
            track = { sliderState ->
                val totalRange = valueRange.endInclusive - valueRange.start
                val startFraction = (sliderState.activeRangeStart - valueRange.start) / totalRange
                val endFraction = (sliderState.activeRangeEnd - valueRange.start) / totalRange

                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val activeFraction = (endFraction - startFraction).coerceAtLeast(0.01f)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (startFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(startFraction)
                                    .height(3.dp)
                                    .background(appColors.outlineVariant)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(activeFraction)
                                .height(3.dp)
                                .background(themeColor)
                        )
                        if (endFraction < 1f) {
                            Box(
                                modifier = Modifier
                                    .weight(1f - endFraction)
                                    .height(3.dp)
                                    .background(appColors.outlineVariant)
                            )
                        }
                    }
                }
            }
        )
        Text(
            "${range.start.toInt()}-${range.endInclusive.toInt()}$unit",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = appColors.onSurface,
            maxLines = 1,
            modifier = Modifier.width(80.dp).padding(start = 6.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    ascending: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = themeColor,
            selectedLabelColor = appColors.onPrimary,
            containerColor = appColors.surface,
            labelColor = appColors.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = appColors.outline,
            selectedBorderColor = themeColor
        ),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 12.sp, maxLines = 1)
                if (selected) {
                    Spacer(Modifier.width(4.dp))
                    Text(if (ascending) "↑" else "↓", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
fun HourItemCard(
    modifier: Modifier = Modifier,
    hourData: HourData,
    themeColor: Color,
    onClick: () -> Unit
) {
    val forecast = hourData.forecastEntry ?: return

    val dateStr = formatDayLabel(hourData.dayOffset)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface)
    ) {
        val viabilityTextColor = appColors.onPrimary
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        String.format("%02d:00 - %02d:00", hourData.localHour, (hourData.localHour + 1) % 24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = appColors.onSurface
                    )
                    Text(
                        dateStr,
                        fontSize = 12.sp,
                        color = appColors.onSurfaceVariant
                    )
                }

                val circleColor = viabilityColor(hourData.viabilityScore)
                val borderColor = if (isDarkTheme) circleColor.copy(red = circleColor.red * 0.5f, green = circleColor.green * 0.5f, blue = circleColor.blue * 0.5f) else circleColor.copy(red = circleColor.red * 0.7f, green = circleColor.green * 0.7f, blue = circleColor.blue * 0.7f)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${hourData.viabilityScore}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = viabilityTextColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = appColors.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat(
                    iconRes = R.drawable.air,
                    value = "%.1f".format(forecast.wind_speed ?: 0.0),
                    unit = "m/s"
                )
                WeatherStat(
                    iconRes = R.drawable.cloud,
                    value = "${(forecast.cloud_area_fraction ?: 0.0).toInt()}",
                    unit = "%"
                )
                WeatherStat(
                    iconRes = R.drawable.rainy,
                    value = "%.1f".format(forecast.precipitation_amount ?: 0.0),
                    unit = "mm"
                )
                WeatherStat(
                    iconRes = R.drawable.thermometer_add,
                    value = "${(forecast.air_temperature ?: 0.0).toInt()}",
                    unit = "°C"
                )
            }
        }
    }
}

@Composable
private fun WeatherStat(
    iconRes: Int,
    value: String,
    unit: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = appColors.onSurfaceVariant
        )
        Text(
            "$value$unit",
            fontSize = 12.sp,
            color = appColors.onSurfaceVariant
        )
    }
}
