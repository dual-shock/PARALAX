package com.dual_shock.paralax.data.frost


import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.dual_shock.paralax.model.frost.Lightning
import com.dual_shock.paralax.BuildConfig


class FrostDataSource {
    private val client = HttpClient(CIO) {
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = BuildConfig.FROST_USERNAME,
                        password = BuildConfig.FROST_PASSWORD
                    )
                }
            }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }



    suspend fun fetchLightning(reftime: String, lat: Double, lon: Double, radius: Double = 0.5): List<Lightning> {
        val polygon = "POLYGON((${lon - radius} ${lat - radius}, ${lon + radius} ${lat - radius}, " +
                "${lon + radius} ${lat + radius}, ${lon - radius} ${lat + radius}, ${lon - radius} ${lat - radius}))"

        return client.get("https://frost.met.no/api/v1/lightning/get") {
            parameter("referencetime", reftime)
            parameter("format", "json")
            parameter("geometry", polygon)

        }.body()
    }

}