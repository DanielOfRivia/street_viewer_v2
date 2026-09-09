package io.github.DanielOfRivia.street_viewer_v2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var trackingPreferencesRepository: TrackingPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (trackingPreferencesRepository.isTrackingRequested.first()) {
                    resumeTrackingOrNotify(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun resumeTrackingOrNotify(context: Context) {
        // BOOT_COMPLETED exempts the general background-service-start restriction, but not
        // the while-in-use rule specific to a `location` foreground service: without
        // ACCESS_BACKGROUND_LOCATION (deliberately not requested -- see the spec's own
        // constraint), this throws because the app is in the background at boot. The
        // failure can surface here at startForegroundService, or later inside the service
        // itself when it calls startForeground -- that second site is already handled by
        // TrackingService's own try/catch around it.
        try {
            TrackingService.start(context)
        } catch (e: Exception) {
            ResumeTrackingNotification.show(context)
        }
    }
}
