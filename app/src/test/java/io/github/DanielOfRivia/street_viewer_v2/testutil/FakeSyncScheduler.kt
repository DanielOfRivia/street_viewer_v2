package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncSchedulerState
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncScheduler : SyncScheduler {

    private val _state = MutableStateFlow<SyncSchedulerState>(SyncSchedulerState.Idle)
    override val state = _state

    var requestSyncCallCount = 0
        private set

    override fun requestSync() {
        requestSyncCallCount++
    }

    fun emit(state: SyncSchedulerState) {
        _state.value = state
    }
}
