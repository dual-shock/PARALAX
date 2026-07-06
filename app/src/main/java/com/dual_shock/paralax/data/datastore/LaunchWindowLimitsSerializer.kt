package com.dual_shock.paralax.data.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import com.dual_shock.paralax.model.settings.LaunchWindowLimits
import java.io.InputStream
import java.io.OutputStream


@OptIn(ExperimentalSerializationApi::class)
object LaunchWindowLimitsSerializer : Serializer<LaunchWindowLimits> {
    override val defaultValue = LaunchWindowLimits()

    override suspend fun readFrom(input: InputStream): LaunchWindowLimits {
        return Json.decodeFromStream(input)
    }

    override suspend fun writeTo(t: LaunchWindowLimits, output: OutputStream) {
        Json.encodeToStream(t, output)
    }
}
