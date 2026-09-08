package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncSchedulerState
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason

data class TrackingUiState(
    val isTracking: Boolean = false,
    val pointCount: Int = 0,
    val stopReason: TrackingStopReason? = null,
    val permissionState: LocationPermissionState = LocationPermissionState.Unknown,
    val syncState: SyncSchedulerState = SyncSchedulerState.Idle,
)

enum class LocationPermissionState {
    Unknown,
    Granted,
    Denied,
    PermanentlyDenied,
}
