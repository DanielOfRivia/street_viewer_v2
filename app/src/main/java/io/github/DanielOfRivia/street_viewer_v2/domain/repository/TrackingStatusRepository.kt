package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStatus
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import kotlinx.coroutines.flow.StateFlow

interface TrackingStatusRepository {
    val status: StateFlow<TrackingStatus>
    fun reportStarted()
    fun reportStopped(reason: TrackingStopReason)
}
