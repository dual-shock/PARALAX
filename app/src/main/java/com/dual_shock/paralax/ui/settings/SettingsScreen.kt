package com.dual_shock.paralax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dual_shock.paralax.R
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme

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
        }
    }
}

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

