package com.dual_shock.paralax

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object MetClient {
    val client: HttpClient by lazy { //not automatically setup
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true // unknown keys in the JSON structure
                    isLenient = true //lenient towards errors
                }
                )
            }

            defaultRequest{
                header("User-Agent", "Paralax/v0.1.0") // authentification
            }

        }
    }

}


