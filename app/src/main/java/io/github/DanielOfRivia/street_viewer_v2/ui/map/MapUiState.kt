package io.github.DanielOfRivia.street_viewer_v2.ui.map

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint

data class MapUiState(
    val points: List<LocationPoint> = emptyList(),
) {
    val newestPoint: LocationPoint? get() = points.lastOrNull()
}
