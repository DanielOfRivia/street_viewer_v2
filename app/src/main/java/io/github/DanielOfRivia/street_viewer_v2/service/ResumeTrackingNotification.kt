package io.github.DanielOfRivia.street_viewer_v2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.DanielOfRivia.street_viewer_v2.MainActivity
import io.github.DanielOfRivia.street_viewer_v2.R

/**
 * The fallback shown when a boot-triggered resume can't start the tracking service directly
 * (no ACCESS_BACKGROUND_LOCATION -- see TrackingService/BootCompletedReceiver). Tapping it
 * opens the app from the foreground, where starting the service is unrestricted; the actual
 * resume happens via TrackingViewModel.shouldAutoResumeTracking(), not this notification.
 */
object ResumeTrackingNotification {
    private const val CHANNEL_ID = "resume_tracking"
    private const val NOTIFICATION_ID = 2

    fun show(context: Context) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.resume_tracking_notification_title))
            .setContentText(context.getString(R.string.resume_tracking_notification_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Posting without checking POST_NOTIFICATIONS is deliberate best-effort: a
        // BroadcastReceiver has no UI to request it from, and if it's denied this simply
        // doesn't show, same tradeoff as the ongoing tracking notification.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.resume_tracking_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
