package io.github.DanielOfRivia.street_viewer_v2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreTrackingPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TrackingPreferencesRepository {

    override val isTrackingRequested: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TRACKING_REQUESTED] ?: false
    }

    override suspend fun setTrackingRequested(requested: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_TRACKING_REQUESTED] = requested }
    }

    private companion object {
        val KEY_TRACKING_REQUESTED = booleanPreferencesKey("tracking_requested")
    }
}
