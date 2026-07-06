package com.dual_shock.paralax.data.locationforecast

import android.util.Log
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.dual_shock.paralax.MetClient
import java.io.File
import java.time.Instant

class LocationForecastDataSource(private val cacheDir: File) {
    private val net = MetClient.client
    private val point = "https://api.met.no/weatherapi/locationforecast/2.0/compact"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val LTAG = "testing"
    }


    suspend fun getForecast(lat: Double, lon: Double): LocationForecastResponse {
        val hourTimestamp = Instant.now().toString().take(13)
        val cacheFile = File(cacheDir, "locationForecast_${lat}_${lon}_$hourTimestamp.json")

        if (cacheFile.exists()) {
            Log.d(LTAG, "Cache hit: ${cacheFile.name}")
            return json.decodeFromString(cacheFile.readText())
        }

        Log.d(LTAG, "Cache miss — downloading locationForecast data")
        val response = net.get(point) {
            parameter("lat", lat)
            parameter("lon", lon)
        }.bodyAsText()

        cacheDir.listFiles()?.filter { it.name.startsWith("locationForecast_${lat}_${lon}_") }?.forEach { it.delete() }
        cacheFile.writeText(response)

        return json.decodeFromString(response)
    }

}

