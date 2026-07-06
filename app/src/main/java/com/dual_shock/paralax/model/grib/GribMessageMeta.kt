package com.dual_shock.paralax.model.grib

data class GribMessageMeta(
    val paramCategory: Int,     // WMO parameter category (GRIB2) or parameter number (GRIB1)
    val paramNumber: Int,       // WMO parameter number — 2=u-wind, 3=v-wind, 0=temperature (GRIB2)
    // For GRIB1: 33=u, 34=v, 11=temperature
    val pressureLevelHpa: Int,  // pressure level in hPa (e.g., 850, 500, 100)
    val refValue: Float,        // R — reference value (simple packing / J2K offset)
    val binaryScale: Int,       // E — binary scale factor
    val decimalScale: Int,      // D — decimal scale factor
    val bitsPerValue: Int,      // N — number of bits per packed value
    val dataOffset: Int,        // byte offset into the file where the raw data starts
    // (GRIB1: start of packed binary values in section 4)
    // (GRIB2: start of the J2K codestream in section 7)
    val dataLength: Int,        // length of the raw data bytes (needed for slicing J2K codestream)
    val isJpeg2000: Boolean     // true for GRIB2 template 5.40, false for simple packing
)
