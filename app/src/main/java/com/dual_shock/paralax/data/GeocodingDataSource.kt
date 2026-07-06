package com.dual_shock.paralax.data


import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import com.dual_shock.paralax.MetClient

@Serializable
data class NominatimResult (
    val display_name: String,
    val lat: String,
    val lon: String
)


class GeocodingDataSource {
    suspend fun search(query: String): List <NominatimResult>{
        return try{
            MetClient.client.get("https://nominatim.openstreetmap.org/search"){
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", 5)
                parameter("countrycodes", "no")
            }.body()
        } catch (e: Exception) { emptyList() }


    }
}