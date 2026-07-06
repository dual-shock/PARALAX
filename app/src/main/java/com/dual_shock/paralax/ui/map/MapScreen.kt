package com.dual_shock.paralax.ui.map

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dual_shock.paralax.R
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import kotlin.collections.forEach

@Composable
fun HomeMapScreen(
    repository: LaunchSiteRepository,
    onNavigateToLaunchSites: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    mapStyleViewModel: MapStyleViewModel,
    viewModel: HomeMapScreenViewModel = viewModel(factory = HomeMapScreenViewModel.Factory(repository))
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStyle by mapStyleViewModel.selectedStyle.collectAsState()

    val themeColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    var stylePickerExpanded by remember { mutableStateOf(false) }
    var selectedSite by remember { mutableStateOf<LaunchSite?>(null) }
    var selectedAltitude by remember { mutableStateOf<Int?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(selectedSite) {
        selectedAltitude = null
        selectedSite?.let { site ->
            val point = Point.fromLngLat(site.lon, site.lat)
            repeat(10) {
                val elev = mapViewRef?.mapboxMap?.getElevation(point)
                if (elev != null) {
                    selectedAltitude = elev.toInt()
                    return@LaunchedEffect
                }
                delay(200)
            }
        }
    }

    Log.d("HomeMapScreen", "Launch sites count: ${uiState.launchSites.size}")

    val markerIcon = rememberIconImage(
        key = R.drawable.location_pin_filled,
        painter = painterResource(id = R.drawable.location_pin_filled)
    )

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(4.275008636630356)
            center(Point.fromLngLat(9.575323200000042, 62.18954459999999))
            pitch(51.57200000000083)
            bearing(16.725)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(Color.White.copy(alpha = 0.5f))),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Icon(painterResource(R.drawable.arrow_left), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dashboard", color = Color.White)
            }
            Text(
                text = "Select Location",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Box(Modifier.fillMaxSize()) {
            MapboxMap(
                Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = { GenericStyle(style = Style.STANDARD) },
                scaleBar = {},
                logo = {
                    Logo(Modifier.navigationBarsPadding().padding(bottom = 8.dp))
                },
                attribution = {
                    Attribution(Modifier.navigationBarsPadding().padding(bottom = 8.dp))
                }
            ) {
            MapEffect(selectedStyle.preset) { mapView ->
                mapViewRef = mapView
                mapView.mapboxMap.getStyle { style ->
                    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(selectedStyle.preset))
                    style.setStyleImportConfigProperty("basemap", "theme", Value.valueOf("faded"))
                    if (!style.styleSourceExists("terrain-dem")) {
                        style.addSource(rasterDemSource("terrain-dem") {
                            url("mapbox://mapbox.mapbox-terrain-dem-v1")
                            tileSize(514)
                            maxzoom(14)
                        })
                        style.setTerrain(terrain("terrain-dem") { exaggeration(1.0) })
                    }
                }
            }

                uiState.launchSites.forEach { site ->
                    PointAnnotation(
                        point = Point.fromLngLat(site.lon, site.lat),
                        onClick = {
                            selectedSite = if (selectedSite?.id == site.id) null else site
                            true
                        }
                    ) {
                        iconImage = markerIcon
                        iconSize = 1.5
                        iconAnchor = IconAnchor.BOTTOM
                    }
                }

                MapEffect(Unit) { mapView ->
                    mapView.gestures.addOnMapClickListener { _ ->
                        selectedSite = null
                        false
                    }
                }

                selectedSite?.let { site ->
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(site.lon, site.lat))
                            allowOverlap(true)
                            annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                        }
                    ) {
                        SiteInfoBox(
                            site = site,
                            altitude = selectedAltitude,
                            themeColor = themeColor,
                            onNavigate = { onNavigateToLaunchSites(site.id) },
                            onDismiss = { selectedSite = null }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
                    Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
                    Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { stylePickerExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(text = "Map style", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(text = selectedStyle.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
                DropdownMenu(
                    expanded = stylePickerExpanded,
                    onDismissRequest = { stylePickerExpanded = false }
                ) {
                    MAP_STYLES.forEach { style ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = style.label,
                                    fontWeight = if (style == selectedStyle) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                mapStyleViewModel.setStyle(style)
                                stylePickerExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteInfoBox(
    site: LaunchSite,
    altitude: Int?,
    themeColor: Color,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.width(155.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColor)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = site.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text("Lat:  ${"%.4f".format(site.lat)}", fontSize = 11.sp, color = Color.DarkGray)
                Text("Lon:  ${"%.4f".format(site.lon)}", fontSize = 11.sp, color = Color.DarkGray)
                Text(
                    text = if (altitude != null) "Elev: ${altitude} m" else "Elev: ...",
                    fontSize = 11.sp,
                    color = if (altitude != null) Color.DarkGray else Color.Gray
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColor)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable { onNavigate() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.my_location),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Select", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
