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
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import io.github.DanielOfRivia.street_viewer_v2.service.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
    lateinit var locationPointRepository: LocationPointRepository

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
        appScope.launch { rebuildVisitedStreetsOnce() }
    }

    // The visited-streets table only ever gets filled incrementally from new fixes as they're
    // tracked (see TrackingService) -- history recorded before this feature existed would
    // otherwise never be reflected, and neither would a change to the matching rules (e.g. the
    // walking-speed filter, which should also un-colour streets only ever driven along). Runs
    // once per install per KEY_REBUILT, using the same locations endpoint the map already calls
    // for historical days, just with the widest possible range.
    private suspend fun rebuildVisitedStreetsOnce() {
        val alreadyRebuilt = preferencesDataStore.data.map { it[KEY_REBUILT] ?: false }
        if (alreadyRebuilt.firstOrNull() == true) return

        val result = locationHistoryRepository.getLocationsInRange(0L, System.currentTimeMillis())
        // A Failure (offline, server down) leaves the flag unset -- retried on the next app
        // start rather than wiping coverage for history that was never actually processed.
        if (result !is LocationHistoryResult.Success) return

        // The server lacks whatever hasn't been uploaded yet -- without those, the rebuild would
        // wipe streets walked since the last sync. Matched on timestamp, preserved exactly
        // through upload.
        val serverTimestamps = result.points.mapTo(HashSet()) { it.timestampMillis }
        val localOnly = locationPointRepository.observeAllPoints().first()
            .filter { it.timestampMillis !in serverTimestamps }

        // False when Overpass couldn't be reached -- existing coverage is kept as is, retried
        // on the next app start.
        if (visitedStreetCoverageRepository.rebuildVisitedSegments(result.points + localOnly)) {
            preferencesDataStore.edit { it[KEY_REBUILT] = true }
        }
    }

    private companion object {
        // Bump the version to force another rebuild after the next matching-rule change.
        val KEY_REBUILT = booleanPreferencesKey("visited_streets_rebuilt_v2")
    }
}
