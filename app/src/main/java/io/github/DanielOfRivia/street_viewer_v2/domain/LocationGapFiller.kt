package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import kotlin.math.ceil

/**
 * Fills gaps between consecutive recorded points with synthetic in-between points, so street
 * coverage matching (a fixed-radius check against each point) doesn't leave uncoloured holes
 * on streets between two genuine fixes that are far apart in distance but close in time.
 *
 * Deliberately not used for the raw track polyline or point count -- these points aren't real
 * GPS fixes, only a straight-line approximation between two real ones, and are meant to exist
 * only for the duration of a single coverage computation, not stored or persisted anywhere.
 */
object LocationGapFiller {

    fun fillGaps(
        points: List<LocationPoint>,
        maxTimeGapMillis: Long = 90_000L,
        maxSegmentMeters: Double = 30.0,
    ): List<LocationPoint> {
        if (points.size < 2) return points

        val sorted = points.sortedBy { it.timestampMillis }
        val result = ArrayList<LocationPoint>(sorted.size)
        result.add(sorted.first())

        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val timeGapMillis = b.timestampMillis - a.timestampMillis

            // A large time gap likely means tracking was off and back on -- the straight line
            // between a and b would cut across whatever the user actually did in between,
            // which is not something to paper over with invented points.
            if (timeGapMillis < maxTimeGapMillis) {
                val distanceMeters = GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                val segmentCount = ceil(distanceMeters / maxSegmentMeters).toInt().coerceAtLeast(1)
                for (segment in 1 until segmentCount) {
                    result.add(interpolate(a, b, segment.toDouble() / segmentCount))
                }
            }

            result.add(b)
        }

        return result
    }

    private fun interpolate(a: LocationPoint, b: LocationPoint, fraction: Double): LocationPoint = LocationPoint(
        id = 0,
        latitude = a.latitude + (b.latitude - a.latitude) * fraction,
        longitude = a.longitude + (b.longitude - a.longitude) * fraction,
        timestampMillis = a.timestampMillis + ((b.timestampMillis - a.timestampMillis) * fraction).toLong(),
        accuracyMeters = a.accuracyMeters + (b.accuracyMeters - a.accuracyMeters) * fraction.toFloat(),
    )
}
