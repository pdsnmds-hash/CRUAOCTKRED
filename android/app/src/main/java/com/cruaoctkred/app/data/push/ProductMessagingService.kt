package com.cruaoctkred.app.data.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.cruaoctkred.app.ProductApp
import com.cruaoctkred.app.MainActivity
import com.cruaoctkred.app.R
import com.cruaoctkred.app.core.ensureNotificationChannel

class ProductMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        val repository = (application as ProductApp).container.pushTokenRepository
        scope.launch { repository.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        ensurePushNotificationChannel(this)
        val manager = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(title.hashCode(), NotificationCompat.Builder(this, PUSH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build())
    }
}

internal const val PUSH_CHANNEL_ID = "product_updates"

internal fun ensurePushNotificationChannel(context: Context) {
    ensureNotificationChannel(context, PUSH_CHANNEL_ID, context.getString(R.string.app_name))
}
