package com.endiar.purrodoro.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.endiar.purrodoro.R

private const val CHANNEL_ID = "kuro_dev"
private const val NOTIFICATION_ID = 9001

fun postDevNotification(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)

    val channel = NotificationChannel(
        CHANNEL_ID,
        "Purrodoro Dev Tools",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Developer utility panel"
    }
    nm.createNotificationChannel(channel)

    val intent = Intent(context, DebugActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_monochrome)
        .setContentTitle("Purrodoro Dev Tools")
        .setContentText("Tap to open debug panel")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    nm.notify(NOTIFICATION_ID, notification)
}
