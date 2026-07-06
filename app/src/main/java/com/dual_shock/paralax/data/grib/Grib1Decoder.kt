package com.dual_shock.paralax.data.grib

import android.util.Log
import com.dual_shock.paralax.model.grib.GribMessageMeta
import com.dual_shock.paralax.model.grib.GribParam
import com.dual_shock.paralax.model.grib.GridDefinition
import com.dual_shock.paralax.model.grib.grib1ParamToEnum
import kotlin.math.roundToInt

/**
 * Parses GRIB1 files (europe region: simple packing, 12 bits/value, 73×41 grid).
 *
 * GRIB1 message structure:
 *   Section 0: Indicator    — 8 bytes  ("GRIB" + 3-byte total length + edition=1)
 *   Section 1: PDS          — Product Definition Section (parameter, level)
 *   Section 2: GDS          — Grid Description Section (grid shape) [optional but present here]
 *   Section 3: BMS          — Bitmap Section [optional, usually absent]
 *   Section 4: BDS          — Binary Data Section (packing params + packed values)
 *   Section 5: End          — 4 bytes "7777"
 */
object Grib1Decoder {

    /**
     * Scans all messages in a GRIB1 file. Returns the grid definition (same for all messages)
     * and a list of message metadata for each parameter+level combo.
     */
    fun index(bytes: ByteArray): Pair<GridDefinition, List<GribMessageMeta>> {
        val messages = mutableListOf<GribMessageMeta>()
        var grid: GridDefinition? = null
        var offset = 0

        while (offset < bytes.size - 4) {
            // Find "GRIB" magic
            if (bytes[offset] != 'G'.code.toByte() ||
                bytes[offset + 1] != 'R'.code.toByte() ||
                bytes[offset + 2] != 'I'.code.toByte() ||
                bytes[offset + 3] != 'B'.code.toByte()
            ) {
                offset++
                continue
            }

            // Section 0: Indicator — 8 bytes
            // Bytes 5-7: total message length (3-byte big-endian unsigned)
            val totalLen = readUint24(bytes, offset + 4)
            val edition = bytes[offset + 7].toInt() and 0xFF
            if (edition != 1) {
                offset += totalLen
                continue
            }

            val msgStart = offset
            var pos = offset + 8 // past indicator section

            // Section 1: PDS (Product Definition Section)
            // PDS length is bytes 1-3 (3-byte big-endian)
            val pdsLen = readUint24(bytes, pos)
            // Byte 9 (1-indexed) = parameter number — that's pos+8 (0-indexed from PDS start)
            val paramNumber = bytes[pos + 8].toInt() and 0xFF
            // Byte 10 (1-indexed) = level type. 100 = isobaric level in hPa
            val levelType = bytes[pos + 9].toInt() and 0xFF
            // Bytes 11-12 (1-indexed) = level value (2-byte big-endian)
            val levelValue = readUint16(bytes, pos + 10)
            // Byte 7 (1-indexed) = GDS flag — bit 7 indicates GDS present
            val gdsFlag = bytes[pos + 7].toInt() and 0xFF
            val hasGds = (gdsFlag and 0x80) != 0
            // Byte 8 (1-indexed) = BMS flag — bit 6 indicates BMS present
            val hasBms = (gdsFlag and 0x40) != 0

            pos += pdsLen

            // Section 2: GDS (Grid Description Section) — if present
            if (hasGds) {
                val gdsLen = readUint24(bytes, pos)

                if (grid == null) {
                    // Parse grid definition from GDS
                    // GDS template for lat/lon grid (data representation type = 0)
                    // Byte 7 (1-indexed) = data representation type
                    // Bytes 7-8: Ni (number of points along parallel)
                    val ni = readUint16(bytes, pos + 6)
                    // Bytes 9-10: Nj (number of points along meridian)
                    val nj = readUint16(bytes, pos + 8)
                    // Bytes 11-13: latitude of first grid point (millidegrees, signed)
                    val lat1 = readInt24(bytes, pos + 10) / 1000.0
                    // Bytes 14-16: longitude of first grid point (millidegrees, signed)
                    val lon1 = readInt24(bytes, pos + 13) / 1000.0
                    // Byte 17: resolution and component flags
                    // Bytes 18-20: latitude of last grid point
                    val lat2 = readInt24(bytes, pos + 17) / 1000.0
                    // Bytes 21-23: longitude of last grid point
                    val lon2 = readInt24(bytes, pos + 20) / 1000.0
                    // Bytes 24-25: i-direction increment (millidegrees)
                    val iInc = readUint16(bytes, pos + 23) / 1000.0
                    // Bytes 26-27: j-direction increment (millidegrees)
                    val jInc = readUint16(bytes, pos + 25) / 1000.0
                    // Byte 28: scanning mode
                    val scanMode = bytes[pos + 27].toInt() and 0xFF

                    grid = GridDefinition(
                        numCols = ni,
                        numRows = nj,
                        firstLat = lat1,
                        firstLon = lon1,
                        lastLat = lat2,
                        lastLon = lon2,
                        lonInc = iInc,
                        latInc = jInc,
                        scanMode = scanMode
                    )
                }
                pos += gdsLen
            }

            // Section 3: BMS (Bitmap Section) — if present, skip it
            if (hasBms) {
                val bmsLen = readUint24(bytes, pos)
                pos += bmsLen
            }

            // Section 4: BDS (Binary Data Section)
            val bdsLen = readUint24(bytes, pos)
            // Byte 4 (1-indexed): flags + unused bits count
            val bdsFlags = bytes[pos + 3].toInt() and 0xFF
            val unusedBits = bdsFlags and 0x0F
            // Bytes 5-6: binary scale factor E (signed 16-bit)
            val binaryScale = readInt16(bytes, pos + 4)
            // Bytes 7-10: reference value R (IBM 32-bit float)
            val refValue = readIbmFloat(bytes, pos + 6)
            // Byte 11: number of bits per value
            val bitsPerValue = bytes[pos + 10].toInt() and 0xFF

            // Packed data starts at byte 12 of BDS (pos + 11)
            val dataOffset = pos + 11
            val dataLength = bdsLen - 11

            val param = grib1ParamToEnum(paramNumber)
            Log.d("testing", "MSG: param=$paramNumber, levelType=$levelType, level=$levelValue")

            if (param != GribParam.UNKNOWN && levelType == 100) {
                messages.add(
                    GribMessageMeta(
                        paramCategory = 0, // not used for GRIB1
                        paramNumber = paramNumber,
                        pressureLevelHpa = levelValue,
                        refValue = refValue,
                        binaryScale = binaryScale,
                        decimalScale = 0, // GRIB1 uses decimal scale differently (in PDS)
                        bitsPerValue = bitsPerValue,
                        dataOffset = dataOffset,
                        dataLength = dataLength,
                        isJpeg2000 = false
                    )
                )
            }

            offset = msgStart + totalLen
        }

        return Pair(grid ?: error("No grid definition found in GRIB1 file"), messages)
    }

    /**
     * Extracts a single value from a simple-packed GRIB1 message at the given grid index.
     * Formula: Y = R + X * 2^E   (GRIB1 decimal scale is in PDS, already applied to R)
     */
    fun extractValue(bytes: ByteArray, msg: GribMessageMeta, gridIndex: Int): Double {
        val bitOffset = gridIndex * msg.bitsPerValue
        val byteOffset = msg.dataOffset + bitOffset / 8
        val bitShift = bitOffset % 8

        val rawValue = extractBits(bytes, byteOffset, bitShift, msg.bitsPerValue)

        return msg.refValue + rawValue * Math.pow(2.0, msg.binaryScale.toDouble())
    }

    /**
     * Computes the grid index for a given lat/lon.
     * For scan mode 0 (N→S, W→E): row counts DOWN from firstLat (north).
     */
    fun gridIndex(grid: GridDefinition, lat: Double, lon: Double): Int {
        val row: Int
        val col: Int

        if (grid.isSouthToNorth) {
            row = ((lat - grid.firstLat) / grid.latInc).roundToInt()
        } else {
            // North→South (scan mode 0): firstLat is the NORTH edge
            row = ((grid.firstLat - lat) / grid.latInc).roundToInt()
        }
        col = ((lon - grid.firstLon) / grid.lonInc).roundToInt()

        return row.coerceIn(0, grid.numRows - 1) * grid.numCols +
                col.coerceIn(0, grid.numCols - 1)
    }

    // ---- Binary reading helpers ----

    /** Reads 3 bytes as unsigned big-endian integer. */
    private fun readUint24(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                (bytes[offset + 2].toInt() and 0xFF)

    /** Reads 3 bytes as signed big-endian integer (sign bit in MSB). */
    private fun readInt24(bytes: ByteArray, offset: Int): Int {
        val raw = readUint24(bytes, offset)
        // GRIB1 uses sign-magnitude: bit 23 is sign, bits 0-22 are magnitude
        val magnitude = raw and 0x7FFFFF
        return if ((raw and 0x800000) != 0) -magnitude else magnitude
    }

    /** Reads 2 bytes as unsigned big-endian integer. */
    private fun readUint16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)

    /** Reads 2 bytes as signed big-endian integer (sign-magnitude). */
    private fun readInt16(bytes: ByteArray, offset: Int): Int {
        val raw = readUint16(bytes, offset)
        val magnitude = raw and 0x7FFF
        return if ((raw and 0x8000) != 0) -magnitude else magnitude
    }

    /**
     * Reads an IBM 32-bit floating point number.
     * IBM floats: 1 sign bit, 7-bit base-16 exponent (bias 64), 24-bit fraction.
     * Formula: (-1)^sign * (fraction / 2^24) * 16^(exponent - 64)
     */
    private fun readIbmFloat(bytes: ByteArray, offset: Int): Float {
        val raw = ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)

        if (raw == 0) return 0f

        val sign = if ((raw and 0x80000000.toInt()) != 0) -1.0 else 1.0
        val exponent = ((raw shr 24) and 0x7F) - 64
        val fraction = (raw and 0x00FFFFFF).toDouble() / (1 shl 24).toDouble()

        return (sign * fraction * Math.pow(16.0, exponent.toDouble())).toFloat()
    }

    /**
     * Extracts N bits from a byte array starting at the given byte offset and bit shift.
     * Handles values spanning up to 4 bytes.
     */
    private fun extractBits(bytes: ByteArray, byteOffset: Int, bitShift: Int, numBits: Int): Int {
        // Read enough bytes to cover the bits we need
        val bytesNeeded = (bitShift + numBits + 7) / 8
        var accumulator = 0
        for (i in 0 until bytesNeeded) {
            if (byteOffset + i < bytes.size) {
                accumulator = (accumulator shl 8) or (bytes[byteOffset + i].toInt() and 0xFF)
            }
        }
        // Shift right to align, then mask
        val rightShift = (bytesNeeded * 8) - bitShift - numBits
        return (accumulator shr rightShift) and ((1 shl numBits) - 1)
    }
}
