package com.dual_shock.paralax.data.interpol

import android.util.Log
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.dual_shock.paralax.MetClient
import org.json.JSONObject
import java.io.File


class InterpolDataSource(private val cacheDir: File) {
    private val net = MetClient.client
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "testing"
    }

    suspend fun getData(location: String): InterpolResponse {
        try {
            val latestId = getLatestInstanceId()
            val cacheFile = File(cacheDir, "interpol_${location}_${latestId}.json")

            if (cacheFile.exists()) {
                Log.d(TAG, "Cache hit: ${cacheFile.name}")
                return json.decodeFromString(cacheFile.readText())
            }

            Log.d(TAG, "Cache miss — downloading interpol data for $location")
            val response = net.get(
                "https://interpol.met.no/collections/meps-det-vertical/instances/$latestId/locations/$location"
            ).bodyAsText()

            cacheDir.listFiles()?.filter { it.name.startsWith("interpol_${location}_") }?.forEach { it.delete() }
            cacheFile.writeText(response)

            return json.decodeFromString(response)
        } catch (e: Exception) {
            Log.d(TAG, "Network failed, checking for cached interpol data for $location")
            val cached = cacheDir.listFiles()
                ?.filter { it.name.startsWith("interpol_${location}_") && it.extension == "json" }
                ?.maxByOrNull { it.lastModified() }

            if (cached != null) {
                Log.d(TAG, "Using offline cache: ${cached.name}")
                return json.decodeFromString(cached.readText())
            }

            throw e
        }
    }

    suspend fun getLocations(): Locations {
        val latestId = getLatestInstanceId()
        val response = net.get(
            "https://interpol.met.no/collections/meps-det-vertical/instances/$latestId/locations"
        ).bodyAsText()
        return json.decodeFromString(response)
    }

    private suspend fun getLatestInstanceId(): String {
        val model = net.get("https://interpol.met.no/collections/meps-det-vertical/instances").bodyAsText()
        val instances = JSONObject(model).getJSONArray("instances")
        return instances.getJSONObject(instances.length() - 1).getString("id")
    }
}
