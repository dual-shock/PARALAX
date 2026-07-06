package com.dual_shock.paralax.model.grib

data class GridDefinition(
    val numCols: Int,       // Ni — number of points along a parallel (west→east)
    val numRows: Int,       // Nj — number of points along a meridian
    val firstLat: Double,   // latitude of the first grid point
    val firstLon: Double,   // longitude of the first grid point
    val lastLat: Double,    // latitude of the last grid point
    val lastLon: Double,    // longitude of the last grid point
    val latInc: Double,     // j-direction increment in degrees
    val lonInc: Double,     // i-direction increment in degrees
    val scanMode: Int       // scanning mode flags byte
) {
    val isSouthToNorth: Boolean get() = (scanMode and 0x40) != 0
    val isWestToEast: Boolean get() = (scanMode and 0x80) == 0
}
