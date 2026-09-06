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
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
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
    @Inject lateinit var trackingStatusRepository: TrackingStatusRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    }

    private fun onNewLocation(location: Location) {
        serviceScope.launch {
            locationPointRepository.insert(
                LocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestampMillis = location.time,
                    accuracyMeters = location.accuracy,
                )
            )
            sessionPointCount++
            startForeground(TrackingNotification.NOTIFICATION_ID, TrackingNotification.build(this@TrackingService, sessionPointCount))
        }
    }

    private fun stopTracking(reason: TrackingStopReason) {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        isTracking = false
        sessionPointCount = 0
        trackingStatusRepository.reportStopped(reason)
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

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
