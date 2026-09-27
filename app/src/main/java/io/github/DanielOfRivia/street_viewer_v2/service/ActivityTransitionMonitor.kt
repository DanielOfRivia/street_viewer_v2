package io.github.DanielOfRivia.street_viewer_v2.service

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Reports when the device settles down or starts moving again, via the Activity Recognition
 * Transition API -- lets [TrackingService] switch GPS off while the user sits still. Measured
 * on-device, GPS stayed on through hours of deep idle otherwise, which was nearly all of the
 * tracking's battery cost.
 *
 * Optional by design: without the ACTIVITY_RECOGNITION permission (or if the request fails),
 * [onStationaryChanged] is simply never called and tracking keeps full-accuracy GPS throughout.
 */
class ActivityTransitionMonitor(
    private val context: Context,
    private val client: ActivityRecognitionClient,
    private val onStationaryChanged: (Boolean) -> Unit,
) {
    private var receiver: BroadcastReceiver? = null
    private var pendingIntent: PendingIntent? = null

    fun start() {
        if (receiver != null || !hasPermission()) return

        val newReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!ActivityTransitionResult.hasResult(intent)) return
                // Only STILL is subscribed to, so the latest event alone says where things stand:
                // ENTER means stationary, EXIT means moving again.
                val latest = ActivityTransitionResult.extractResult(intent)?.transitionEvents?.lastOrNull() ?: return
                onStationaryChanged(latest.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            }
        }
        // Not exported: the PendingIntent below is sent under this app's own identity, so the
        // not-exported receiver still gets it while nothing else on the device can.
        ContextCompat.registerReceiver(
            context, newReceiver, IntentFilter(ACTION_TRANSITION), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // Mutable: Play services fills the transition result into this intent's extras.
        // Explicit (package set), as Android 14+ requires of a mutable PendingIntent.
        val newPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_TRANSITION).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        receiver = newReceiver
        pendingIntent = newPendingIntent

        try {
            client.requestActivityTransitionUpdates(ActivityTransitionRequest(TRANSITIONS), newPendingIntent)
                .addOnFailureListener { stop() }
        } catch (e: SecurityException) {
            stop()
        }
    }

    fun stop() {
        pendingIntent?.let { pending ->
            try {
                client.removeActivityTransitionUpdates(pending)
            } catch (e: SecurityException) {
                // Permission revoked since start() -- Play services drops the subscription itself.
            }
            pending.cancel()
        }
        pendingIntent = null
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val ACTION_TRANSITION = "io.github.DanielOfRivia.street_viewer_v2.action.ACTIVITY_TRANSITION"

        val TRANSITIONS = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
        )
    }
}
