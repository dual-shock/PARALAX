package com.dual_shock.paralax.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.dual_shock.paralax.R
import com.dual_shock.paralax.ui.theme.appColors

@Composable
fun StaggeredAtmosphericTable(
    atmosphericLayers: List<Layer>,
    shearLayers: List<Layer>,
    locationName: String,
    dateLabel: String,
    hour: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wind Shear", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appColors.onSurface)
                        Text(
                            "$locationName · $dateLabel · $hour",
                            fontSize = 10.sp,
                            color = appColors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.close), contentDescription = "Close", tint = appColors.onSurface)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = appColors.outline)

                val isLoading = atmosphericLayers.isEmpty() && shearLayers.isEmpty()

                if (isLoading) {
                    var rotation by remember { mutableFloatStateOf(0f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(16)
                            rotation = (rotation + 3f) % 360f
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.loading),
                            contentDescription = "Loading",
                            tint = appColors.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .rotate(rotation)
                        )
                    }
                } else {
                    val rowHeight = 64.dp
                    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
                    val connectorColor = appColors.onSurfaceVariant

                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            shearLayers.forEachIndexed { i, _ ->
                                val lineY = (i + 1) * rowHeightPx
                                val startX = size.width / 2 - 40f
                                val endX = size.width / 2 + 40f

                                drawLine(
                                    color = connectorColor,
                                    start = Offset(startX, lineY),
                                    end = Offset(endX, lineY),
                                    strokeWidth = 2f
                                )

                                drawLine(
                                    color = connectorColor,
                                    start = Offset(endX - 10f, lineY - 8f),
                                    end = Offset(endX, lineY),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(endX - 10f, lineY + 8f),
                                    end = Offset(endX, lineY),
                                    strokeWidth = 2f
                                )
                            }
                        }

                        Row(modifier = modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                atmosphericLayers.forEach { layer ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowHeight)
                                            .border(0.5.dp, appColors.outline)
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("%.0f m".format(layer.altitude), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(painterResource(R.drawable.air), contentDescription = "wind speed",
                                                    modifier = Modifier.size(14.dp), tint = appColors.primary)
                                                Text("%.1f m/s".format(layer.windSpeed), fontSize = 11.sp, color = appColors.primary)
                                                Icon(
                                                    painter = painterResource(R.drawable.arrow_back),
                                                    contentDescription = "direction",
                                                    tint = appColors.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .graphicsLayer { rotationZ = (layer.windDirection + 90).toFloat() }
                                                )
                                                Text("%.0f°".format(layer.windDirection), fontSize = 11.sp, color = appColors.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(32.dp))

                            Column(modifier = Modifier.weight(1f).padding(top = rowHeight / 2)) {
                                shearLayers.forEach { layer ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowHeight)
                                            .border(0.5.dp, appColors.outline)
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("%.0f m".format(layer.altitude), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(painterResource(R.drawable.air), contentDescription = "wind speed",
                                                    modifier = Modifier.size(14.dp), tint = appColors.primary)
                                                Text("%.1f m/s".format(layer.windSpeed), fontSize = 11.sp, color = appColors.primary)
                                                Icon(
                                                    painter = painterResource(R.drawable.arrow_back),
                                                    contentDescription = "direction",
                                                    tint = appColors.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .graphicsLayer { rotationZ = (layer.windDirection + 90).toFloat() }
                                                )
                                                Text("%.0f°".format(layer.windDirection), fontSize = 11.sp, color = appColors.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}