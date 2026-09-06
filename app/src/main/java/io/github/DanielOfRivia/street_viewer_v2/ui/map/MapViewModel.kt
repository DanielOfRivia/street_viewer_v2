package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    locationPointRepository: LocationPointRepository,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = locationPointRepository.observeAllPoints()
        .map { points -> MapUiState(points = points) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
        )
}
