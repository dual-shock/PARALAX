package com.dual_shock.paralax.ui.weatherdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dual_shock.paralax.R
import com.dual_shock.paralax.model.locationforecast.ForecastEntry
import com.dual_shock.paralax.ui.theme.appColors
import kotlin.math.roundToInt

@Composable
fun WeatherDetailsPopup(
    entry: ForecastEntry,
    lightningCount: Int?,
    location: String,
    dateLabel: String,
    hour: String,
    viabilityScore: Int,
    onDismiss: () -> Unit
) {
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        Text("Weather Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                        Text(
                            "$location · $dateLabel · $hour",
                            fontSize = 10.sp,
                            color = appColors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.close), contentDescription = "Close", tint = appColors.onSurface)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = appColors.outline)

                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.air_temperature?.let { temp ->
                        WeatherDetailRow(R.drawable.thermometer_add, "Temperature", "${temp.roundToInt()}°C")
                    }
                    entry.wind_speed?.let {
                        WeatherDetailRow(R.drawable.air, "Wind Speed", "${it.roundToInt()} m/s")
                    }
                    entry.wind_from_direction?.let { dir ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.explore),
                                contentDescription = "Wind Direction",
                                modifier = Modifier.size(24.dp),
                                tint = appColors.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Wind Direction", style = MaterialTheme.typography.bodyMedium, color = appColors.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                tint = appColors.onSurface,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = (dir + 90).toFloat() }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("${dir.roundToInt()}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = appColors.onSurface)
                        }
                    }
                    entry.precipitation_amount?.let {
                        WeatherDetailRow(R.drawable.rainy, "Precipitation", "${"%.1f".format(it)} mm")
                    }
                    entry.cloud_area_fraction?.let {
                        WeatherDetailRow(R.drawable.cloud, "Cloud Cover", "${it.roundToInt()}%")
                    }
                    entry.relative_humidity?.let {
                        WeatherDetailRow(R.drawable.humidity_low, "Humidity", "${it.roundToInt()}%")
                    }
                    lightningCount?.let {
                        WeatherDetailRow(R.drawable.bolt, "Lightning", "$it strikes")
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = recommendation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = recommendationColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailRow(iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = appColors.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = appColors.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = appColors.onSurface)
    }
}
