package io.github.DanielOfRivia.street_viewer_v2.data.local

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun

fun VisitedStreetSegmentEntity.toDomain() = VisitedStreetRun(
    points = listOf(LatLon(startLat, startLon), LatLon(endLat, endLon)),
)
