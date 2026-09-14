package io.github.DanielOfRivia.street_viewer_v2

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp
import io.github.DanielOfRivia.street_viewer_v2.service.SyncWorker
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

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
    }
}
