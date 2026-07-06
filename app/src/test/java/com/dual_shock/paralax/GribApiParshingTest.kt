package com.dual_shock.paralax

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class GribApiParsingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var data: JsonArray

    @Before
    fun setup() {
        val file = File("src/test/java/com/dual_shock/paralax/api_data_fetching/example_responses/GRIB_Available.json")
        data = json.parseToJsonElement(file.readText()).jsonArray
    }

    @Test
    fun notEmpty() {
        assertTrue(data.isNotEmpty())
    }

    @Test
    fun hasUrl() {
        for (entry in data) {
            assertNotNull(entry.jsonObject["uri"])
        }
    }

    @Test
    fun hasArea() {
        for (entry in data) {
            assertNotNull(entry.jsonObject["params"]?.jsonObject?.get("area"))
        }
    }
}
