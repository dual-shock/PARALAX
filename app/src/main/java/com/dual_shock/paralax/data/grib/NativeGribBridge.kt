package com.dual_shock.paralax.data.grib

object NativeGribBridge {
    init {
        System.loadLibrary("gribbridge")
    }
    external fun decodeNearestFromFile(
        filePath: String,
        lat: Double,
        lon: Double
    ): FloatArray?

    external fun decodeRegionFromFile(
        filePath: String,
        centerLat: Double,
        centerLon: Double,
        patchRadius: Int
    ): FloatArray?
}

