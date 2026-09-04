package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStatus
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingStatusRepositoryImpl @Inject constructor() : TrackingStatusRepository {

    private val _status = MutableStateFlow(TrackingStatus())
    override val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    override fun reportStarted() {
        _status.value = TrackingStatus(isActive = true, stopReason = null)
    }

    override fun reportStopped(reason: TrackingStopReason) {
        _status.value = TrackingStatus(isActive = false, stopReason = reason)
    }
}
