package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace

/**
 * Splits a chronological track into the runs where the user was actually moving between known
 * stays, dropping any point whose timestamp falls inside a visited place's arrival-departure
 * window. Meant only for the track polyline/arrows -- GPS jitter while stationary at a place
 * otherwise reads as motion on the map even though the points are just noise around one spot.
 *
 * A run adjacent to a stay is anchored to that place's own pin at the near end, rather than
 * starting/ending wherever the nearest real fix happens to be -- GPS fixes are only recorded
 * periodically, so the first fix after leaving (or last fix before arriving) can be a real
 * distance from the place itself, which otherwise reads as the line starting or stopping short
 * of the pin for no reason.
 */
object MovementSegmenter {

    fun segmentByMovement(points: List<LocationPoint>, stays: List<VisitedPlace>): List<List<LocationPoint>> {
        if (points.isEmpty()) return emptyList()

        val segments = mutableListOf<List<LocationPoint>>()
        var current = mutableListOf<LocationPoint>()
        var justExitedStay: VisitedPlace? = null

        for (point in points.sortedBy { it.timestampMillis }) {
            val stay = stays.find { point.timestampMillis in it.arrivalTimeMillis..it.departureTimeMillis }
            if (stay != null) {
                if (current.isNotEmpty()) {
                    current.add(anchorPoint(stay, current.last().timestampMillis))
                    segments.add(current)
                    current = mutableListOf()
                }
                justExitedStay = stay
            } else {
                justExitedStay?.let { stay ->
                    current.add(anchorPoint(stay, point.timestampMillis))
                }
                justExitedStay = null
                current.add(point)
            }
        }
        if (current.isNotEmpty()) segments.add(current)

        return segments
    }

    private fun anchorPoint(stay: VisitedPlace, timestampMillis: Long) = LocationPoint(
        latitude = stay.latitude,
        longitude = stay.longitude,
        timestampMillis = timestampMillis,
        accuracyMeters = 0f,
    )
}
