package com.dual_shock.paralax.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dual_shock.paralax.R
import com.dual_shock.paralax.model.settings.STANDARD_ATMOSPHERIC_ALTITUDES
import com.dual_shock.paralax.model.settings.STANDARD_ATMOSPHERIC_ALTITUDES_KM
import com.dual_shock.paralax.model.settings.WindKeyframe
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    onToggleTheme: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val themeColor = appColors.primary
    val backgroundColor = appColors.background
    val isDark = isDarkTheme
    val headerContentColor = appColors.onPrimary
    val headerOverlayColor = if (isDark) Color.White else appColors.onPrimaryOverlay

    Column(modifier = Modifier.fillMaxSize().background(appColors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to themeColor,
                                0.45f to themeColor,
                                1f to backgroundColor
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
            OutlinedButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = headerContentColor,
                    containerColor = headerOverlayColor.copy(alpha = 0.15f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(headerContentColor.copy(alpha = 0.5f))),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Icon(painterResource(R.drawable.arrow_left), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dashboard", color = headerContentColor)
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    painterResource(if (isDark) R.drawable.lightmode else R.drawable.darkmode),
                    contentDescription = "Toggle theme",
                    tint = if (isDark) Color(0xFF00BCD4) else Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = MaterialTheme.shapes.large,
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
//                ) {
//
//                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
//
//                        Text(
//                            text = "General",
//                            fontSize = 11.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Color(0xFF999999),
//                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 20.dp),
//                            thickness = 0.5.dp,
//                            color = appColors.outlineVariant
//                        )
//                    }
//                }
//            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {

                        Text(
                            text = "weather viability targets and thresholds",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                        )

                        SettingRowInputField(
                            label = "Maximum Temperature",
                            defaultValue = settings.maxTemperature,
                            unit = "°C",
                            icon = painterResource(R.drawable.thermometer_add),
                            minValue = -10.0,
                            maxValue = 60.0,
                            allowNegativeNumber = true,
                            onSaveSetting = {
                                if (it <= settings.minTemperature) viewModel.saveSettings(settings.copy(minTemperature = settings.minTemperature + 1))
                                else viewModel.saveSettings(settings.copy(maxTemperature = it))                            }
                        )

                        SettingRowInputField(
                            label = "Minimum Temperature",
                            defaultValue = settings.minTemperature,
                            unit = "°C",
                            icon = painterResource(R.drawable.thermometer_minus),
                            minValue = -30.0,
                            maxValue = 40.0,
                            allowNegativeNumber = true,
                            onSaveSetting = {
                                if (it >= settings.maxTemperature) viewModel.saveSettings(settings.copy(minTemperature = settings.maxTemperature - 1))
                                else viewModel.saveSettings(settings.copy(minTemperature = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Dew Point Temperature",
                            defaultValue = settings.maxDewPoint,
                            unit = "°C",
                            minValue = -50.0,
                            maxValue = 50.0,
                            allowNegativeNumber = true,
                            icon = painterResource(R.drawable.cloud),
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxDewPoint = it))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = appColors.outlineVariant
                        )

                        SettingRowInputField(
                            label = "Maximum Precipitation",
                            defaultValue = settings.maxPrecipitation,
                            unit = "mm",
                            minValue = 0.0,
                            maxValue = 30.0,
                            icon = painterResource(R.drawable.rainy),
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxPrecipitation = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Humidity",
                            defaultValue = settings.maxHumidity,
                            unit = "%",
                            icon = painterResource(R.drawable.humidity_low),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxHumidity = it))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = appColors.outlineVariant
                        )

                        SettingRowInputField(
                            label = "Cloud Coverage (Low Altitude)",
                            defaultValue = settings.maxCloudCoverLow,
                            unit = "%",
                            icon = painterResource(R.drawable.cloud),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxCloudCoverLow = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Cloud Coverage (High Altitude)",
                            defaultValue = settings.maxCloudCoverMediumHigh,
                            unit = "%",
                            icon = painterResource(R.drawable.cloud),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxCloudCoverMediumHigh = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Fog Coverage",
                            defaultValue = settings.maxFogCover,
                            unit = "%",
                            icon = painterResource(R.drawable.foggy),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxFogCover = it))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = appColors.outlineVariant
                        )

                        SettingRowInputField(
                            label = "Maximum Wind Speed (Altitude)",
                            defaultValue = settings.maxWindSpeedAltitude,
                            unit = "m/s",
                            icon = painterResource(R.drawable.air),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxWindSpeedAltitude = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Maximum Wind Speed (Ground)",
                            defaultValue = settings.maxWindSpeedGround,
                            unit = "m/s",
                            icon = painterResource(R.drawable.air),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxWindSpeedGround = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Maximum Shear Wind Speed",
                            defaultValue = settings.maxShearWind,
                            unit = "m/s",
                            icon = painterResource(R.drawable.compare_arrows),
                            minValue = 0.0,
                            maxValue = 100.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxShearWind = it))
                            }
                        )

                        SettingRowInputField(
                            label = "Minimum Air Pressure",
                            defaultValue = settings.minAirPressure,
                            unit = "Pa",
                            icon = painterResource(R.drawable.airwave),
                            minValue = 0.0,
                            maxValue = 2000.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(minAirPressure = it))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = appColors.outlineVariant
                        )

                        SettingRowInputField(
                            label = "Maximum altitude",
                            defaultValue = settings.maxAltitude,
                            unit = "m",
                            icon = painterResource(R.drawable.altitude),
                            minValue = 0.0,
                            maxValue = 20000.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(maxAltitude = it))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = appColors.outlineVariant
                        )

                        SettingRowInputField(
                            label = "Danger Zone Radius",
                            defaultValue = settings.dangerZoneRadiusM,
                            unit = "m",
                            icon = painterResource(R.drawable.warning),
                            minValue = 100.0,
                            maxValue = 100000.0,
                            onSaveSetting = {
                                viewModel.saveSettings(settings.copy(dangerZoneRadiusM = it))
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    WindDirectionKeyframeList(
                        keyframes = settings.keyframes,
                        onKeyframeChange = { index, updated ->
                            val newList = settings.keyframes.toMutableList().apply { this[index] = updated }
                            viewModel.saveSettings(settings.copy(keyframes = newList))
                        },
                        onAddKeyframe = {
                            val usedAltitudes = settings.keyframes
                                .map { kotlin.math.round(it.altitudeKm * 100).toInt() }
                                .toSet()

                            val nextAltitude = STANDARD_ATMOSPHERIC_ALTITUDES_KM
                                .filter { kotlin.math.round(it * 100).toInt() !in usedAltitudes }
                                .minOrNull()

                            if (nextAltitude != null) {
                                val existing = settings.keyframes.sortedBy { it.altitudeKm }
                                val newAltitudeKm = nextAltitude.toFloat()
                                val nearest = existing.minByOrNull { kotlin.math.abs(it.altitudeKm - newAltitudeKm) }

                                val newKeyframe = WindKeyframe(
                                    altitudeKm = newAltitudeKm,
                                    preferredDeg = nearest?.preferredDeg ?: 90f,
                                    weight = nearest?.weight ?: 5f
                                )

                                viewModel.saveSettings(settings.copy(keyframes = (existing + newKeyframe).sortedBy { it.altitudeKm }))
                            }
                        }
                        ,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        }
}


/**
 * One altitude checkpoint in the wind-direction profile.
 * Direction/weight at altitudes between keyframes should be interpolated
 * circularly (see WindProfileInterpolation) — not stored, only computed at scoring time.
 */


private val compassLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

private fun compassLabel(deg: Float): String =
    compassLabels[((deg / 45f).roundToInt()) % 8]

@Composable
fun WindDirectionKeyframeList(
    keyframes: List<WindKeyframe>,
    onKeyframeChange: (index: Int, updated: WindKeyframe) -> Unit,
    onAddKeyframe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openIndex by remember { mutableIntStateOf(keyframes.lastIndex.coerceAtLeast(0)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Wind direction by altitude",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Set favored direction at a few altitudes. Values between them are interpolated.",
            style = MaterialTheme.typography.bodySmall,
            color = appColors.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        keyframes.forEachIndexed { index, keyframe ->
            KeyframeRow(
                keyframe = keyframe,
                isOpen = openIndex == index,
                onToggle = { openIndex = if (openIndex == index) -1 else index },
                onChange = { updated -> onKeyframeChange(index, updated) }
            )
        }

        TextButton(
            onClick = onAddKeyframe,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Add altitude point")
        }
    }
}

@Composable
private fun KeyframeRow(
    keyframe: WindKeyframe,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onChange: (WindKeyframe) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniCompass(degrees = keyframe.preferredDeg, sizeDp = 28.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("${"%.2f".format(keyframe.altitudeKm)} km", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${compassLabel(keyframe.preferredDeg)} favored · strength ${keyframe.weight.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_down),
                contentDescription = if (isOpen) "Collapse" else "Expand",
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isOpen) 180f else 0f)
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                CompassDial(
                    degrees = keyframe.preferredDeg,
                    sizeDp = 88.dp,
                    onDegreesChange = { newDeg -> onChange(keyframe.copy(preferredDeg = newDeg)) }
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Strength", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = keyframe.weight,
                        onValueChange = { onChange(keyframe.copy(weight = it)) },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }
            }
        }
    }
}

/** Static, non-interactive compass glyph used in the collapsed row summary. */
@Composable
private fun MiniCompass(degrees: Float, sizeDp: androidx.compose.ui.unit.Dp) {
    val accent = appColors.primary
    val ring = appColors.outlineVariant
    Canvas(modifier = Modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4f
        drawCircle(color = ring, radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
        val rad = Math.toRadians((degrees - 90).toDouble())
        val tip = Offset(
            center.x + radius * cos(rad).toFloat(),
            center.y + radius * sin(rad).toFloat()
        )
        drawLine(color = accent, start = center, end = tip, strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(color = accent, radius = 3f, center = tip)
    }
}

/** Draggable compass dial — drag or tap anywhere on the ring to set the bearing. */
@Composable
private fun CompassDial(
    degrees: Float,
    sizeDp: androidx.compose.ui.unit.Dp,
    onDegreesChange: (Float) -> Unit
) {
    val accent = appColors.primary
    val ring = appColors.outlineVariant
    val muted = appColors.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .size(sizeDp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = angleFromCenter(center, change.position)
                    onDegreesChange(angle)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    onDegreesChange(angleFromCenter(center, tapOffset))
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8f

        drawCircle(color = ring, radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))

        listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, deg) ->
            val rad = Math.toRadians((deg - 90).toDouble())
            val labelPos = Offset(
                center.x + (radius + 12f) * cos(rad).toFloat(),
                center.y + (radius + 12f) * sin(rad).toFloat()
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                labelPos.x,
                labelPos.y,
                android.graphics.Paint().apply {
                    color = muted.toArgb()
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        val rad = Math.toRadians((degrees - 90).toDouble())
        val tip = Offset(
            center.x + radius * cos(rad).toFloat(),
            center.y + radius * sin(rad).toFloat()
        )
        drawLine(color = accent, start = center, end = tip, strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(color = accent, radius = 5f, center = tip)
        drawCircle(color = muted, radius = 2.5f, center = center)
    }
}

private fun angleFromCenter(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    var deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
    if (deg < 0f) deg += 360f
    return deg
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt()
)
@Composable
fun SettingRowInputField(
    label: String = "Label",
    defaultValue: Double = 0.0,
    unit: String = "units",
    maxValue: Double = 100.0,
    minValue: Double = 0.0,
    allowNegativeNumber: Boolean = false,
    icon: Painter = painterResource(R.drawable.foggy),
    onSaveSetting: (Double) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var text by remember(defaultValue) { mutableStateOf(defaultValue.toString()) }
    var wasFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            if(newValue.isEmpty()
                || (newValue.matches(Regex("^\\d*\\.?\\d*$")) && !allowNegativeNumber)
                || (newValue.matches(Regex("^-?\\d*\\.?\\d*$")) && allowNegativeNumber)) {
                text = newValue
            }
        },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = appColors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        suffix = { Text(unit, color = appColors.onSurfaceVariant) },
        supportingText = {
            Text(
                text = "${minValue.toInt()} - ${maxValue.toInt()} $unit",
                fontSize = 10.sp
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            if (text.isEmpty()) text = defaultValue.toString()
            else if (text.toDouble() < minValue) text = minValue.toString()
            else if (text.toDouble() > maxValue) text = maxValue.toString()
            onSaveSetting(text.toDouble())
            focusManager.clearFocus()
        }),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = appColors.primary,
            unfocusedBorderColor = appColors.outlineVariant,
            focusedTextColor = appColors.onSurface,
            unfocusedTextColor = appColors.onSurface,
            cursorColor = appColors.primary,
            unfocusedContainerColor = appColors.surface,
            focusedContainerColor = appColors.surface
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused) {
                    if (text.isEmpty()) text = defaultValue.toString()
                    else if (text.toDoubleOrNull() != null) {
                        if (text.toDouble() < minValue) text = minValue.toString()
                        else if (text.toDouble() > maxValue) text = maxValue.toString()
                    }
                    text.toDoubleOrNull()?.let { onSaveSetting(it) }
                }
                wasFocused = focusState.isFocused
            }
    )
}

