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
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class GribClient {
    private val baseUrl = "https://in2000.api.met.no/weatherapi/isobaricgrib/1.0"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchAvailable(): String {
        return client.get("$baseUrl/available.json") {
            parameter("type", "grib2")
            header("User-Agent", "paralax/v0.1.0")
        }.bodyAsText()
    }

    suspend fun fetchGrib2Url(url: String): ByteArray {
        return client.get(url) {
                header("User-Agent", "paralax/v0.1.0")
        }.readRawBytes()
    }

    fun close() = client.close()
}

class GribApiParsingTest {

    private suspend fun fetchAvailable(): JsonArray {
        val api = GribClient()

        val jsonContent = api.fetchAvailable()
        val json = Json { ignoreUnknownKeys = true }
        val data = json.parseToJsonElement(jsonContent).jsonArray

        val availableTimes = data.jsonArray
        println("Found ${availableTimes.size} available entries:\n")

        for (entry in availableTimes) {
            val obj = entry.jsonObject
            val params = obj["params"]?.jsonObject
            val area = params?.get("area")?.jsonPrimitive?.content ?: "unknown"
            val time = params?.get("time")?.jsonPrimitive?.content ?: "unknown"
            val uri = obj["uri"]?.jsonPrimitive?.content ?: ""
            println("  Area: $area")
            println("    Time: $time")
            println("    URI: $uri")
            println()
        }

        val prettyJson = Json { prettyPrint = true }
        val prettyString = prettyJson.encodeToString(JsonElement.serializer(), data)
        File(
            File("src/test/java/com/dual_shock/paralax/api_data_fetching"),
            "GRIB_Available.json"
        ).writeText(prettyString)

        api.close()
        return data
    }

    @Test
    fun getAvailable() {
        runBlocking {
            fetchAvailable()
        }
    }

    private suspend fun fetchGrib2Data() {
        val AREA = "southern_norway"

        val available = fetchAvailable()

        val filtered = available.filter {
            it.jsonObject["params"]?.jsonObject
                ?.get("area")?.jsonPrimitive?.content == AREA
        }

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val hour = (now.hour / 3) * 3
        val snapped = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        val closestTime = snapped.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

        val entry = filtered.minByOrNull { entry ->
            val entryTime = entry.jsonObject["params"]?.jsonObject
                ?.get("time")?.jsonPrimitive?.content ?: ""
            val entryZdt = ZonedDateTime.parse(entryTime)
            abs(Duration.between(snapped, entryZdt).seconds)
        }?.jsonObject ?: error("No available entry found for area=$AREA")

        val uri = entry["uri"]?.jsonPrimitive?.content ?: error("No URI found")
        val time = entry["params"]?.jsonObject?.get("time")?.jsonPrimitive?.content ?: closestTime

        println("Fetching GRIB2 for area=$AREA, time=$time")
        println("  URI: $uri")

        val api = GribClient()
        val bytes = api.fetchGrib2Url(uri)
        val outputFile = File(
            File("src/test/java/com/dual_shock/paralax/api_data_fetching"),
            "GRIB2_${AREA}_${time.replace(":", "-")}.grib2"
        )
        outputFile.writeBytes(bytes)
        println("Saved ${bytes.size} bytes to ${outputFile.absolutePath}")

        api.close()
    }

    @Test
    fun getGrib2() {
        runBlocking {
            fetchGrib2Data()
        }
    }
}
