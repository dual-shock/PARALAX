package com.dual_shock.paralax.data.grib

import android.util.Log

object Grib2Decoder {
    private const val TAG = "testing"

    data class Grib2Record(
        val paramCategory: Int,
        val paramNumber: Int,
        val levelHpa: Int,
        val value: Double
    )

    fun decodeNearPoint(
        grib2FilePath: String,
        lat: Double,
        lon: Double
    ): List<Grib2Record> {
        val start = System.currentTimeMillis()
        Log.d(TAG, "decodeNearPoint start: file=$grib2FilePath lat=$lat lon=$lon")
        val raw = NativeGribBridge.decodeNearestFromFile(grib2FilePath, lat, lon)
        val elapsed = System.currentTimeMillis() - start

        if (raw == null) {
            Log.e(TAG, "Native GRIB2 decode returned null")
            return emptyList()
        }
        Log.d(TAG, "Native GRIB2 decode ${raw.size} floats in ${elapsed}ms")

        if (raw.isEmpty()) return emptyList()
        if (raw.size % 4 != 0) {
            Log.e(TAG, "Unexpected tuple count from native: ${raw.size}")
            return emptyList()
        }

        val out = ArrayList<Grib2Record>(raw.size / 4)
        var i = 0
        while (i < raw.size) {
            out.add(
                Grib2Record(
                    paramCategory = raw[i].toInt(),
                    paramNumber = raw[i + 1].toInt(),
                    levelHpa = raw[i + 2].toInt(),
                    value = raw[i + 3].toDouble()
                )
            )
            i += 4
        }
        out.take(5).forEachIndexed { index, r ->
            Log.d(TAG, "Parsed record[$index]: cat=${r.paramCategory}, num=${r.paramNumber}, level=${r.levelHpa}, value=${r.value}")
        }
        return out
    }

    data class Grib2RegionRecord(
        val lat: Double,
        val lon: Double,
        val paramCategory: Int,
        val paramNumber: Int,
        val levelHpa: Int,
        val value: Double
    )

    fun decodeRegion(
        grib2FilePath: String,
        centerLat: Double,
        centerLon: Double,
        patchRadius: Int = 2
    ): List<Grib2RegionRecord> {
        val start = System.currentTimeMillis()
        Log.d(TAG, "decodeRegion: center=($centerLat,$centerLon) radius=$patchRadius")

        val raw = NativeGribBridge.decodeRegionFromFile(grib2FilePath, centerLat, centerLon, patchRadius)
        val elapsed = System.currentTimeMillis() - start

        if (raw == null) {
            Log.e(TAG, "Native GRIB2 region decode returned null")
            return emptyList()
        }
        Log.d(TAG, "Native GRIB2 region decode: ${raw.size} floats in ${elapsed}ms")

        if (raw.isEmpty()) return emptyList()
        if (raw.size % 6 != 0) {
            Log.e(TAG, "Unexpected tuple count from native region: ${raw.size}")
            return emptyList()
        }

        val out = ArrayList<Grib2RegionRecord>(raw.size / 6)
        var i = 0
        while (i < raw.size) {
            out.add(Grib2RegionRecord(
                lat = raw[i].toDouble(),
                lon = raw[i + 1].toDouble(),
                paramCategory = raw[i + 2].toInt(),
                paramNumber = raw[i + 3].toInt(),
                levelHpa = raw[i + 4].toInt(),
                value = raw[i + 5].toDouble()
            ))
            i += 6
        }

        val gridPoints = out.map { Pair(it.lat, it.lon) }.distinct()
        Log.d(TAG, "Region decoded: ${out.size} records across ${gridPoints.size} grid points")

        return out
    }
}