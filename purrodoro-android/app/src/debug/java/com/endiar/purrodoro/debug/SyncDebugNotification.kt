package com.endiar.purrodoro.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.endiar.purrodoro.R

private const val CHANNEL_ID = "kuro_sync_debug"
private const val NOTIFICATION_ID = 9002

fun postSyncDebugNotification(context: Context, summary: String) {
    val nm = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Purrodoro Sync Debug",
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = "Live sync pass outcomes (debug builds only)"
    }
    nm.createNotificationChannel(channel)

    val intent = Intent(context, SyncDebugActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val pendingIntent = PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_monochrome)
        .setContentTitle("Sync debug")
        .setContentText(summary)
        .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    nm.notify(NOTIFICATION_ID, notification)
}

fun cancelSyncDebugNotification(context: Context) {
    context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
}
