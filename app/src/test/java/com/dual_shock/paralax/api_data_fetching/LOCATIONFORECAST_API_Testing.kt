package com.dual_shock.paralax.api_data_fetching

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import java.io.File

class LocationforecastClient {
    private val baseUrl = "https://in2000.api.met.no/weatherapi/locationforecast/2.0"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }


    suspend fun fetchComplete(lat: Double, lon: Double): String {
        return client.get("$baseUrl/complete") {
            parameter("lat", lat)
            parameter("lon", lon)
            header("User-Agent", "paralax/v0.1.0")
        }.bodyAsText()
    }

    suspend fun fetchStatus(): String {
        return client.get("$baseUrl/status") {
            header("User-Agent", "paralax/v0.1.0")
        }.bodyAsText()
    }

    fun close() = client.close()
}

class LocationforecastApiParsingTest {

    private suspend fun fetchCompleteForecast() {
        val LAT = 59.93
        val LON = 10.72
        val api = LocationforecastClient()

        val jsonContent = api.fetchComplete(LAT, LON)
        val json = Json { ignoreUnknownKeys = true }
        val data = json.parseToJsonElement(jsonContent).jsonObject

        val properties = data["properties"]?.jsonObject
        val timeseries = properties?.get("timeseries")?.jsonArray
        println("Found ${timeseries?.size} timeseries entries (complete)")

        for (entry in timeseries?.take(5) ?: emptyList()) {
            val obj = entry.jsonObject
            val time = obj["time"]?.jsonPrimitive?.content ?: "unknown"
            val instant = obj["data"]?.jsonObject
                ?.get("instant")?.jsonObject
                ?.get("details")?.jsonObject
            val temp = instant?.get("air_temperature")?.jsonPrimitive?.doubleOrNull
            val humidity = instant?.get("relative_humidity")?.jsonPrimitive?.doubleOrNull
            val cloudFraction = instant?.get("cloud_area_fraction")?.jsonPrimitive?.doubleOrNull
            println("  Time: $time")
            println("    Temperature: $temp °C")
            println("    Humidity: $humidity %")
            println("    Cloud fraction: $cloudFraction %")
            println()
        }

        val prettyJson = Json { prettyPrint = true }
        val prettyString = prettyJson.encodeToString(JsonElement.serializer(), data)
        File(
            File("src/test/java/com/dual_shock/paralax/api_data_fetching"),
            "LOCATIONFORECAST_Complete_${LAT}_${LON}.json"
        ).writeText(prettyString)

        api.close()
    }

    @Test
    fun getCompleteForecast() {
        runBlocking {
            fetchCompleteForecast()
        }
    }

    private suspend fun fetchStatusData() {
        val api = LocationforecastClient()

        val content = api.fetchStatus()
        println("Status: $content")

        File(
            File("src/test/java/com/dual_shock/paralax/api_data_fetching"),
            "LOCATIONFORECAST_Status.txt"
        ).writeText(content)

        api.close()
    }

    @Test
    fun getStatus() {
        runBlocking {
            fetchStatusData()
        }
    }
}
