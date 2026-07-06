package com.dual_shock.paralax.ui.trajectorysetup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.dual_shock.paralax.R
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import com.dual_shock.paralax.data.rocketmodels.RocketModel
import com.dual_shock.paralax.data.rocketmodels.RocketModelRepository
import com.dual_shock.paralax.ui.map.MapStyleViewModel
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfigurationsViewModel
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrajectorySetupScreen(
    repository: LaunchSiteRepository,
    rocketModelRepository: RocketModelRepository,
    location: String = "",
    day: Int = 1,
    hour: String = "",
    viabilityScore: Int = 0,
    onNavigateBack: () -> Unit = {},
    onNavigateToTrajectory: (Int) -> Unit = {},
    mapStyleViewModel: MapStyleViewModel,
    viewModel: TrajectorySetupViewModel = viewModel(factory = TrajectorySetupViewModel.Factory(repository)),
    rocketConfigViewModel: RocketConfigurationsViewModel = viewModel(
        factory = RocketConfigurationsViewModel.Companion.factory(rocketModelRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val rocketConfigState by rocketConfigViewModel.uiState.collectAsState()
    val themeColor = appColors.primary

    var showRocketPopup by remember { mutableStateOf(false) }
    var isAddMode by remember { mutableStateOf(false) }

    val primaryColor = appColors.primary
    val backgroundColor = appColors.background
    val isDark = isDarkTheme
    val headerContentColor = appColors.onPrimary
    val headerOverlayColor = if (isDark) Color.White else appColors.onPrimaryOverlay

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to backgroundColor,
                                0.55f to primaryColor,
                                1f to primaryColor
                            ),
                            start = Offset(size.width, size.height),
                            end = Offset(0f, 0f)
                        )
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
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
                Text("Simulation", color = headerContentColor)
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Model selector + config preview section
            ModelSelectorSection(
                savedModels = rocketConfigState.savedModels,
                selectedModel = rocketConfigState.selectedModel,
                onSelectModel = { rocketConfigViewModel.selectModel(it) },
                onAddModel = { isAddMode = true; showRocketPopup = true },
                onEditClick = { isAddMode = false; showRocketPopup = true },
                dryMass = rocketConfigState.dryMass,
                fuelMass = rocketConfigState.fuelMass,
                burnTime = rocketConfigState.burnTime,
                themeColor = themeColor
            )

            Spacer(Modifier.height(12.dp))

            // Launch orientation sliders
            LaunchOrientationSliders(
                launchAngle = rocketConfigState.launchAngle.toFloatOrNull() ?: 85f,
                onAngleChange = { rocketConfigViewModel.updateLaunchAngle(it.toString()) },
                direction = rocketConfigState.direction.toFloatOrNull() ?: 0f,
                onDirectionChange = { rocketConfigViewModel.updateDirection(it.toString()) },
                themeColor = themeColor
            )

            Spacer(Modifier.height(12.dp))

            // Location/time info section + map
            LaunchInfoSection(
                site = uiState.site,
                hasGrib2 = uiState.hasGrib2,
                hasInterpol = uiState.hasInterpol,
                day = day,
                hour = hour,
                viabilityScore = viabilityScore,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.height(16.dp))

            // Launch trajectory button
            Button(
                onClick = { uiState.site?.let { onNavigateToTrajectory(it.id) } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.site != null && rocketConfigViewModel.isConfigComplete()
            ) {
                Icon(
                    painterResource(R.drawable.orbit),
                    contentDescription = null,
                    tint = appColors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Launch Trajectory", color = appColors.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
        }

        // Config popup overlay
        if (showRocketPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appColors.onSurface.copy(alpha = 0.5f))
                    .clickable { showRocketPopup = false },
                contentAlignment = Alignment.Center
            ) {
                RocketConfigPopup(
                    onDismiss = { showRocketPopup = false },
                    viewModel = rocketConfigViewModel,
                    isAddMode = isAddMode,
                    themeColor = themeColor
                )
            }
        }
    }
}

@Composable
private fun LaunchInfoSection(
    site: LaunchSite?,
    hasGrib2: Boolean,
    hasInterpol: Boolean,
    day: Int,
    hour: String,
    viabilityScore: Int,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, day - 1)
    val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.time)
    val dom = cal.get(Calendar.DAY_OF_MONTH)
    val suffix = when {
        dom in 11..13 -> "th"
        dom % 10 == 1 -> "st"
        dom % 10 == 2 -> "nd"
        dom % 10 == 3 -> "rd"
        else -> "th"
    }

    val recommendation = when {
        viabilityScore >= 65 -> "Good launch window"
        viabilityScore >= 40 -> "Marginal"
        else -> "Not recommended"
    }
    val recommendationColor = when {
        viabilityScore >= 65 -> appColors.viabilityGood
        viabilityScore >= 40 -> appColors.viabilityMedium
        else -> appColors.viabilityBad
    }

    val dataSources = buildList {
        if (hasGrib2) add("GRIB2") else add("GRIB1")
        if (hasInterpol) add("Interpol")
    }.joinToString(", ")

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(site?.let { Point.fromLngLat(it.lon, it.lat) } ?: Point.fromLngLat(10.0, 63.0))
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(site) {
        site?.let {
            mapViewportState.easeTo(
                CameraOptions.Builder()
                    .zoom(12.0)
                    .center(Point.fromLngLat(it.lon, it.lat))
                    .build()
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(appColors.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Location: ${site?.name ?: "Loading..."} (${String.format("%.2f", site?.lon ?: 0.0)}, ${String.format("%.2f", site?.lat ?: 0.0)})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Altitude: ${site?.altitude?.let { "${it}m" } ?: "N/A"}    Data: $dataSources",
            fontSize = 12.sp,
            color = appColors.onSurfaceVariant,
            maxLines = 1
        )

        Spacer(Modifier.height(4.dp))

        Row {
            Text(
                text = "$monthName $dom$suffix $hour - ",
                fontSize = 12.sp,
                color = appColors.onSurfaceVariant
            )
            Text(
                text = recommendation,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = recommendationColor
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar = {},
                logo = {},
                attribution = {}
            ) {
                MapEffect(site) { mapView ->
                    mapView.mapboxMap.loadStyle("mapbox://styles/mapbox/satellite-v9")
                }

                site?.let {
                    val markerIcon = rememberIconImage(
                        key = R.drawable.location_pin_filled,
                        painter = painterResource(id = R.drawable.location_pin_filled)
                    )
                    PointAnnotation(point = Point.fromLngLat(it.lon, it.lat)) {
                        iconImage = markerIcon
                        iconSize = 1.5
                        iconAnchor = IconAnchor.BOTTOM
                    }
                }
            }
        }
    }
}

@Composable
private fun RocketModelSelector(
    savedModels: List<RocketModel>,
    selectedModel: RocketModel?,
    onSelectModel: (RocketModel) -> Unit,
    onAddModel: () -> Unit,
    onEditClick: () -> Unit,
    themeColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, themeColor)
            ) {
                Text(
                    selectedModel?.name ?: "Select model",
                    modifier = Modifier.weight(1f),
                    color = if (selectedModel != null) themeColor else appColors.onSurfaceVariant
                )
                Icon(
                    painterResource(if (expanded) R.drawable.arrow_down else R.drawable.arrow_right),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.Companion.background(appColors.surface)
            ) {
                savedModels.forEach { model ->
                    val isSelected = selectedModel?.name == model.name
                    DropdownMenuItem(
                        text = {
                            Text(
                                model.name,
                                color = if (isSelected) themeColor else appColors.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelectModel(model); expanded = false }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onAddModel,
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp),
            border = BorderStroke(1.dp, themeColor)
        ) {
            Icon(
                painterResource(R.drawable.add),
                contentDescription = "Add model",
                tint = themeColor,
                modifier = Modifier.size(20.dp)
            )
        }

        OutlinedButton(
            onClick = onEditClick,
            enabled = selectedModel != null,
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp),
            border = BorderStroke(1.dp, if (selectedModel != null) themeColor else appColors.onSurfaceVariant)
        ) {
            Icon(
                painterResource(R.drawable.edit),
                contentDescription = "Edit model",
                tint = if (selectedModel != null) themeColor else appColors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModelSelectorSection(
    savedModels: List<RocketModel>,
    selectedModel: RocketModel?,
    onSelectModel: (RocketModel) -> Unit,
    onAddModel: () -> Unit,
    onEditClick: () -> Unit,
    dryMass: String,
    fuelMass: String,
    burnTime: String,
    themeColor: Color
) {
    val totalMass = (dryMass.toDoubleOrNull() ?: 0.0) + (fuelMass.toDoubleOrNull() ?: 0.0)
    val burnTimeVal = burnTime.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        RocketModelSelector(
            savedModels = savedModels,
            selectedModel = selectedModel,
            onSelectModel = onSelectModel,
            onAddModel = onAddModel,
            onEditClick = onEditClick,
            themeColor = themeColor
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Total Mass: ${String.format("%.1f", totalMass)} kg",
                fontSize = 11.sp,
                color = appColors.onSurfaceVariant
            )
            Text(
                "Burn Time: ${String.format("%.1f", burnTimeVal)}s",
                fontSize = 11.sp,
                color = appColors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RocketConfigPopup(
    onDismiss: () -> Unit,
    viewModel: RocketConfigurationsViewModel,
    isAddMode: Boolean,
    themeColor: Color
) {
    val uiState by viewModel.uiState.collectAsState()

    var modelName by remember { mutableStateOf(if (isAddMode) "" else uiState.selectedModel?.name ?: "") }
    var dryMass by remember { mutableStateOf(uiState.dryMass) }
    var fuelMass by remember { mutableStateOf(uiState.fuelMass) }
    var thrust by remember { mutableStateOf(uiState.thrust) }
    var burnTime by remember { mutableStateOf(uiState.burnTime) }
    var dragCoefficient by remember { mutableStateOf(uiState.dragCoefficient) }
    var diameter by remember { mutableStateOf(uiState.diameter) }
    var firstChuteReleaseHeight by remember { mutableStateOf(uiState.firstChuteReleaseHeight) }
    var firstChuteTerminalVelocity by remember { mutableStateOf(uiState.firstChuteTerminalVelocity) }
    var firstChuteAtApogee by remember { mutableStateOf(uiState.firstChuteAtApogee) }
    var secondChuteReleaseHeight by remember { mutableStateOf(uiState.secondChuteReleaseHeight) }
    var secondChuteTerminalVelocity by remember { mutableStateOf(uiState.secondChuteTerminalVelocity) }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.85f)
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isAddMode) "New Model" else "Edit Configuration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.close), "Close", tint = appColors.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name *") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dryMass,
                    onValueChange = { dryMass = it },
                    label = { Text("Dry Mass (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fuelMass,
                    onValueChange = { fuelMass = it },
                    label = { Text("Fuel Mass (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = thrust,
                    onValueChange = { thrust = it },
                    label = { Text("Thrust (N)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = burnTime,
                    onValueChange = { burnTime = it },
                    label = { Text("Burn Time (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dragCoefficient,
                    onValueChange = { dragCoefficient = it },
                    label = { Text("Drag Coefficient (Cd)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = diameter,
                    onValueChange = { diameter = it },
                    label = { Text("Diameter (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
                Text("First Parachute", fontWeight = FontWeight.Medium, color = appColors.onSurface, modifier = Modifier.padding(bottom = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Release at Apogee", color = appColors.onSurface)
                    Switch(
                        checked = firstChuteAtApogee,
                        onCheckedChange = { firstChuteAtApogee = it }
                    )
                }

                if (!firstChuteAtApogee) {
                    OutlinedTextField(
                        value = firstChuteReleaseHeight,
                        onValueChange = { firstChuteReleaseHeight = it },
                        label = { Text("Release Height (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = firstChuteTerminalVelocity,
                    onValueChange = { firstChuteTerminalVelocity = it },
                    label = { Text("Terminal Velocity (m/s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )

                Text("Second Parachute", fontWeight = FontWeight.Medium, color = appColors.onSurface, modifier = Modifier.padding(bottom = 8.dp))

                OutlinedTextField(
                    value = secondChuteReleaseHeight,
                    onValueChange = { secondChuteReleaseHeight = it },
                    label = { Text("Release Height (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = secondChuteTerminalVelocity,
                    onValueChange = { secondChuteTerminalVelocity = it },
                    label = { Text("Terminal Velocity (m/s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    viewModel.updateDryMass(dryMass)
                    viewModel.updateFuelMass(fuelMass)
                    viewModel.updateThrust(thrust)
                    viewModel.updateBurnTime(burnTime)
                    viewModel.updateDragCoefficient(dragCoefficient)
                    viewModel.updateDiameter(diameter)
                    viewModel.updateFirstChuteReleaseHeight(firstChuteReleaseHeight)
                    viewModel.updateFirstChuteTerminalVelocity(firstChuteTerminalVelocity)
                    if (firstChuteAtApogee != uiState.firstChuteAtApogee) viewModel.toggleFirstChuteAtApogee()
                    viewModel.updateSecondChuteReleaseHeight(secondChuteReleaseHeight)
                    viewModel.updateSecondChuteTerminalVelocity(secondChuteTerminalVelocity)

                    if (isAddMode && modelName.isNotBlank()) {
                        viewModel.addModel(modelName)
                    } else if (!isAddMode && modelName.isNotBlank()) {
                        viewModel.updateModel(uiState.selectedModel?.name ?: "", modelName)
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp),
                enabled = modelName.isNotBlank() &&
                    dryMass.toDoubleOrNull() != null &&
                    fuelMass.toDoubleOrNull() != null &&
                    thrust.toDoubleOrNull() != null &&
                    burnTime.toDoubleOrNull() != null &&
                    dragCoefficient.toDoubleOrNull() != null &&
                    diameter.toDoubleOrNull() != null
            ) {
                Text(
                    if (isAddMode) "Save Model" else "Apply Changes",
                    fontWeight = FontWeight.Bold,
                    color = appColors.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchOrientationSliders(
    launchAngle: Float,
    onAngleChange: (Float) -> Unit,
    direction: Float,
    onDirectionChange: (Float) -> Unit,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Angle:", fontSize = 11.sp, color = appColors.onSurfaceVariant, maxLines = 1, modifier = Modifier.width(64.dp))

            Slider(
                value = 90f - launchAngle,
                onValueChange = { onAngleChange(90f - it) },
                valueRange = 0f..89f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = appColors.outlineVariant
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(themeColor, RoundedCornerShape(2.dp))
                    )
                },
                track = { sliderState ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(appColors.outlineVariant)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderState.value / 89f)
                                .height(3.dp)
                                .background(themeColor)
                        )
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.width(58.dp)
            ) {
                Text(
                    "${launchAngle.toInt()}°",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.onSurface,
                    maxLines = 1,
                    modifier = Modifier.width(36.dp)
                )
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 180f - launchAngle }
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Heading:", fontSize = 11.sp, color = appColors.onSurfaceVariant, maxLines = 1, modifier = Modifier.width(64.dp))

            Slider(
                value = direction,
                onValueChange = onDirectionChange,
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = appColors.outlineVariant
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(themeColor, RoundedCornerShape(2.dp))
                    )
                },
                track = { sliderState ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(appColors.outlineVariant)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderState.value / 360f)
                                .height(3.dp)
                                .background(themeColor)
                        )
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.width(58.dp)
            ) {
                Text(
                    "${direction.toInt()}°",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.onSurface,
                    maxLines = 1,
                    modifier = Modifier.width(36.dp)
                )
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 90f + direction }
                )
            }
        }
    }
}
