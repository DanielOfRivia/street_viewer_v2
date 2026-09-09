package io.github.DanielOfRivia.street_viewer_v2.ui.map

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun

data class MapUiState(
    val points: List<LocationPoint> = emptyList(),
    val visitedStreetRuns: List<VisitedStreetRun> = emptyList(),
) {
    val newestPoint: LocationPoint? get() = points.lastOrNull()
}
