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

class Client {
    private val baseUrl = "https://interpol.met.no"
    private val collection = "meps-det-vertical"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchJsonData(endpoint: String = "collections/$collection/instances", hasBaseUrl: Boolean = false): String {
        val endpointUrl = if (hasBaseUrl) {
            endpoint
        } else {
            "$baseUrl/$endpoint"
        }

        return client.get(endpointUrl).bodyAsText()
    }


    fun close() = client.close()
}

class ApiParsingTest {



    private suspend fun fetchInstances(): JsonArray  {
        val api = Client()

        val jsonContent = api.fetchJsonData()
        val json = Json { ignoreUnknownKeys = true }
        val data = json.parseToJsonElement(jsonContent).jsonObject

        val instances = data["instances"]?.jsonArray ?: error("No instances found")
        println("Found ${instances.size} available instances:\n")

        val urls = mutableListOf<String>()
        var fileId = ""

        for (inst in instances) {
            val obj = inst.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: "unknown"
            fileId = obj["id"]?.jsonPrimitive?.content ?: "unknown"
            val extent = obj["extent"]?.jsonObject


            val boundingBox = extent?.get("spatial")?.jsonObject
                ?.get("bbox")?.jsonArray
            val bbox = boundingBox?.firstOrNull()?.jsonArray
            val longFrom = bbox?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val longTo = bbox?.getOrNull(1)?.jsonPrimitive?.doubleOrNull
            val latFrom = bbox?.getOrNull(2)?.jsonPrimitive?.doubleOrNull
            val latTo = bbox?.getOrNull(3)?.jsonPrimitive?.doubleOrNull

            val timeInterval = extent?.get("temporal")?.jsonObject
                ?.get("interval")?.jsonArray
                ?.firstOrNull()?.jsonArray
            val timeFrom = timeInterval?.getOrNull(0)?.jsonPrimitive?.content
            val timeTo = timeInterval?.getOrNull(1)?.jsonPrimitive?.content

            val layersInterval = extent?.get("vertical")?.jsonObject
                ?.get("interval")?.jsonArray
                ?.firstOrNull()?.jsonArray
            val layerFrom = layersInterval?.getOrNull(0)?.jsonPrimitive?.content
            val layerTo = layersInterval?.getOrNull(1)?.jsonPrimitive?.content

            val parameterNames = obj["parameter_names"]?.jsonObject ?: JsonObject(emptyMap())
            val paramsWithUnits = parameterNames.entries.joinToString(", ") { (name, value) ->
                val unit = value.jsonObject["unit"]?.jsonObject
                    ?.get("label")?.jsonPrimitive?.content ?: "?"
                "$name ($unit)"
            }

            val locationUrl = obj["data_queries"]?.jsonObject
                ?.get("locations")?.jsonObject
                ?.get("link")?.jsonObject
                ?.get("href")?.jsonPrimitive?.content ?: ""
            urls.add(locationUrl)

            println("  ID: $id")
            if (timeFrom != null && timeTo != null) {
                println("    Time range: $timeFrom → $timeTo")
            }
            if (layerFrom != null && layerTo != null) {
                println("    Vertical layers: $layerFrom → $layerTo")
            }
            println("    Parameters and their units: $paramsWithUnits")
            println("    Available spatial extent of simulation: $boundingBox")
            println("      Longitude: $longFrom → $longTo")
            println("      Latitude: $latFrom → $latTo")
            println("    URL for querying specific locations: $locationUrl")
            println()
        }

        val prettyJson = Json { prettyPrint = true }

        val prettyString = prettyJson.encodeToString(JsonElement.serializer(), data)
        File( File("src/test/java/com/dual_shock/paralax/api_data_fetching"),"EDR_Instances_${fileId.replace(":", "-")}.json").writeText(prettyString)

        api.close()
        return instances
    }

    @Test
    fun getInstances() {
        runBlocking {
            fetchInstances()
        }
    }

    private suspend fun fetchInstanceLocations(): JsonArray {
        val instances = fetchInstances()
        val lastInstance = instances.last()
        val obj = lastInstance.jsonObject
        val id = obj["id"]?.jsonPrimitive?.content ?: "unknown"
        val fileId = obj["id"]?.jsonPrimitive?.content ?: "unknown"
        val url = obj["data_queries"]?.jsonObject
            ?.get("locations")?.jsonObject
            ?.get("link")?.jsonObject
            ?.get("href")?.jsonPrimitive?.content ?: ""


        val api = Client()

        val jsonContent = api.fetchJsonData(url, true)
        val json = Json { ignoreUnknownKeys = true }
        val data = json.parseToJsonElement(jsonContent).jsonObject

        val prettyJson = Json { prettyPrint = true }
        val prettyString = prettyJson.encodeToString(JsonElement.serializer(), data)
        File( File("src/test/java/com/dual_shock/paralax/api_data_fetching"),"EDR_Instance_${fileId.replace(":", "-")}_Locations.json").writeText(prettyString)



        api.close()
        return data["features"]?.jsonArray ?: error("No features found")
    }



    @Test
    fun getLocations() {
        runBlocking {
            fetchInstanceLocations()
        }
    }


    private suspend fun fetchLocationData() {
        val LOCATION_TO_FIND_ID = "utsira"

        val locations = fetchInstanceLocations()

        val location = locations.firstOrNull {
            it.jsonObject["id"]?.jsonPrimitive?.content == LOCATION_TO_FIND_ID
        }?.jsonObject
        if (location == null) {
            println("Location not found")
            return
        }

        println(location)
        val url = location["properties"]?.jsonObject
            ?.get("href")?.jsonPrimitive?.content ?: ""


        val api = Client()
        val jsonContent = api.fetchJsonData(url, true)
        val json = Json { ignoreUnknownKeys = true }
        val data = json.parseToJsonElement(jsonContent).jsonObject


        val fileId = data["coverages"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("domain")?.jsonObject
            ?.get("axes")?.jsonObject
            ?.get("t")?.jsonObject
            ?.get("values")?.jsonArray
            ?.firstOrNull()?.jsonPrimitive?.content ?: ""



        val prettyJson = Json { prettyPrint = true }
        val prettyString = prettyJson.encodeToString(JsonElement.serializer(), data)
        File(
            File("src/test/java/com/dual_shock/paralax/api_data_fetching"),
            "EDR_Location_${LOCATION_TO_FIND_ID}_${fileId.replace(":", "-")}_data.json"
        ).writeText(prettyString)
    }

    @Test
    fun getLocationData() {
        runBlocking {
            fetchLocationData()
        }
    }
}