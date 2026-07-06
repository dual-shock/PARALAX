package com.dual_shock.paralax.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import com.dual_shock.paralax.model.hour.WatchedHour
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
object WatchedHoursSerializer : Serializer<List<WatchedHour>> {
    override val defaultValue: List<WatchedHour> = emptyList()

    override suspend fun readFrom(input: InputStream): List<WatchedHour> {
        return try {
            Json.decodeFromStream(ListSerializer(WatchedHour.serializer()), input)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun writeTo(t: List<WatchedHour>, output: OutputStream) {
        Json.encodeToStream(ListSerializer(WatchedHour.serializer()), t, output)
    }
}

val Context.watchedHoursDataStore: DataStore<List<WatchedHour>> by dataStore(
    fileName = "watched_hours.json",
    serializer = WatchedHoursSerializer
)
