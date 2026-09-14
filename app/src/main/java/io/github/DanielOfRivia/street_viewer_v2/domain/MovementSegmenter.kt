package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace

/**
 * Splits a chronological track into the runs where the user was actually moving between known
 * stays, dropping any point whose timestamp falls inside a visited place's arrival-departure
 * window. Meant only for the track polyline/arrows -- GPS jitter while stationary at a place
 * otherwise reads as motion on the map even though the points are just noise around one spot.
 */
object MovementSegmenter {

    fun segmentByMovement(points: List<LocationPoint>, stays: List<VisitedPlace>): List<List<LocationPoint>> {
        if (points.isEmpty()) return emptyList()

        val segments = mutableListOf<List<LocationPoint>>()
        var current = mutableListOf<LocationPoint>()

        for (point in points.sortedBy { it.timestampMillis }) {
            val isAtAStay = stays.any { point.timestampMillis in it.arrivalTimeMillis..it.departureTimeMillis }
            if (isAtAStay) {
                if (current.isNotEmpty()) {
                    segments.add(current)
                    current = mutableListOf()
                }
            } else {
                current.add(point)
            }
        }
        if (current.isNotEmpty()) segments.add(current)

        return segments
    }
}
