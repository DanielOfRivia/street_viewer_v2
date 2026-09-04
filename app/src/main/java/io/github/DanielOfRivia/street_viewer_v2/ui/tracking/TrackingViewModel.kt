package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    locationPointRepository: LocationPointRepository,
    trackingStatusRepository: TrackingStatusRepository,
) : ViewModel() {

    private val permissionState = MutableStateFlow(LocationPermissionState.Unknown)

    val uiState: StateFlow<TrackingUiState> = combine(
        locationPointRepository.observePointCount(),
        trackingStatusRepository.status,
        permissionState,
    ) { pointCount, status, permission ->
        TrackingUiState(
            isTracking = status.isActive,
            pointCount = pointCount,
            stopReason = status.stopReason,
            permissionState = permission,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingUiState(),
    )

    fun onPermissionStateChanged(state: LocationPermissionState) {
        permissionState.value = state
    }
}
