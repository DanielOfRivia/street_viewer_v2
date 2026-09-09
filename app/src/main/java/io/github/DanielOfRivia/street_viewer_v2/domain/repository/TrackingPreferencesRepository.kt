package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import kotlinx.coroutines.flow.Flow

interface TrackingPreferencesRepository {
    val isTrackingRequested: Flow<Boolean>
    suspend fun setTrackingRequested(requested: Boolean)
}
