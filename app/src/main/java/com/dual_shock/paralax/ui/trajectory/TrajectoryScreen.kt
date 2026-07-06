package com.dual_shock.paralax.ui.trajectory


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.ModelElevationReference
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.dual_shock.paralax.R
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.model.trajectory.TrajectoryPoint
import com.dual_shock.paralax.ui.map.MapStyleViewModel
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfigurationsViewModel
import com.dual_shock.paralax.ui.settings.SettingsViewModel
import com.dual_shock.paralax.ui.theme.AppFontFamily
import com.dual_shock.paralax.ui.theme.appColors
import com.dual_shock.paralax.ui.theme.isDarkTheme
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt


private fun trajectoryFilterValue(maxIndex: Int): Value {
    return Value.fromJson(
        """["<=", ["get", "trajectory_index"], $maxIndex]"""
    ).value!!
}



enum class GradientDisplayMode { HEIGHT, SPEED }

@Composable
fun TrajectoryScreen(
    viewModel: TrajectoryViewModel,
    rocketConfigViewModel: RocketConfigurationsViewModel,
    settingsViewModel: SettingsViewModel,
    mapStyleViewModel: MapStyleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val rocketConfigState by rocketConfigViewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val isConfigComplete = rocketConfigViewModel.isConfigComplete()
    val selectedStyle by mapStyleViewModel.selectedStyle.collectAsState()
    val themeColor = appColors.primary

    val primaryColor = appColors.primary
    val backgroundColor = appColors.background
    val isDark = isDarkTheme
    val headerContentColor = appColors.onPrimary
    val headerOverlayColor = if (isDark) Color.White else appColors.onPrimaryOverlay

    val siteMarkerImage = rememberIconImage(
        key = R.drawable.location_pin_filled,
        painter = painterResource(id = R.drawable.location_pin_filled)
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to backgroundColor,
                                0.45f to primaryColor,
                                1f to primaryColor
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height)
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
            OutlinedButton(
                onClick = onNavigateToSetup,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = headerContentColor,
                    containerColor = headerOverlayColor.copy(alpha = 0.15f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(headerContentColor.copy(alpha = 0.5f))),
                contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Text("Setup Simulation", color = headerContentColor)
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(R.drawable.arrow_right), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
            }
        }

        val site = uiState.site

        when {
            site != null -> Box(modifier = Modifier.fillMaxSize()) {
                TrajectoryMapContent(
                    site = site,
                    trajectoryPoints = uiState.trajectoryPoints,
                    mode = uiState.mode,
                    markerIcon = siteMarkerImage,
                    isConfigComplete = isConfigComplete,
                    altitudeThreshold = settings.maxAltitude,
                    selectedStylePreset = selectedStyle.preset,
                    dangerZoneRadiusKm = settings.dangerZoneRadiusM / 1000.0
                )

                val hasGrib = uiState.hasGrib
                val hasInterpol = uiState.hasInterpol
                val isTrajectoryLoading = uiState.trajectoryPoints.isEmpty() && uiState.mode != null

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.Companion
                                .background(appColors.surface, RoundedCornerShape(999.dp))
                                .border(1.dp, themeColor.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        listOf(
                            Triple(TrajectoryMode.Grib, "GRIB", hasGrib),
                            Triple(TrajectoryMode.Average, "Average", hasGrib && hasInterpol),
                            Triple(TrajectoryMode.Interpol, "Interpol", hasInterpol)
                        ).forEach { (mode, label, isAvailable) ->
                            val isSelected = uiState.mode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSelected && isAvailable) themeColor else Color.Transparent)
                                    .then(if (isAvailable) Modifier.clickable { viewModel.setMode(mode) } else Modifier)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = when {
                                        !isAvailable -> appColors.onPrimaryMuted.copy(alpha = 0.4f)
                                        isSelected -> appColors.onPrimary
                                        else -> appColors.onPrimaryMuted
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected && isAvailable) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                        }
                    }

                    if (isTrajectoryLoading) {
                        var rotation by remember { mutableFloatStateOf(0f) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(16)
                                rotation = (rotation + 3f) % 360f
                            }
                        }
                        Icon(
                            painter = painterResource(R.drawable.loading),
                            contentDescription = "Loading",
                            tint = themeColor,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-44).dp)
                                .size(36.dp)
                                .rotate(rotation)
                        )
                    }
                }

                if (!isConfigComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(appColors.onSurface.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToSetup,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = headerContentColor,
                                containerColor = headerOverlayColor.copy(alpha = 0.15f)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(headerContentColor.copy(alpha = 0.5f))),
                            contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text("Rocket configuration not complete", color = headerContentColor)
                            Spacer(Modifier.width(6.dp))
                            Icon(painterResource(R.drawable.arrow_right), contentDescription = null, tint = headerContentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            uiState.isLoading -> Text("Loading map...", modifier = Modifier.padding(16.dp))
            uiState.error != null -> Text("Error: ${uiState.error}", modifier = Modifier.padding(16.dp))
        }
    }
}


@OptIn(MapboxExperimental::class)
@Composable
private fun TrajectoryMapContent(
    site: LaunchSite,
    trajectoryPoints: List<TrajectoryPoint>,
    mode: TrajectoryMode?,
    markerIcon: IconImage?,
    isConfigComplete: Boolean,
    altitudeThreshold: Double,
    selectedStylePreset: String,
    dangerZoneRadiusKm: Double
) {
    var isAnimating by remember { mutableStateOf(false) }
    var gradientMode by remember { mutableStateOf(GradientDisplayMode.HEIGHT) }
    var is3DMode by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    var trajectoryAnimationJob by remember { mutableStateOf<Job?>(null) }

    var rocketClickLayerIdForTap by remember { mutableStateOf<String?>(null) }



    val apogee = trajectoryPoints.maxOfOrNull { it.altitude } ?: 6500.0
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(calculateCameraZoom(apogee))
            center(Point.fromLngLat(site.lon, site.lat + calculateCameraLatOffset(apogee)))
            pitch(67.5)
            bearing(0.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { GenericStyle(style = Style.STANDARD) },
            scaleBar = {},
            logo = {},
            attribution = {}
        ) {
            // Camera positioning based on trajectory
            MapEffect(trajectoryPoints) { mapView ->
                if (trajectoryPoints.isNotEmpty()) {
                    val newApogee = trajectoryPoints.maxOf { it.altitude }
                    Log.d("testing", "Camera update: apogee=$newApogee, zoom=${calculateCameraZoom(newApogee)}, offset=${calculateCameraLatOffset(newApogee)}")
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .zoom(calculateCameraZoom(newApogee))
                            .center(Point.fromLngLat(site.lon, site.lat + calculateCameraLatOffset(newApogee)))
                            .pitch(67.5)
                            .bearing(0.0)
                            .build()
                    )
                }
            }
            // 3-D terrain + style config
            MapEffect(selectedStylePreset) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    // Standard style configuration
                    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(selectedStylePreset))
                    style.setStyleImportConfigProperty("basemap", "theme", Value.valueOf("faded"))
                    style.setStyleImportConfigProperty("basemap", "showPedestrianRoads", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showPlaceLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(false))

                    if (!style.styleSourceExists("terrain-dem")) {
                        style.addSource(
                            rasterDemSource("terrain-dem") {
                                url("mapbox://mapbox.mapbox-terrain-dem-v1")
                                tileSize(514)
                                maxzoom(14)
                            }
                        )
                        style.setTerrain(terrain("terrain-dem") { exaggeration(1.5) })
                    }
                }
            }

            // 2D/3D mode toggle - update camera
            MapEffect(is3DMode, trajectoryPoints) { mapView ->
                if (trajectoryPoints.isEmpty()) return@MapEffect

                if (is3DMode) {
                    // 3D mode: use apogee-based camera positioning
                    val apogee = trajectoryPoints.maxOf { it.altitude }
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(site.lon, site.lat + calculateCameraLatOffset(apogee)))
                            .zoom(calculateCameraZoom(apogee))
                            .pitch(67.5)
                            .bearing(0.0)
                            .build()
                    )
                } else {
                    // 2D mode: center between launch and landing, zoom to fit trajectory
                    val firstPoint = trajectoryPoints.first()
                    val lastPoint = trajectoryPoints.last()
                    val centerLat = (firstPoint.lat + lastPoint.lat) / 2
                    val centerLon = (firstPoint.lon + lastPoint.lon) / 2

                    val latSpread = trajectoryPoints.maxOf { it.lat } - trajectoryPoints.minOf { it.lat }
                    val lonSpread = trajectoryPoints.maxOf { it.lon } - trajectoryPoints.minOf { it.lon }
                    val maxSpread = maxOf(latSpread, lonSpread)
                    val zoom = (15.0 - ln(maxSpread * 500 + 1) * 1.3).coerceIn(9.0, 15.0)

                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(centerLon, centerLat))
                            .zoom(zoom)
                            .pitch(0.0)
                            .bearing(0.0)
                            .build()
                    )
                }
            }

            // Camera debug logging
            MapEffect(site) { mapView ->
                mapView.mapboxMap.subscribeCameraChanged {
                    val cam = mapView.mapboxMap.cameraState
                    val lonOffset = cam.center.longitude() - site.lon
                    val latOffset = cam.center.latitude() - site.lat
                    Log.d("testing", "CAMERA: zoom=%.2f, offset=(lon%+.4f, lat%+.4f), pitch=%.1f, bearing=%.1f".format(
                        cam.zoom, lonOffset, latOffset, cam.pitch, cam.bearing
                    ))
                }
            }

            if (trajectoryPoints.isNotEmpty()) {

                MapEffect(trajectoryPoints, isAnimating, gradientMode, altitudeThreshold, dangerZoneRadiusKm, selectedStylePreset) { mapView ->

                    val isDarkMapMode = selectedStylePreset == "night" || selectedStylePreset == "dusk"
                    val textColor = if (isDarkMapMode) "#FFFFFF" else "#2A211A"
                    val textHaloColor = if (isDarkMapMode) "#000000" else "#F2EADB"
                    val lineColor = if (isDarkMapMode) "#FFFFFF" else "#2A211A"

                    trajectoryAnimationJob?.cancel()

                    trajectoryAnimationJob = coroutineScope.launch(Dispatchers.Main.immediate) launch@{

                        val style = suspendCancellableCoroutine { cont ->
                            mapView.mapboxMap.getStyle { s ->
                                if (cont.isActive) cont.resume(s)
                            }
                        }


                        val layerIdsToRemove = style.styleLayers
                            .map { it.id }
                            .filter { id ->
                                id == "trajectory-layer" ||
                                        id == "rocket-layer" ||
                                        id == "parachute-layer" ||
                                        id.startsWith("trajectory-layer-") ||
                                        id.startsWith("rocket-layer-") ||
                                        id.startsWith("parachute-layer-") ||
                                        id.startsWith("rocket-click-layer-") ||
                                        id.startsWith("danger-zone-layer-") ||
                                        id.startsWith("landing-label-layer-") ||
                                        id.startsWith("distance-line-layer-") ||
                                        id.startsWith("distance-label-layer-") ||
                                        id.startsWith("launch-name-layer-")
                            }
                            .reversed()


                        layerIdsToRemove.forEach { layerId ->
                            if (style.styleLayerExists(layerId)) {
                                style.removeStyleLayer(layerId)
                            }
                        }

                        val sourceIdsToRemove = style.styleSources
                            .map { it.id }
                            .filter { id ->
                                id == "trajectory-source" ||
                                        id == "rocket-source" ||
                                        id == "parachute-source" ||
                                        id.startsWith("trajectory-source-") ||
                                        id.startsWith("rocket-source-") ||
                                        id.startsWith("parachute-source-") ||
                                        id.startsWith("rocket-click-source-") ||
                                        id.startsWith("danger-zone-source-") ||
                                        id.startsWith("landing-label-source-") ||
                                        id.startsWith("distance-line-source-") ||
                                        id.startsWith("distance-label-source-") ||
                                        id.startsWith("launch-name-source-")
                            }


                        sourceIdsToRemove.forEach { sourceId ->
                            if (style.styleSourceExists(sourceId)) {
                                style.removeStyleSource(sourceId)
                            }
                        }

                        // ---------------------------------------------------------------------
                        // Terrain
                        // ---------------------------------------------------------------------

                        if (!style.styleSourceExists("mapbox-dem")) {
                            style.addSource(
                                rasterDemSource("mapbox-dem") {
                                    url("mapbox://mapbox.mapbox-terrain-dem-v1")
                                    tileSize(512)
                                    maxzoom(14L)
                                }
                            )
                        }

                        style.setTerrain(
                            terrain("mapbox-dem") {
                                exaggeration(1.0)
                            }
                        )

                        // ---------------------------------------------------------------------
                        // Basic calculations
                        // ---------------------------------------------------------------------

                        val firstPoint = trajectoryPoints.first()

                        val apogeeIndex = trajectoryPoints.indices.maxByOrNull { index ->
                            trajectoryPoints[index].altitude
                        } ?: 0

                        val apogeePoint = trajectoryPoints[apogeeIndex]

                        val apogee = trajectoryPoints.maxOfOrNull { it.altitude } ?: 0.0

                        val cubeSize = calculateCubeSize(apogee)

                        val cosLat = cos(Math.toRadians(firstPoint.lat)).takeIf {
                            abs(it) > 0.000001
                        } ?: 0.000001

                        val cubeSizeLon = cubeSize / cosLat
                        val verticalSize = calculateVerticalSize(apogee)

                        val altitudes = trajectoryPoints.map { it.altitude }
                        val minAlt = altitudes.minOrNull() ?: 0.0
                        val maxAlt = altitudes.maxOrNull() ?: 0.0
                        val altRange = (maxAlt - minAlt).takeIf { it > 0.0 } ?: 1.0

                        val speeds = trajectoryPoints.map { it.speed }
                        val minSpeed = speeds.minOrNull() ?: 0.0
                        val maxSpeed = speeds.maxOrNull() ?: 0.0
                        val speedRange = (maxSpeed - minSpeed).takeIf { it > 0.0 } ?: 1.0

                        val allFeatures = trajectoryPoints.map { point ->
                            buildFeature(
                                point,
                                minAlt,
                                altRange,
                                minSpeed,
                                speedRange,
                                cubeSize,
                                cubeSizeLon,
                                verticalSize,
                                gradientMode,
                                altitudeThreshold
                            )
                        }

                        if (allFeatures.isEmpty()) {
                            return@launch
                        }



                        val chunkSize = 100
                        val trajectoryChunks = allFeatures.chunked(chunkSize)

                        if (trajectoryChunks.isEmpty()) {
                            return@launch
                        }

                        val apogeeChunkIndex = (apogeeIndex / chunkSize)
                            .coerceIn(0, trajectoryChunks.lastIndex)

                        val animationDurationMs = 5000L

                        val delayPerChunk = if (trajectoryChunks.isNotEmpty()) {
                            (animationDurationMs / trajectoryChunks.size).coerceAtLeast(16L)
                        } else {
                            50L
                        }

                        val runId = System.currentTimeMillis().toString()

                        val trajectorySourceIds = trajectoryChunks.indices.map { chunkIndex ->
                            "trajectory-source-$runId-$chunkIndex"
                        }

                        val trajectoryLayerIds = trajectoryChunks.indices.map { chunkIndex ->
                            "trajectory-layer-$runId-$chunkIndex"
                        }

                        val rocketSourceId = "rocket-source-$runId"
                        val rocketLayerId = "rocket-layer-$runId"


                        val rocketClickSourceId = "rocket-click-source-$runId"
                        val rocketClickLayerId = "rocket-click-layer-$runId"

                        rocketClickLayerIdForTap = rocketClickLayerId


                        val parachuteSourceId = "parachute-source-$runId"
                        val parachuteLayerId = "parachute-layer-$runId"

                        val rocketModelId = "rocket-model"
                        val parachuteModelId = "parachute-model"

                        // ---------------------------------------------------------------------
                        // Add trajectory chunk sources/layers
                        // ---------------------------------------------------------------------

                        trajectoryChunks.forEachIndexed { chunkIndex, chunkFeatures ->

                            val sourceId = trajectorySourceIds[chunkIndex]
                            val layerId = trajectoryLayerIds[chunkIndex]

                            style.addSource(
                                geoJsonSource(sourceId) {
                                    featureCollection(
                                        FeatureCollection.fromFeatures(chunkFeatures.toList())
                                    )
                                }
                            )

                            style.addLayer(
                                fillExtrusionLayer(layerId, sourceId) {
                                    minZoom(0.0)

                                    fillExtrusionHeight(Expression.get("height"))
                                    fillExtrusionBase(Expression.get("base"))
                                    fillExtrusionColor(Expression.get("color"))
                                    fillExtrusionOpacity(0.85)
                                    if (isDarkMapMode) fillExtrusionEmissiveStrength(1.5)

                                    visibility(
                                        if (isAnimating) Visibility.NONE else Visibility.VISIBLE
                                    )
                                }
                            )
                        }

                        // ---------------------------------------------------------------------
                        // Rocket at launch site
                        // ---------------------------------------------------------------------

                        style.addSource(
                            geoJsonSource(rocketSourceId) {
                                feature(
                                    Feature.fromGeometry(
                                        Point.fromLngLat(firstPoint.lon, firstPoint.lat)
                                    )
                                )
                            }
                        )

                        runCatching {
                            style.addStyleModel(rocketModelId, "asset://saturn.glb")
                        }

                        style.addLayer(
                            modelLayer(rocketLayerId, rocketSourceId) {
                                modelId(rocketModelId)
                                modelType(ModelType.COMMON_3D)
                                modelScale(listOf(500.0, 500.0, 500.0))

                                // Rotate 90 degrees counterclockwise around vertical/Z axis
                                modelRotation(listOf(0.0, 0.0, 90.0))

                                modelElevationReference(ModelElevationReference.SEA)
                            }
                        )


                        val rocketClickFeature = Feature.fromGeometry(
                            Point.fromLngLat(firstPoint.lon, firstPoint.lat)
                        )

                        rocketClickFeature.addStringProperty("click_target", "rocket")

                        style.addSource(
                            geoJsonSource(rocketClickSourceId) {
                                feature(rocketClickFeature)
                            }
                        )

                        style.addLayer(
                            circleLayer(rocketClickLayerId, rocketClickSourceId) {
                                // Big tap area around rocket
                                circleRadius(40.0)

                                // Almost invisible but still queryable
                                circleColor("#ffffff")
                                circleOpacity(0.01)

                                visibility(Visibility.VISIBLE)
                            }
                        )

                        // Launch site name label
                        val launchNameSourceId = "launch-name-source-$runId"
                        val launchNameLayerId = "launch-name-layer-$runId"

                        val launchNameFeature = Feature.fromGeometry(
                            Point.fromLngLat(firstPoint.lon, firstPoint.lat)
                        )
                        launchNameFeature.addStringProperty("name", site.name)

                        style.addSource(
                            geoJsonSource(launchNameSourceId) {
                                feature(launchNameFeature)
                            }
                        )

                        style.addLayer(
                            symbolLayer(launchNameLayerId, launchNameSourceId) {
                                textField(Expression.get("name"))
                                textSize(14.0)
                                textFont(listOf("Open Sans Bold"))
                                textColor(textColor)
                                textHaloColor(textHaloColor)
                                textHaloWidth(1.5)
                                textAllowOverlap(true)
                                textIgnorePlacement(true)
                                textOffset(listOf(2.5, 0.0))
                            }
                        )

                        // ---------------------------------------------------------------------
                        // Parachute at apogee
                        // ---------------------------------------------------------------------

// Parachute at apogee, placed above the apogee point
// Use the actual rendered extrusion height from the apogee feature.
// This keeps the parachute aligned with your visual trajectory scaling.
                        val apogeeFeature = allFeatures[apogeeIndex]

                        val apogeeVisualHeight = if (apogeeFeature.hasProperty("height")) {
                            apogeeFeature.getNumberProperty("height").toDouble()
                        } else {
                            apogeePoint.altitude * verticalSize
                        }

// Extra upward offset so the parachute is clearly above the apogee cube.
                        val parachuteHeightOffset = 100.0

                        val parachuteZ = apogeeVisualHeight + parachuteHeightOffset

                        style.addSource(
                            geoJsonSource(parachuteSourceId) {
                                feature(
                                    Feature.fromGeometry(
                                        Point.fromLngLat(
                                            apogeePoint.lon,
                                            apogeePoint.lat
                                        )
                                    )
                                )
                            }
                        )

                        runCatching {
                            style.addStyleModel(parachuteModelId, "asset://parachute.glb")
                        }

                        style.addLayer(
                            modelLayer(parachuteLayerId, parachuteSourceId) {
                                modelId(parachuteModelId)
                                modelType(ModelType.COMMON_3D)

                                modelScale(listOf(150.0, 150.0, 150.0))

                                modelRotation(listOf(0.0, 0.0, 0.0))

                                modelElevationReference(ModelElevationReference.SEA)

                                modelTranslation(listOf(0.0, 0.0, parachuteZ))

                                visibility(Visibility.NONE)
                            }
                        )

                        // Danger zone circle at landing point
                        val lastPoint = trajectoryPoints.last()
                        val dangerZoneSourceId = "danger-zone-source-$runId"
                        val dangerZoneLayerId = "danger-zone-layer-$runId"

                        val dangerZonePolygon = createCirclePolygon(
                            lastPoint.lat,
                            lastPoint.lon,
                            dangerZoneRadiusKm
                        )

                        style.addSource(
                            geoJsonSource(dangerZoneSourceId) {
                                feature(Feature.fromGeometry(dangerZonePolygon))
                            }
                        )

                        style.addLayer(
                            fillLayer(dangerZoneLayerId, dangerZoneSourceId) {
                                fillColor("#FF0000")
                                if (isDarkMapMode) fillOpacity(0.65) else fillOpacity(0.35)
                                if (isDarkMapMode) fillEmissiveStrength(1.0)
                            }
                        )

                        // Landing coordinates label
                        val landingLabelSourceId = "landing-label-source-$runId"
                        val landingLabelLayerId = "landing-label-layer-$runId"

                        val labelFeature = Feature.fromGeometry(
                            Point.fromLngLat(lastPoint.lon, lastPoint.lat)
                        )
                        labelFeature.addStringProperty("coords", "%.4f, %.4f".format(lastPoint.lat, lastPoint.lon))

                        style.addSource(
                            geoJsonSource(landingLabelSourceId) {
                                feature(labelFeature)
                            }
                        )

                        style.addLayer(
                            symbolLayer(landingLabelLayerId, landingLabelSourceId) {
                                textField(Expression.get("coords"))
                                textSize(14.0)
                                textFont(listOf("Open Sans Bold"))
                                textColor(textColor)
                                textHaloColor(textHaloColor)
                                textHaloWidth(1.5)
                                textAllowOverlap(true)
                                textIgnorePlacement(true)
                                textOffset(listOf(0.0, 1.0))
                            }
                        )

                        // Distance line from launch to landing
                        val distanceLineSourceId = "distance-line-source-$runId"
                        val distanceLineLayerId = "distance-line-layer-$runId"

                        val lineGeometry = LineString.fromLngLats(listOf(
                            Point.fromLngLat(firstPoint.lon, firstPoint.lat),
                            Point.fromLngLat(lastPoint.lon, lastPoint.lat)
                        ))

                        style.addSource(
                            geoJsonSource(distanceLineSourceId) {
                                geometry(lineGeometry)
                            }
                        )

                        style.addLayer(
                            lineLayer(distanceLineLayerId, distanceLineSourceId) {
                                lineColor(lineColor)
                                lineWidth(2.0)
                                lineOpacity(if (isDarkMapMode) 0.9 else 0.7)
                                lineDasharray(listOf(4.0, 2.0))
                                if (isDarkMapMode) lineEmissiveStrength(1.0)
                            }
                        )

                        // Distance label at midpoint
                        val distanceLabelSourceId = "distance-label-source-$runId"
                        val distanceLabelLayerId = "distance-label-layer-$runId"

                        val midLat = (firstPoint.lat + lastPoint.lat) / 2
                        val midLon = (firstPoint.lon + lastPoint.lon) / 2

                        val R = 6371.0
                        val dLat = Math.toRadians(lastPoint.lat - firstPoint.lat)
                        val dLon = Math.toRadians(lastPoint.lon - firstPoint.lon)
                        val a = sin(dLat / 2) * sin(dLat / 2) +
                                cos(Math.toRadians(firstPoint.lat)) * cos(Math.toRadians(lastPoint.lat)) *
                                sin(dLon / 2) * sin(dLon / 2)
                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                        val distanceKm = R * c
                        val distanceText = if (distanceKm < 1) "%.0f m".format(distanceKm * 1000) else "%.2f km".format(distanceKm)

                        val distanceLabelFeature = Feature.fromGeometry(
                            Point.fromLngLat(midLon, midLat)
                        )
                        distanceLabelFeature.addStringProperty("distance", distanceText)

                        style.addSource(
                            geoJsonSource(distanceLabelSourceId) {
                                feature(distanceLabelFeature)
                            }
                        )

                        style.addLayer(
                            symbolLayer(distanceLabelLayerId, distanceLabelSourceId) {
                                textField(Expression.get("distance"))
                                textSize(14.0)
                                textFont(listOf("Open Sans Bold"))
                                textColor(textColor)
                                textHaloColor(textHaloColor)
                                textHaloWidth(1.5)
                                textAllowOverlap(true)
                                textIgnorePlacement(true)
                                textOffset(listOf(2.25, 0.0))
                            }
                        )

                        if (isAnimating) {
                            var parachuteShown = false

                            for (chunkIndex in trajectoryChunks.indices) {

                                if (!isActive) {
                                    return@launch
                                }

                                mapView.mapboxMap.getStyle { currentStyle ->

                                    // Reveal current trajectory chunk
                                    currentStyle.getLayerAs<FillExtrusionLayer>(
                                        trajectoryLayerIds[chunkIndex]
                                    )?.visibility(Visibility.VISIBLE)

                                    // Open parachute when apogee chunk is reached
                                    if (!parachuteShown && chunkIndex >= apogeeChunkIndex) {
                                        parachuteShown = true

                                        currentStyle.getLayerAs<ModelLayer>(parachuteLayerId)
                                            ?.visibility(Visibility.VISIBLE)
                                    }
                                }

                                delay(delayPerChunk)
                            }

                            mapView.mapboxMap.getStyle { currentStyle ->

                                trajectoryLayerIds.forEach { layerId ->
                                    currentStyle.getLayerAs<FillExtrusionLayer>(layerId)
                                        ?.visibility(Visibility.VISIBLE)
                                }

                                currentStyle.getLayerAs<ModelLayer>(parachuteLayerId)
                                    ?.visibility(Visibility.VISIBLE)
                            }
                        } else {

                    trajectoryLayerIds.forEach { layerId ->
                        style.getLayerAs<FillExtrusionLayer>(layerId)
                            ?.visibility(Visibility.VISIBLE)
                    }

                    style.getLayerAs<ModelLayer>(parachuteLayerId)
                        ?.visibility(Visibility.NONE)
                }

                }
                }
            }

            MapEffect(Unit) { mapView ->
                mapView.gestures.addOnMapClickListener { clickedPoint ->
                    val clickLayerId = rocketClickLayerIdForTap
                        ?: return@addOnMapClickListener false

                    val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(clickedPoint)

                    mapView.mapboxMap.queryRenderedFeatures(
                        RenderedQueryGeometry(screenCoordinate),
                        RenderedQueryOptions(listOf(clickLayerId), null)
                    ) { result ->
                        val hits = result.value
                        if (!hits.isNullOrEmpty()) {
                            if (!isAnimating) {
                                isAnimating = true
                            }
                        }
                    }
                    true
                }
            }

        }

            // ── Legend sidebar ───────────────────────────────────────────────────
        if (trajectoryPoints.isNotEmpty()) {
            TrajectoryLegend(
                trajectoryPoints = trajectoryPoints,
                gradientMode = gradientMode,
                altitudeThreshold = altitudeThreshold,
                themeColor = appColors.primary
            )
        }

        // Right-side controls: 2D/3D toggle + zoom buttons + Height/Speed toggle
        val controlIsDark = isDarkTheme
        val controlContentColor = if (controlIsDark) appColors.onPrimary else appColors.onSurface
        val controlOverlayColor = if (controlIsDark) appColors.onPrimaryOverlay else appColors.surface
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 2D/3D toggle
            Surface(
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(appColors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, appColors.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { is3DMode = !is3DMode },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (is3DMode) "2D" else "3D",
                        color = appColors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Zoom in
            Surface(
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(appColors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, appColors.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                mapViewportState.easeTo(
                                    CameraOptions.Builder()
                                        .zoom((mapViewportState.cameraState?.zoom ?: 10.0) + 1.0)
                                        .build()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = appColors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Zoom out
            Surface(
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(appColors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, appColors.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                mapViewportState.easeTo(
                                    CameraOptions.Builder()
                                        .zoom((mapViewportState.cameraState?.zoom ?: 10.0) - 1.0)
                                        .build()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", color = appColors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

        }

        // Height/Speed toggle - centered on right side
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-40).dp)
                .padding(end = 12.dp),
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .background(appColors.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, appColors.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    GradientDisplayMode.HEIGHT to "Alt.",
                    GradientDisplayMode.SPEED to "Vel."
                ).forEach { (mode, label) ->
                    val isSelected = gradientMode == mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) appColors.primary else Color.Transparent)
                            .clickable { gradientMode = mode }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) appColors.onPrimary else appColors.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

    }

}



// ── Helper to build a single extrusion Feature ───────────────────────────────
private fun buildFeature(
    point: TrajectoryPoint,
    minAlt: Double,
    altRange: Double,
    minSpeed: Double,
    speedRange: Double,
    cubeSize: Double,
    cubeSizeLon: Double,
    verticalSize: Double,
    mode: GradientDisplayMode,
    altitudeThreshold: Double
): Feature {
    val color = when (mode) {
        GradientDisplayMode.HEIGHT -> {
            if (point.altitude >= altitudeThreshold) {
                "#B73A2C"
            } else {
                val maxAlt = minAlt + altRange
                val gradientTop = if (maxAlt > altitudeThreshold) altitudeThreshold else maxAlt
                val gradientRange = (gradientTop - minAlt).coerceAtLeast(1.0)
                val t = ((point.altitude - minAlt) / gradientRange).coerceIn(0.0, 1.0)
                val hue = (270.0 - t * 270.0).toFloat()
                val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.75f, 0.925f))
                "#%06X".format(rgb and 0xFFFFFF)
            }
        }
        GradientDisplayMode.SPEED -> {
            val t = ((point.speed - minSpeed) / speedRange).coerceIn(0.0, 1.0)
            // t=0 (slowest) -> green, t=1 (fastest) -> magenta/purple
            val hue = (120.0 - t * 120.0 + if (t > 0.5) (t - 0.5) * 60.0 else 0.0).toFloat()
            val adjustedHue = if (t > 0.5) (300.0 - (1.0 - t) * 240.0).toFloat() else (120.0 - t * 120.0).toFloat()
            val finalHue = (120.0 * (1.0 - t) + 300.0 * t).toFloat() % 360f
            val rgb = android.graphics.Color.HSVToColor(floatArrayOf(finalHue, 0.75f, 0.925f))
            "#%06X".format(rgb and 0xFFFFFF)
        }
    }

    val polygon = Polygon.fromLngLats(listOf(listOf(
        Point.fromLngLat(point.lon - cubeSizeLon, point.lat - cubeSize),
        Point.fromLngLat(point.lon + cubeSizeLon, point.lat - cubeSize),
        Point.fromLngLat(point.lon + cubeSizeLon, point.lat + cubeSize),
        Point.fromLngLat(point.lon - cubeSizeLon, point.lat + cubeSize),
        Point.fromLngLat(point.lon - cubeSizeLon, point.lat - cubeSize),
    )))

    return Feature.fromGeometry(polygon, JsonObject().apply {
        addProperty("height", point.altitude + verticalSize / 2)
        addProperty("base", max(0.0, point.altitude - verticalSize / 2))
        addProperty("color", color)
    })
}

private fun calculateCubeSize(apogee: Double): Double {
    val clamped = apogee.coerceIn(100.0, 50000.0)
    val t = (ln(clamped) - ln(100.0)) / (ln(50000.0) - ln(100.0))
    return 0.000075 + t * (0.00075 - 0.000075)
}

private fun calculateVerticalSize(apogee: Double): Double {
    val clamped = apogee.coerceIn(100.0, 50000.0)
    val t = (ln(clamped) - ln(100.0)) / (ln(50000.0) - ln(100.0))
    return 10.0 + t * (175.0 - 10.0)
}

private fun calculateCameraZoom(apogee: Double): Double {
    // Logarithmic: zoom 16 at 100m, 9.4 at 15000m, extrapolates to ~8.2 at 50000m
    val clamped = apogee.coerceAtLeast(100.0)
    val t = (ln(clamped) - ln(100.0)) / (ln(15000.0) - ln(100.0))
    return 16.0 - t * (16.0 - 9.4)
}

private fun calculateCameraLatOffset(apogee: Double): Double {
    // Linear: ~0.0000115 per meter of apogee
    return apogee * 0.0000115 + 0.00025
}


// ─────────────────────────────────────────────────────────────────────────────
// Trajectory legend with mode selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrajectoryLegend(
    trajectoryPoints: List<TrajectoryPoint>,
    gradientMode: GradientDisplayMode,
    altitudeThreshold: Double,
    themeColor: Color
) {
    val altitudes = trajectoryPoints.map { it.altitude }
    val speeds = trajectoryPoints.map { it.speed }

    val peakMeters = altitudes.max().toInt()
    val groundMeters = altitudes.min().toInt()
    val midMeters = (peakMeters + groundMeters) / 2

    val maxSpeed = speeds.max()
    val minSpeed = speeds.min()
    val midSpeed = (maxSpeed + minSpeed) / 2

    fun formatAlt(m: Int): String =
        m.toString().reversed().chunked(3).joinToString("'").reversed() + "m"

    fun formatSpeed(s: Double): String = "%.0f m/s".format(s)

    val deepRed = Color(0xFFB73A2C)

    val altRange = (peakMeters - groundMeters).coerceAtLeast(1)
    val thresholdFraction = if (peakMeters > altitudeThreshold && groundMeters < altitudeThreshold) {
        ((peakMeters - altitudeThreshold) / altRange).toFloat().coerceIn(0f, 1f)
    } else null

    val heightColors = if (thresholdFraction != null) {
        val belowThresholdColors = listOf(
            Color(0xFFE26B5C),
            Color(0xFFEF965A),
            Color(0xFFE9BC44),
            Color(0xFF86B870),
            Color(0xFF7BABC7),
            Color(0xFF6B8CCE),
            Color(0xFFA080C0),
        )
        val colorStops = belowThresholdColors.mapIndexed { index, color ->
            val position = thresholdFraction + (1f - thresholdFraction) * (index.toFloat() / (belowThresholdColors.size - 1))
            position to color
        }.toMutableList()
        colorStops.add(0, 0f to deepRed)
        colorStops.add(1, thresholdFraction to deepRed)
        colorStops.sortedBy { it.first }
    } else {
        listOf(
            Color(0xFFE26B5C),
            Color(0xFFEF965A),
            Color(0xFFE9BC44),
            Color(0xFF86B870),
            Color(0xFF7BABC7),
            Color(0xFF6B8CCE),
            Color(0xFFA080C0),
        ).mapIndexed { index, color ->
            index.toFloat() / 6f to color
        }
    }

    val speedColors = listOf(
        0.00f to Color(0xFF5DEC5D),  // green (hue 120)
        0.33f to Color(0xFF5DECEC),  // cyan (hue 180)
        0.67f to Color(0xFF5D5DEC),  // blue (hue 240)
        1.00f to Color(0xFFEC5DEC),  // magenta (hue 300)
    )

    val legendColors = when (gradientMode) {
        GradientDisplayMode.HEIGHT -> heightColors
        GradientDisplayMode.SPEED -> speedColors
    }

    val legendTextColor = appColors.onSurface
    val legendShadowColor = if (isDarkTheme) appColors.surface else Color.White

    val labelStyle = TextStyle(
        fontFamily = AppFontFamily,
        color = legendTextColor,
        fontWeight = FontWeight.Bold,
        shadow = Shadow(color = legendShadowColor, offset = Offset.Zero, blurRadius = 4f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, top = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            // Gradient bar with labels on the right
            Box(
                modifier = Modifier.fillMaxHeight(0.55f)
            ) {
                Row {
                    // Gradient bar
                    Surface(
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(0.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .border(1.5.dp, appColors.outlineVariant)
                                .background(Brush.verticalGradient(colorStops = legendColors.toTypedArray()))
                        )
                    }

                    // Labels column - uses weights to position threshold/mid label correctly
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    ) {
                        if (gradientMode == GradientDisplayMode.HEIGHT && thresholdFraction != null) {
                            Spacer(modifier = Modifier.weight(thresholdFraction))
                            Text(
                                text = formatAlt(altitudeThreshold.toInt()),
                                style = labelStyle,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.weight(1f - thresholdFraction))
                        } else {
                            Spacer(modifier = Modifier.weight(0.5f))
                            Text(
                                text = when (gradientMode) {
                                    GradientDisplayMode.HEIGHT -> formatAlt(midMeters)
                                    GradientDisplayMode.SPEED -> formatSpeed(midSpeed)
                                },
                                style = labelStyle,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.weight(0.5f))
                        }
                    }
                }

                // Top label - positioned at top
                Text(
                    text = when (gradientMode) {
                        GradientDisplayMode.HEIGHT -> formatAlt(peakMeters)
                        GradientDisplayMode.SPEED -> formatSpeed(minSpeed)
                    },
                    style = labelStyle,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 32.dp)
                        .offset(y = (-8).dp)
                )

                // Bottom label - positioned at bottom
                Text(
                    text = when (gradientMode) {
                        GradientDisplayMode.HEIGHT -> formatAlt(groundMeters)
                        GradientDisplayMode.SPEED -> formatSpeed(maxSpeed)
                    },
                    style = labelStyle,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp)
                        .offset(y = 8.dp)
                )
            }
        }
    }
}

private fun createCirclePolygon(
    centerLat: Double,
    centerLon: Double,
    radiusKm: Double,
    numPoints: Int = 64
): Polygon {
    val radiusLat = radiusKm / 111.0
    val radiusLon = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))

    val points = (0..numPoints).map { i ->
        val angle = 2 * Math.PI * i / numPoints
        Point.fromLngLat(
            centerLon + radiusLon * cos(angle),
            centerLat + radiusLat * sin(angle)
        )
    }
    return Polygon.fromLngLats(listOf(points))
}
