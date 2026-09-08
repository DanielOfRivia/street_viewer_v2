package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncSchedulerState
import kotlinx.coroutines.flow.Flow

interface SyncScheduler {
    val state: Flow<SyncSchedulerState>
    fun requestSync()
}
