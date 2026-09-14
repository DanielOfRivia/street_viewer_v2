package io.github.DanielOfRivia.street_viewer_v2.ui.map

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import java.time.LocalDate

data class MapUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val points: List<LocationPoint> = emptyList(),
    val visitedStreetRuns: List<VisitedStreetRun> = emptyList(),
    val visitedPlaces: List<VisitedPlace> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val newestPoint: LocationPoint? get() = points.lastOrNull()
}
