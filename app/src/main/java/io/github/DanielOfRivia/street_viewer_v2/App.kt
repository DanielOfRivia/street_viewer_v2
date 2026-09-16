package io.github.DanielOfRivia.street_viewer_v2

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import io.github.DanielOfRivia.street_viewer_v2.service.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesDataStore: DataStore<Preferences>

    @Inject
    lateinit var locationHistoryRepository: LocationHistoryRepository

    @Inject
    lateinit var visitedStreetCoverageRepository: VisitedStreetCoverageRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // BitmapDescriptorFactory (used to build the direction-arrow icon on the map screen)
        // throws "IBitmapDescriptorFactory is not initialized" unless the Maps SDK has been
        // initialized at least once in the process -- normally a MapView does this itself,
        // but rememberArrowIcon() needs it synchronously, before any MapView has attached.
        MapsInitializer.initialize(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicRequest(),
        )
        appScope.launch { backfillVisitedStreetsOnce() }
    }

    // The visited-streets table only ever gets filled incrementally from new fixes as they're
    // tracked (see TrackingService) -- history recorded before this feature existed would
    // otherwise never be reflected. Runs once per install, using the same locations endpoint
    // the map already calls for historical days, just with the widest possible range.
    private suspend fun backfillVisitedStreetsOnce() {
        val alreadyBackfilled = preferencesDataStore.data.map { it[KEY_BACKFILLED] ?: false }
        if (alreadyBackfilled.firstOrNull() == true) return

        val result = locationHistoryRepository.getLocationsInRange(0L, System.currentTimeMillis())
        if (result is LocationHistoryResult.Success) {
            if (result.points.isNotEmpty()) {
                visitedStreetCoverageRepository.recordVisitedSegments(result.points)
            }
            preferencesDataStore.edit { it[KEY_BACKFILLED] = true }
        }
        // A Failure (offline, server down) leaves the flag unset -- retried on the next app
        // start rather than silently giving up on history that was never actually processed.
    }

    private companion object {
        val KEY_BACKFILLED = booleanPreferencesKey("visited_streets_backfilled")
    }
}
