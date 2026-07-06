package com.dual_shock.paralax

import com.dual_shock.paralax.data.grib.Grib1Decoder
import com.dual_shock.paralax.model.grib.GribParam
import com.dual_shock.paralax.model.grib.grib1ParamToEnum
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GribDecoderTest {

    private fun loadGrib1(): ByteArray {
        val f = File("src/test/java/com/dual_shock/paralax/api_data_fetching/example_responses/europe_grib1_sample.grb")
        assertTrue("GRIB1 sample file must exist", f.exists())
        return f.readBytes()
    }

    @Test
    fun grib1_index_finds_30_messages() {
        val bytes = loadGrib1()
        val (grid, messages) = Grib1Decoder.index(bytes)
        println("GRIB1: Found ${messages.size} messages")
        println("  Grid: ${grid.numCols}x${grid.numRows}, scan mode ${grid.scanMode}")
        println("  First lat/lon: ${grid.firstLat}, ${grid.firstLon}")
        println("  Last lat/lon: ${grid.lastLat}, ${grid.lastLon}")

        assertEquals("Expected 73 columns", 73, grid.numCols)
        assertEquals("Expected 41 rows", 41, grid.numRows)
        assertEquals("Expected scan mode 0", 0, grid.scanMode)
        assertEquals("Expected 30 messages (10 levels × 3 params)", 30, messages.size)
    }

    @Test
    fun grib1_has_expected_pressure_levels() {
        val bytes = loadGrib1()
        val (_, messages) = Grib1Decoder.index(bytes)
        val levels = messages.map { it.pressureLevelHpa }.toSortedSet()
        println("GRIB1 pressure levels: $levels")
        assertEquals("Expected 10 pressure levels", 10, levels.size)
    }

    @Test
    fun grib1_decode_produces_sane_values_at_oslo() {
        val bytes = loadGrib1()
        val (grid, messages) = Grib1Decoder.index(bytes)

        val gridIdx = Grib1Decoder.gridIndex(grid, 59.91, 10.75)
        println("Oslo grid index (GRIB1): $gridIdx")

        for (msg in messages) {
            val value = Grib1Decoder.extractValue(bytes, msg, gridIdx)
            val param = grib1ParamToEnum(msg.paramNumber)
            println("  Level ${msg.pressureLevelHpa} hPa, $param = $value")

            when (param) {
                GribParam.U_WIND, GribParam.V_WIND -> {
                    assertTrue("Wind should be in [-150, 150] m/s, got $value", value in -150.0..150.0)
                }
                GribParam.TEMPERATURE -> {
                    assertTrue("Temperature should be in [150, 350] K, got $value", value in 150.0..350.0)
                }
                else -> {}
            }
        }
    }
}
