package io.github.DanielOfRivia.street_viewer_v2.domain.model

sealed interface SyncSchedulerState {
    data object Idle : SyncSchedulerState
    data object Running : SyncSchedulerState
    data class Succeeded(val uploadedCount: Int) : SyncSchedulerState
    data class Failed(val uploadedCount: Int, val reason: String) : SyncSchedulerState
}
