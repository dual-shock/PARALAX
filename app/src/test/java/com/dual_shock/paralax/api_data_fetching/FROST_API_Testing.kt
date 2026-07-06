package com.dual_shock.paralax.api_data_fetching

import com.dual_shock.paralax.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import com.dual_shock.paralax.data.frost.FrostRepository
import org.junit.Test
import java.io.File
import java.util.Base64

class FrostClient {
    private val baseUrl = "https://frost.met.no/api/v1/lightning/get"
    private val clientId = BuildConfig.FROST_USERNAME
    private val authToken = BuildConfig.FROST_PASSWORD

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchLightning(reftime: String): HttpResponse {
        val basicAuth = Base64.getEncoder().encodeToString("$clientId:$authToken".toByteArray())
        return client.get(baseUrl) {
            parameter("referencetime", reftime)
            parameter("format", "json")
            header("Authorization", "Basic $basicAuth")
            header("User-Agent", "Paralax/v0.1.0")
        }
    }

    fun close() = client.close()
}

class FrostApiParsingTest {

    private suspend fun fetchLightningData() {
        val api = FrostClient()

        val outputDir = File("src/test/java/com/dual_shock/paralax/api_data_fetching/example_responses")
        outputDir.mkdirs()

        val reftime = "2026-03-01T00:00:00Z/2026-03-27T00:00:00Z"
        val response = api.fetchLightning(reftime)
        val statusCode = response.status.value
        val body = response.bodyAsText()

        println("Status: $statusCode")
        println("Response preview:\n${body.take(1000)}")

        if (statusCode == 200 && body.isNotBlank()) {
            val outputFile = File(outputDir, "FROST_Lightning.json")
            outputFile.writeText(body)

            println("Saved to ${outputFile.absolutePath}")
        }

        api.close()



    }

    @Test
    fun getLightningData() {
        runBlocking {
            fetchLightningData()
        }
    }


    @Test
    fun testRepository() {
        runBlocking {
            val repo = FrostRepository()
            val strikes = repo.getRecentLightningStrikes(lat = 69.10, lon = 15.58, hoursBack = 48)
            println("Got ${strikes.size} strikes")
            strikes.take(3).forEach {
                println("Time: ${it.Epoch}, Lat: ${it.latitude}, Lon: ${it.longitude}")
            }
            assert(strikes.isNotEmpty())
        }
    }

}
