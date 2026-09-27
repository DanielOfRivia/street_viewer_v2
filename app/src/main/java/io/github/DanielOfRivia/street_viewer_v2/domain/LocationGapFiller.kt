package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import kotlin.math.ceil

/**
 * Prepares recorded points for street coverage matching (a fixed-radius check against each
 * point): drops points recorded while travelling too fast to have actually walked the street,
 * and fills gaps between the remaining consecutive fixes with synthetic in-between points, so
 * streets between two genuine fixes that are far apart in distance but close in time don't
 * end up with uncoloured holes.
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
        maxSpeedKmh: Double = 35.0,
    ): List<LocationPoint> {
        if (points.size < 2) return points

        val sorted = points.sortedBy { it.timestampMillis }
        val pairIsFast = BooleanArray(sorted.size - 1) { i -> isFast(sorted[i], sorted[i + 1], maxSpeedKmh) }

        // Driving, a bus, a train: every fix along the way sits right on a street the user only
        // passed through. A point is only kept if it's joined to at least one neighbour at a
        // slow pace -- the first/last fix of a walk stays, even when the leg on its other side
        // is a drive. Applies across long gaps too: surfacing from a subway a few km away after
        // 10 minutes underground is a fast pair, so a lone fix picked up at a station mid-ride
        // is dropped, while the walk on either end still counts.
        val kept = sorted.filterIndexed { i, _ ->
            val slowBefore = i > 0 && !pairIsFast[i - 1]
            val slowAfter = i < pairIsFast.size && !pairIsFast[i]
            slowBefore || slowAfter
        }
        if (kept.size < 2) return kept

        val result = ArrayList<LocationPoint>(kept.size)
        result.add(kept.first())

        for (i in 0 until kept.size - 1) {
            val a = kept[i]
            val b = kept[i + 1]
            val timeGapMillis = b.timestampMillis - a.timestampMillis

            // A large time gap likely means tracking was off and back on, or no fix could be had
            // (underground, indoors) -- the straight line between a and b would cut across
            // whatever the user actually did in between, which is not something to paper over
            // with invented points. Neither is a fast pair that survived filtering only because
            // each end is part of a separate walk.
            if (timeGapMillis < maxTimeGapMillis && !isFast(a, b, maxSpeedKmh)) {
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

    private fun isFast(a: LocationPoint, b: LocationPoint, maxSpeedKmh: Double): Boolean {
        val timeGapMillis = b.timestampMillis - a.timestampMillis
        if (timeGapMillis <= 0) return false
        val distanceMeters = GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        val speedKmh = distanceMeters / (timeGapMillis / 1000.0) * 3.6
        return speedKmh > maxSpeedKmh
    }

    private fun interpolate(a: LocationPoint, b: LocationPoint, fraction: Double): LocationPoint = LocationPoint(
        id = 0,
        latitude = a.latitude + (b.latitude - a.latitude) * fraction,
        longitude = a.longitude + (b.longitude - a.longitude) * fraction,
        timestampMillis = a.timestampMillis + ((b.timestampMillis - a.timestampMillis) * fraction).toLong(),
        accuracyMeters = a.accuracyMeters + (b.accuracyMeters - a.accuracyMeters) * fraction.toFloat(),
    )
}
