package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTrackingPreferencesRepository : TrackingPreferencesRepository {

    private val requested = MutableStateFlow(false)
    override val isTrackingRequested = requested

    override suspend fun setTrackingRequested(requested: Boolean) {
        this.requested.value = requested
    }
}
