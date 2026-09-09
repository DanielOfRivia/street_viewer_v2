package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    locationPointRepository: LocationPointRepository,
    private val trackingStatusRepository: TrackingStatusRepository,
    private val trackingPreferencesRepository: TrackingPreferencesRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val permissionState = MutableStateFlow(LocationPermissionState.Unknown)

    val uiState: StateFlow<TrackingUiState> = combine(
        locationPointRepository.observePointCount(),
        trackingStatusRepository.status,
        permissionState,
        syncScheduler.state,
    ) { pointCount, status, permission, syncState ->
        TrackingUiState(
            isTracking = status.isActive,
            pointCount = pointCount,
            stopReason = status.stopReason,
            permissionState = permission,
            syncState = syncState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingUiState(),
    )

    fun onPermissionStateChanged(state: LocationPermissionState) {
        permissionState.value = state
    }

    fun onSyncClick() {
        syncScheduler.requestSync()
    }

    /**
     * True if the user's last explicit action was starting tracking (not yet followed by an
     * explicit stop) and it isn't currently running -- covers both "resumed via the boot
     * notification" and "the service died some other way while the app was open the whole
     * time." Reads the DataStore flag directly rather than through [uiState] so a cold start
     * gets the real persisted value instead of uiState's initial default before its first
     * emission arrives.
     */
    suspend fun shouldAutoResumeTracking(): Boolean {
        val wantsTracking = trackingPreferencesRepository.isTrackingRequested.first()
        return wantsTracking && !trackingStatusRepository.status.value.isActive
    }
}
