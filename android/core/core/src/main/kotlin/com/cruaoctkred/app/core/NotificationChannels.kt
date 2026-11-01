package com.cruaoctkred.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

fun ensureNotificationChannel(
    context: Context,
    channelId: String,
    channelName: String,
    importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    enableSound: Boolean = false,
    enableVibration: Boolean = false,
    showBadge: Boolean = false,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(channelId, channelName, importance).apply {
            if (!enableSound) setSound(null, null)
            enableVibration(enableVibration)
            setShowBadge(showBadge)
        },
    )
}
