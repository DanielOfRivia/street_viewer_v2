package io.github.DanielOfRivia.street_viewer_v2.domain

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Small-scale (tens of meters) geometry helpers using a local equirectangular projection
 * centered on the query point. Not accurate at large distances, but well within a meter of
 * error at the scale this app uses it for (matching GPS points to nearby street segments).
 */
object GeoMath {
    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

    fun distanceToSegmentMeters(
        pointLat: Double,
        pointLon: Double,
        aLat: Double,
        aLon: Double,
        bLat: Double,
        bLon: Double,
    ): Double {
        val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(pointLat))

        // Project with the query point as the origin, so its own coordinates are (0, 0).
        val ax = (aLon - pointLon) * metersPerDegreeLongitude
        val ay = (aLat - pointLat) * METERS_PER_DEGREE_LATITUDE
        val bx = (bLon - pointLon) * metersPerDegreeLongitude
        val by = (bLat - pointLat) * METERS_PER_DEGREE_LATITUDE

        return distancePointToSegment(0.0, 0.0, ax, ay, bx, by)
    }

    private fun distancePointToSegment(
        px: Double,
        py: Double,
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double,
    ): Double {
        val abx = bx - ax
        val aby = by - ay
        val abLengthSquared = abx * abx + aby * aby

        val t = if (abLengthSquared == 0.0) {
            0.0
        } else {
            (((px - ax) * abx + (py - ay) * aby) / abLengthSquared).coerceIn(0.0, 1.0)
        }

        val closestX = ax + t * abx
        val closestY = ay + t * aby
        val dx = px - closestX
        val dy = py - closestY
        return sqrt(dx * dx + dy * dy)
    }
}
