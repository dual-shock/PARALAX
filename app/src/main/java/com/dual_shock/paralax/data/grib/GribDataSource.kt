package com.dual_shock.paralax.data.grib

import android.util.Log
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.dual_shock.paralax.MetClient
import java.io.File
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

class GribDataSource(private val cacheDir: File) {

    private val client = MetClient.client
    private val baseUrl = "https://in2000.api.met.no/weatherapi/isobaricgrib/1.0"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "testing"
    }

    data class GribResult(
        val bytes: ByteArray,
        val actualTime: String
    )

    data class GribFileResult(
        val file: File,
        val actualTime: String
    )

    suspend fun getGrib1Bytes(targetTime: String? = null): GribResult {
        return getGribBytes(area = "europe", type = "grib1", extension = "grb", targetTime = targetTime)
    }

    suspend fun getGrib2File(targetTime: String? = null): GribFileResult {
        return getGribFile(area = "southern_norway", type = "grib2", extension = "grb2", targetTime = targetTime)
    }

    fun findClosestAvailableTime(targetTime: String, availableTimes: List<String>): String {
        if (availableTimes.isEmpty()) return targetTime
        val target = try { Instant.parse(targetTime) } catch (e: Exception) { return availableTimes.first() }
        return availableTimes.minByOrNull { timeStr ->
            val t = try { Instant.parse(timeStr) } catch (e: Exception) { return@minByOrNull Long.MAX_VALUE }
            abs((target - t).inWholeSeconds)
        } ?: availableTimes.first()
    }

    private fun findClosestCachedFile(area: String, extension: String, targetTime: String?): Pair<File, String>? {
        val files = cacheDir.listFiles()
            ?.filter { it.name.startsWith("${area}_") && it.extension == extension }
            ?: return null

        if (files.isEmpty()) return null

        val referenceTime = if (targetTime != null) {
            try { Instant.parse(targetTime) } catch (e: Exception) { Clock.System.now() }
        } else {
            Clock.System.now()
        }

        val bestFile = files.minByOrNull { file ->
            val timeStr = parseTimeFromCacheFilename(file.nameWithoutExtension, area)
            val t = try { Instant.parse(timeStr) } catch (e: Exception) { return@minByOrNull Long.MAX_VALUE }
            abs((referenceTime - t).inWholeSeconds)
        } ?: return null

        val time = parseTimeFromCacheFilename(bestFile.nameWithoutExtension, area)
        return Pair(bestFile, time)
    }

    private fun parseTimeFromCacheFilename(filename: String, area: String): String {
        val raw = filename.removePrefix("${area}_")
        val tIndex = raw.indexOf('T')
        if (tIndex == -1) return raw
        val datePart = raw.substring(0, tIndex)
        val timePart = raw.substring(tIndex + 1).replace("-", ":")
        return "${datePart}T$timePart"
    }

    private suspend fun getGribBytes(area: String, type: String, extension: String, targetTime: String?): GribResult {
        try {
            val availableJson = client.get("$baseUrl/available.json") {
                parameter("type", type)
            }.bodyAsText()

            val entries = json.parseToJsonElement(availableJson).jsonArray
            val filtered = entries.filter {
                it.jsonObject["params"]?.jsonObject?.get("area")?.jsonPrimitive?.content == area
            }
            if (filtered.isEmpty()) error("No available $type entries for area=$area")

            val referenceTime = if (targetTime != null) {
                try { Instant.parse(targetTime) } catch (e: Exception) { Clock.System.now() }
            } else {
                Clock.System.now()
            }

            val entry = filtered.minByOrNull { e ->
                val timeStr = e.jsonObject["params"]?.jsonObject?.get("time")?.jsonPrimitive?.content ?: ""
                val t = try { Instant.parse(timeStr) } catch (e: Exception) { return@minByOrNull Long.MAX_VALUE }
                abs((referenceTime - t).inWholeSeconds)
            }?.jsonObject ?: error("No entry found")

            val time = entry["params"]?.jsonObject?.get("time")?.jsonPrimitive?.content ?: ""
            val uri = entry["uri"]?.jsonPrimitive?.content ?: error("No URI")

            val safeTime = time.replace(":", "-")
            val cacheFile = File(cacheDir, "${area}_${safeTime}.$extension")

            if (cacheFile.exists()) {
                Log.d(TAG, "Cache hit: ${cacheFile.name} (${cacheFile.length() / 1024}KB)")
                return GribResult(cacheFile.readBytes(), time)
            }

            Log.d(TAG, "Cache miss — downloading $area GRIB for $time")
            val dlStart = System.currentTimeMillis()

            val bytes = client.get(uri).readRawBytes()

            val dlMs = System.currentTimeMillis() - dlStart
            Log.d(TAG, "Download complete: ${bytes.size / 1024}KB in ${dlMs}ms")

            cacheFile.writeBytes(bytes)

            return GribResult(bytes, time)
        } catch (e: Exception) {
            Log.d(TAG, "Network failed, checking for cached $type data for $area")
            val cached = findClosestCachedFile(area, extension, targetTime)

            if (cached != null) {
                val (file, time) = cached
                Log.d(TAG, "Using offline cache: ${file.name}")
                return GribResult(file.readBytes(), time)
            }

            throw e
        }
    }

    private suspend fun getGribFile(area: String, type: String, extension: String, targetTime: String?): GribFileResult {
        try {
            val availableJson = client.get("$baseUrl/available.json") {
                parameter("type", type)
            }.bodyAsText()

            val entries = json.parseToJsonElement(availableJson).jsonArray
            val filtered = entries.filter {
                it.jsonObject["params"]?.jsonObject?.get("area")?.jsonPrimitive?.content == area
            }
            if (filtered.isEmpty()) error("No available $type entries for area=$area")

            val referenceTime = if (targetTime != null) {
                try { Instant.parse(targetTime) } catch (e: Exception) { Clock.System.now() }
            } else {
                Clock.System.now()
            }

            val entry = filtered.minByOrNull { e ->
                val timeStr = e.jsonObject["params"]?.jsonObject?.get("time")?.jsonPrimitive?.content ?: ""
                val t = try { Instant.parse(timeStr) } catch (e: Exception) { return@minByOrNull Long.MAX_VALUE }
                abs((referenceTime - t).inWholeSeconds)
            }?.jsonObject ?: error("No entry found")

            val time = entry["params"]?.jsonObject?.get("time")?.jsonPrimitive?.content ?: ""
            val uri = entry["uri"]?.jsonPrimitive?.content ?: error("No URI")

            val safeTime = time.replace(":", "-")
            val cacheFile = File(cacheDir, "${area}_${safeTime}.$extension")
            Log.d(TAG, "Checking $type availability for area=$area, target=$targetTime, closest=$time")

            if (cacheFile.exists()) {
                Log.d(TAG, "Cache hit for $type: ${cacheFile.name} (${cacheFile.length() / 1024}KB)")
                return GribFileResult(cacheFile, time)
            }

            Log.d(TAG, "Cache miss for $type — downloading from $uri")
            val bytes = client.get(uri).readRawBytes()

            cacheFile.writeBytes(bytes)

            return GribFileResult(cacheFile, time)
        } catch (e: Exception) {
            Log.d(TAG, "Network failed, checking for cached $type data for $area")
            val cached = findClosestCachedFile(area, extension, targetTime)

            if (cached != null) {
                val (file, time) = cached
                Log.d(TAG, "Using offline cache: ${file.name}")
                return GribFileResult(file, time)
            }

            throw e
        }
    }
}
