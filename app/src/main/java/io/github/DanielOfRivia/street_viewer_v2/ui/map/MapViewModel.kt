package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    locationPointRepository: LocationPointRepository,
    streetCoverageRepository: StreetCoverageRepository,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = locationPointRepository.observeAllPoints()
        .map { points ->
            MapUiState(
                points = points,
                visitedStreetRuns = streetCoverageRepository.getVisitedStreetRuns(points),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
        )
}
