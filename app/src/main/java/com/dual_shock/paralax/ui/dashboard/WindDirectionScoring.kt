package com.dual_shock.paralax.ui.dashboard

import com.dual_shock.paralax.model.settings.WindKeyframe
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

object WindDirectionScorer {

    // How sharply the score falls off, safety cone
    private const val FALLOFF_POWER = 4.0

    private fun altitudeKey(altitudeKm: Float): Int = Math.round(altitudeKm * 100)
    private fun interpolateAt(altitudeKm: Float, sortedKeyframes: List<WindKeyframe>): Pair<Float, Float> {
        val exactMatch = sortedKeyframes.firstOrNull { altitudeKey(it.altitudeKm) == altitudeKey(altitudeKm) }
        if (exactMatch != null) return exactMatch.preferredDeg to exactMatch.weight
        if (sortedKeyframes.size == 1) {
            val only = sortedKeyframes[0]
            return only.preferredDeg to only.weight
        }

        val lower = sortedKeyframes.lastOrNull { it.altitudeKm <= altitudeKm } ?: sortedKeyframes.first()
        val upper = sortedKeyframes.firstOrNull { it.altitudeKm >= altitudeKm } ?: sortedKeyframes.last()

        if (altitudeKey(lower.altitudeKm) == altitudeKey(upper.altitudeKm)) return lower.preferredDeg to lower.weight

        val t = ((altitudeKm - lower.altitudeKm) / (upper.altitudeKm - lower.altitudeKm)).coerceIn(0f, 1f)

        val lowerRad = Math.toRadians(lower.preferredDeg.toDouble())
        val upperRad = Math.toRadians(upper.preferredDeg.toDouble())
        val x = (1 - t) * cos(lowerRad) + t * cos(upperRad)
        val y = (1 - t) * sin(lowerRad) + t * sin(upperRad)
        var interpolatedDeg = Math.toDegrees(atan2(y, x)).toFloat()
        if (interpolatedDeg < 0f) interpolatedDeg += 360f

        val interpolatedWeight = (1 - t) * lower.weight + t * upper.weight

        return interpolatedDeg to interpolatedWeight
    }

    private fun angularDifference(a: Double, b: Double): Double {
        var diff = (a - b) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    fun calculateDirectionalScore(layers: List<Layer>?, keyframes: List<WindKeyframe>): Double? {
        if (layers.isNullOrEmpty() || keyframes.isEmpty()) return null

        val sortedKeyframes = keyframes.sortedBy { it.altitudeKm }

        var weightedSum = 0.0
        var totalWeight = 0.0

        for (layer in layers) {
            val altitudeKm = (layer.altitude / 1000.0).toFloat()
            val (preferredDeg, weight) = interpolateAt(altitudeKm, sortedKeyframes)

            if (weight <= 0f) continue

            val diffRad = Math.toRadians(angularDifference(layer.windDirection, preferredDeg.toDouble()))
            val cosDiff = cos(diffRad)
            val steepScore = sign(cosDiff) * abs(cosDiff).pow(FALLOFF_POWER)

            weightedSum += steepScore * weight
            totalWeight += weight
        }

        if (totalWeight == 0.0) return null

        val aggregate = weightedSum / totalWeight
        return ((aggregate + 1.0) / 2.0).coerceIn(0.0, 1.0)
    }
}