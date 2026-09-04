package io.github.DanielOfRivia.street_viewer_v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.DanielOfRivia.street_viewer_v2.MainActivity
import io.github.DanielOfRivia.street_viewer_v2.R

object TrackingNotification {
    const val CHANNEL_ID = "tracking"
    const val NOTIFICATION_ID = 1

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.tracking_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.tracking_notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, pointCount: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentText(context.getString(R.string.tracking_notification_point_count, pointCount))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
