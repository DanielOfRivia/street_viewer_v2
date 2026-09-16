package io.github.DanielOfRivia.street_viewer_v2.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var locationPointRepository: LocationPointRepository
    @Inject lateinit var visitedStreetCoverageRepository: VisitedStreetCoverageRepository
    @Inject lateinit var trackingStatusRepository: TrackingStatusRepository
    @Inject lateinit var trackingPreferencesRepository: TrackingPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastAcceptedPoint: LocationPoint? = null
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
        // setIntervalMillis (set above via the constructor) is only a target, not a floor:
        // the fused provider can deliver a fresher/better fix ahead of schedule (observed
        // on-device as low as ~14s between fixes with this left unset). This is the actual
        // floor. Deliberately not paired with an app-level "reject if too early" guard: the
        // provider's own next-delivery clock resets from whenever it last delivered
        // regardless of whether the app used that fix, so rejecting a fix doesn't get a
        // do-over on our schedule — it can just push the next one to 2x the interval away.
        .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MILLIS)
        .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
        .build()

    private var locationCallback: LocationCallback? = null
    private var isTracking = false
    private var sessionPointCount = 0

    override fun onCreate() {
        super.onCreate()
        TrackingNotification.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking(TrackingStopReason.USER_REQUESTED)
        } else {
            startTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTracking() {
        if (isTracking) return

        // Persists the user's intent, not whether this attempt actually succeeds: a resume
        // attempt (boot, sticky restart) should keep retrying on future occasions even if
        // this particular attempt fails below, e.g. because permission is still missing.
        serviceScope.launch { trackingPreferencesRepository.setTrackingRequested(true) }

        if (!hasLocationPermission()) {
            stopTracking(TrackingStopReason.PERMISSION_MISSING)
            return
        }

        try {
            startForeground(TrackingNotification.NOTIFICATION_ID, TrackingNotification.build(this, sessionPointCount))
        } catch (e: Exception) {
            stopTracking(TrackingStopReason.START_FOREGROUND_FAILED)
            return
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onNewLocation(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { stopTracking(TrackingStopReason.LOCATION_REQUEST_FAILED) }
        } catch (e: SecurityException) {
            stopTracking(TrackingStopReason.PERMISSION_MISSING)
            return
        }

        locationCallback = callback
        isTracking = true
        trackingStatusRepository.reportStarted()

        // Restores the gap-fill bridge across a service restart (sticky restart, reboot
        // resume) -- without this, the first fix after a restart would be treated as if
        // nothing came before it, missing whatever street segment lies between the two.
        serviceScope.launch {
            lastAcceptedPoint = locationPointRepository.getMostRecentPoint()
        }
    }

    private fun onNewLocation(location: Location) {
        // A fix this imprecise (weak signal, indoors, urban canyon, GPS still warming up)
        // would show up on the map as a spurious jump off the actual street rather than
        // genuine movement. Silently dropped, same as the interval/distance filters above --
        // there's always a next fix, no need to surface this to the user.
        if (location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) return

        serviceScope.launch {
            val point = LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestampMillis = location.time,
                accuracyMeters = location.accuracy,
            )
            locationPointRepository.insert(point)
            sessionPointCount++
            startForeground(TrackingNotification.NOTIFICATION_ID, TrackingNotification.build(this@TrackingService, sessionPointCount))

            val previous = lastAcceptedPoint
            lastAcceptedPoint = point
            // Decorative/best-effort, same as everywhere else this app talks to Overpass --
            // never let a coverage-matching hiccup interrupt tracking itself. Whatever isn't
            // recorded this time gets picked up again on the next fix.
            try {
                visitedStreetCoverageRepository.recordVisitedSegments(listOfNotNull(previous, point))
            } catch (e: Exception) {
                // ignored
            }
        }
    }

    private fun stopTracking(reason: TrackingStopReason) {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        isTracking = false
        sessionPointCount = 0
        trackingStatusRepository.reportStopped(reason)
        // Only an explicit user stop clears the "resume on reboot" intent. A failure-driven
        // stop (permission revoked, start-foreground rejected, ...) means tracking should
        // still come back once the obstacle is gone -- it wasn't the user asking for it off.
        if (reason == TrackingStopReason.USER_REQUESTED) {
            serviceScope.launch { trackingPreferencesRepository.setTrackingRequested(false) }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    companion object {
        private const val ACTION_STOP = "io.github.DanielOfRivia.street_viewer_v2.action.STOP"
        private const val UPDATE_INTERVAL_MILLIS = 30_000L
        private const val MIN_UPDATE_DISTANCE_METERS = 10f
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 50f

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
