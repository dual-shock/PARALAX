package com.dual_shock.paralax.ui.locationselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.bindgen.Value
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dual_shock.paralax.R
import com.dual_shock.paralax.data.GeocodingDataSource
import com.dual_shock.paralax.data.NominatimResult
import com.dual_shock.paralax.data.interpol.InterpolRepository
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import com.dual_shock.paralax.ui.dashboard.ZoomControls
import com.dual_shock.paralax.ui.map.MapStyleViewModel

@Composable
fun animatedEllipsis(): String {
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }
    return ".".repeat(dotCount)
}

@Composable
fun LocationSelectionScreen(
    repository: LaunchSiteRepository,
    interpolRepository: InterpolRepository,
    mapStyleViewModel: MapStyleViewModel,
    editSiteId: Int? = null,
    onNavigateBack: () -> Unit,
    onLocationAdded: (Int) -> Unit,
    viewModel: LocationSelectionViewModel = viewModel(factory = LocationSelectionViewModel.Factory(repository, interpolRepository, editSiteId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStyle by mapStyleViewModel.selectedStyle.collectAsState()
    val themeColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    var hasMovedToEditLocation by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showInterpolHelp by remember { mutableStateOf(false) }
    var showGribHelp by remember { mutableStateOf(false) }
    var geocodingResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val geocoder = remember { GeocodingDataSource() }
    val currentHasMovedToEditLocation by rememberUpdatedState(hasMovedToEditLocation)
    val currentIsEditMode by rememberUpdatedState(uiState.isEditMode)
    val currentOriginalLat by rememberUpdatedState(uiState.originalLat)
    val currentOriginalLon by rememberUpdatedState(uiState.originalLon)
    val currentOriginalsLoaded by rememberUpdatedState(uiState.originalsLoaded)
    val flyToEditLocationRef = remember { mutableStateOf<(() -> Unit)?>(null) }
    val pinOffsetDp = 60.dp
    val pinOffsetPx = with(density) { pinOffsetDp.toPx() }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(6.0)
            center(Point.fromLngLat(10.0, 63.0))
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(mapReady, uiState.originalsLoaded, hasMovedToEditLocation) {
        if (mapReady && uiState.originalsLoaded && !hasMovedToEditLocation && uiState.isEditMode) {
            flyToEditLocationRef.value?.invoke()
        }
    }

    LaunchedEffect(searchQuery) {
        delay(400)
        if (searchQuery.length >= 3) {
            geocodingResults = geocoder.search(searchQuery)
        } else {
            geocodingResults = emptyList()
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { GenericStyle(style = Style.STANDARD) },
            scaleBar = {},
            compass = { Compass(Modifier.padding(top = if (uiState.isEditMode) 100.dp else 0.dp)) },
            logo = { Logo(Modifier.navigationBarsPadding().padding(bottom = 140.dp)) },
            attribution = { Attribution(Modifier.navigationBarsPadding().padding(bottom = 140.dp)) }
        ) {
            MapEffect(Unit) { mapView ->
                mapView.gestures.pitchEnabled = false

                mapView.mapboxMap.getStyle { style ->
                    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(selectedStyle.preset))
                    style.setStyleImportConfigProperty("basemap", "theme", Value.valueOf("faded"))
                    style.setStyleImportConfigProperty("basemap", "showPedestrianRoads", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "show3dBuildings", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "show3dTrees", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "show3dFacades", Value.valueOf(true))
                    style.setStyleImportConfigProperty("basemap", "showLandmarkIconLabels", Value.valueOf(false))
                    style.setStyleImportConfigProperty("basemap", "showIndoorLabels", Value.valueOf(false))

                    // GRIB1 boundary (Europe): 90N -30W 40S 60E
                    if (!style.styleSourceExists("grib1-boundary-source")) {
                        val grib1Boundary = LineString.fromLngLats(listOf(
                            Point.fromLngLat(-30.0, 90.0),
                            Point.fromLngLat(60.0, 90.0),
                            Point.fromLngLat(60.0, 40.0),
                            Point.fromLngLat(-30.0, 40.0),
                            Point.fromLngLat(-30.0, 90.0)
                        ))
                        style.addSource(geoJsonSource("grib1-boundary-source") { geometry(grib1Boundary) })
                        style.addLayer(lineLayer("grib1-boundary-layer", "grib1-boundary-source") {
                            lineColor("#CCCC00")
                            lineWidth(2.0)
                            lineOpacity(0.5)
                        })
                    }

                    // GRIB2 boundary (Southern Norway): 64.25N -1.45W 55.35S 14.51E
                    if (!style.styleSourceExists("grib2-boundary-source")) {
                        val grib2Boundary = LineString.fromLngLats(listOf(
                            Point.fromLngLat(-1.45, 64.25),
                            Point.fromLngLat(14.51, 64.25),
                            Point.fromLngLat(14.51, 55.35),
                            Point.fromLngLat(-1.45, 55.35),
                            Point.fromLngLat(-1.45, 64.25)
                        ))
                        style.addSource(geoJsonSource("grib2-boundary-source") { geometry(grib2Boundary) })
                        style.addLayer(lineLayer("grib2-boundary-layer", "grib2-boundary-source") {
                            lineColor("#00CC00")
                            lineWidth(2.0)
                            lineOpacity(0.5)
                        })
                    }
                }

                fun getPinCoordinate(): Point {
                    val mapSize = mapView.mapboxMap.getSize()
                    val pinScreenX = mapSize.width / 2.0
                    val pinScreenY = mapSize.height / 2.0 + pinOffsetPx
                    return mapView.mapboxMap.coordinateForPixel(
                        ScreenCoordinate(pinScreenX, pinScreenY)
                    )
                }

                flyToEditLocationRef.value = {
                    if (!hasMovedToEditLocation) {
                        val mapSize = mapView.mapboxMap.getSize()
                        val screenCenterY = mapSize.height / 2.0
                        val pinTipY = screenCenterY + pinOffsetPx

                        val targetZoom = 10.0
                        mapView.mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(currentOriginalLon, currentOriginalLat))
                                .zoom(targetZoom)
                                .build()
                        )

                        val centerCoord = mapView.mapboxMap.coordinateForPixel(
                            ScreenCoordinate(mapSize.width / 2.0, screenCenterY)
                        )
                        val pinTipCoord = mapView.mapboxMap.coordinateForPixel(
                            ScreenCoordinate(mapSize.width / 2.0, pinTipY)
                        )
                        val latOffset = centerCoord.latitude() - pinTipCoord.latitude()

                        val adjustedLat = currentOriginalLat + latOffset
                        mapView.mapboxMap.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(currentOriginalLon, adjustedLat))
                                .zoom(targetZoom)
                                .build()
                        )
                        viewModel.onInitialPositioningComplete(currentOriginalLat, currentOriginalLon)
                        hasMovedToEditLocation = true
                    }
                }
                mapReady = true

                mapView.mapboxMap.subscribeCameraChanged {
                    val coord = getPinCoordinate()
                    viewModel.updateLocation(coord.latitude(), coord.longitude())
                    viewModel.onCameraMoving()
                }

                mapView.mapboxMap.subscribeMapIdle {
                    val coord = getPinCoordinate()
                    viewModel.onMapIdle()
                    mapView.mapboxMap.getStyle { style ->
                        if (!style.styleSourceExists("terrain-dem")) {
                            style.addSource(
                                rasterDemSource("terrain-dem") {
                                    url("mapbox://mapbox.mapbox-terrain-dem-v1")
                                    tileSize(514)
                                    maxzoom(14)
                                }
                            )
                            style.setTerrain(terrain("terrain-dem") { exaggeration(1.0) })
                        }
                        val elevation = mapView.mapboxMap.getElevation(
                            Point.fromLngLat(coord.longitude(), coord.latitude())
                        )
                        viewModel.updateAltitude(elevation?.toInt())
                    }
                }
            }
        }


        Icon(
            painter = painterResource(R.drawable.location_selector),
            contentDescription = "Location pin",
            tint = Color.Black,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = pinOffsetDp - 24.dp)
                .size(48.dp)
        )

        ZoomControls(
            mapViewportState = mapViewportState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // Top section
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Cancel + Delete buttons + Search icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    //Spacer(Modifier.width(4.dp))
                    //Text(if (uiState.isEditMode) "Cancel Edit" else "Cancel Add")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isEditMode) {
                        OutlinedButton(
                            onClick = { viewModel.deleteLocation { onNavigateBack() } },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Red),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete_location),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Location")
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        IconButton(onClick = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) {
                                searchQuery = ""
                                geocodingResults = emptyList()
                            }
                        }) {
                            Icon(painterResource(R.drawable.search), contentDescription = "Search location", tint = Color.Black)
                        }
                    }
                }
            }

            // Search bar
            if (searchExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search location...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }

                if (geocodingResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            geocodingResults.forEach { result ->
                                Text(
                                    text = result.display_name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val point = Point.fromLngLat(result.lon.toDouble(), result.lat.toDouble())
                                            scope.launch {
                                                mapViewportState.flyTo(
                                                    CameraOptions.Builder()
                                                        .center(point)
                                                        .zoom(12.0)
                                                        .build()
                                                )
                                            }
                                            searchQuery = ""
                                            geocodingResults = emptyList()
                                            searchExpanded = false
                                        }
                                        .padding(12.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Coordinate box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.my_location),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (uiState.isEditMode) "Edited Location: " else "New Location: ",
                        fontSize = 12.sp
                    )
                    Row {
                        Text(text = "Lat: ", fontSize = 12.sp)
                        Text(text = "${"%.2f".format(uiState.lat)}°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = " - Lon: ", fontSize = 12.sp)
                        Text(text = "${"%.2f".format(uiState.lon)}°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interpol station info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(text = "Closest Interpol Station:", fontSize = 12.sp)
                        val stationName = uiState.closestInterpolStation
                        val stationText = when {
                            uiState.isSearchingInterpol -> "searching${animatedEllipsis()}"
                            stationName != null -> {
                                val distanceStr = uiState.closestInterpolDistanceKm?.let {
                                    if (it < 1) ", ${(it * 1000).toInt()}m" else ", ${"%.1f".format(it)}km"
                                } ?: ""
                                "$stationName$distanceStr"
                            }
                            else -> "none within range"
                        }
                        Text(text = stationText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.help),
                    contentDescription = "What is Interpol?",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showInterpolHelp = true },
                    tint = Color.Black
                )
            }

            // GRIB fidelity info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(text = "GRIB fidelity:", fontSize = 12.sp)
                        Text(
                            text = when (uiState.gribFidelity) {
                                GribFidelity.GRIB2 -> "GRIB2"
                                GribFidelity.GRIB1 -> "GRIB1"
                                GribFidelity.OUTSIDE_RANGE -> "outside range"
                                null -> "checking${animatedEllipsis()}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.help),
                    contentDescription = "What is GRIB?",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showGribHelp = true },
                    tint = Color.Black
                )
            }

            // Altitude info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.altitude),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(text = "Altitude:", fontSize = 12.sp)
                    Text(
                        text = if (uiState.altitude != null) "${uiState.altitude}M" else "loading${animatedEllipsis()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Name field row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.isEditMode) "Edit Name:" else "Add name:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = { Text("name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedBorderColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
            }

            // Submit button - require altitude to be loaded before allowing save
            val buttonEnabled = (!uiState.isEditMode || uiState.hasChanges) && uiState.altitude != null
            OutlinedButton(
                onClick = { viewModel.submitLocation { index -> onLocationAdded(index) } },
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (buttonEnabled) Color.Black else Color.Black.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = Color.Black.copy(alpha = 0.3f)
                )
            ) {
                Text(if (uiState.isEditMode) "Update Location" else "Add Location")
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(if (uiState.isEditMode) R.drawable.edit_location else R.drawable.add_location),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (buttonEnabled) Color.Black else Color.Black.copy(alpha = 0.3f)
                )
            }
        }

        // Interpol help popup
        if (showInterpolHelp) {
            Dialog(
                onDismissRequest = { showInterpolHelp = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "What is interpol?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Interpol is one of our data sources for atmospheric weather data, it has accurate coverage over a number of locations mostly within the MEPS area (Scandinavia and The Baltics), but we only use the data from these locations if they're within a set range, naturally, the closer the station is to your location the more accurate the data. The atmospheric wind data provided by Interpol (especially the fidelity of vertical layers) is important for the trajectory simulation",
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // GRIB help popup
        if (showGribHelp) {
            Dialog(
                onDismissRequest = { showGribHelp = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "What is grib?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "GRIB is one of our data sources for atmospheric weather data, its especially relevant for the trajectory simulation given its rich wind data at different heights. The GRIB1 coverage area (bright green border) has a resolution of 1.25 degrees, meaning it has data points every 1.25 degrees. The GRIB2 area (dark green border) has a considerably higher resolution of 0.1 degrees, making it more accurate.",
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
