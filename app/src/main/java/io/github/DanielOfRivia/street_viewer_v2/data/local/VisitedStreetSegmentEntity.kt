package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Entity

/**
 * One street segment (a single pair of consecutive nodes on an OSM way) recorded as visited.
 * Never purged by the location-point retention window -- this is the whole point of persisting
 * the matched result instead of the raw points that produced it.
 */
@Entity(tableName = "visited_street_segments", primaryKeys = ["wayId", "segmentIndex"])
data class VisitedStreetSegmentEntity(
    val wayId: Long,
    val segmentIndex: Int,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
)
