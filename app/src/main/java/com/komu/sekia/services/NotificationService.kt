package com.komu.sekia.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.domain.models.NotificationAction
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.NotificationType
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    class ActiveNotificationsBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WebSocketService.ACTION_SEND_ACTIVE_NOTIFICATIONS -> {
                    val service = context as? NotificationService
                    service?.sendActiveNotifications()
                }
            }
        }
    }

    private val broadcastReceiver = ActiveNotificationsBroadcastReceiver()



    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Service created")
        val filter = IntentFilter(WebSocketService.ACTION_SEND_ACTIVE_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            sendBroadcast(Intent(WebSocketService.ACTION_SEND_ACTIVE_NOTIFICATIONS))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        scope.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationService", "Listener connected")
        // Now it's safe to call sendActiveNotifications
        sendActiveNotifications()
    }


    private fun sendActiveNotifications() {
        val activeNotifications = activeNotifications
        if (activeNotifications.isNullOrEmpty()) {
            Log.d("activeNotification", "No active notifications found.")
        } else {
            Log.d("activeNotification", "Active notifications found: ${activeNotifications.size}")
            val rankingMap = currentRanking
            activeNotifications.forEach { sbn ->
                sendNotification(sbn, rankingMap, NotificationType.ACTIVE)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationService", "Notification posted: ${sbn.packageName}")
        sendNotification(sbn, currentRanking, NotificationType.NEW)
    }

    private fun sendNotification(sbn: StatusBarNotification, rankingMap: RankingMap?, notificationType: NotificationType) {
        val notification = sbn.notification
        // Check if the notification is ongoing
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            Log.d("NotificationService", "Skipping ongoing notification: ${sbn.packageName}")
            return
        }
        val packageName = sbn.packageName

        // Get the app name using PackageManager
        val packageManager = packageManager

        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown App"
        }

        // Get app icon
        val appIcon = try {
            val appIconDrawable = packageManager.getApplicationIcon(packageName)
            if (appIconDrawable is BitmapDrawable) {
                val appIconBitmap = appIconDrawable.bitmap
                bitmapToBase64(appIconBitmap)
            } else {
                // Convert to Bitmap if it's not already a BitmapDrawable
                val appIconBitmap = drawableToBitmap(appIconDrawable)
                bitmapToBase64(appIconBitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        // Get the notification large icon
        val largeIcon = notification.getLargeIcon()?.let { icon ->
            val largeIconBitmap = icon.loadDrawable(this)?.let { (it as BitmapDrawable).bitmap }
            largeIconBitmap?.let { bitmapToBase64(it) }
        }

        // Get picture (if available)
        val picture = notification.extras.get(Notification.EXTRA_PICTURE)?.let { pictureBitmap ->
            bitmapToBase64(pictureBitmap as Bitmap)
        }

        val title = notification.extras.getString(Notification.EXTRA_TITLE)
        val text = notification.extras.getString(Notification.EXTRA_TEXT)
        val id = notification.extras.getString(Notification.EXTRA_NOTIFICATION_ID)
        val tag = notification.extras.getString(Notification.EXTRA_NOTIFICATION_TAG)
        Log.d("NotificationService", sbn.toString())

        // Retrieve the ranking of the notification
        val ranking = Ranking()
        val rankingImportance = if (rankingMap?.getRanking(sbn.key, ranking) == true) {
            ranking.importance
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

        // Retrieve the actions from the notification
        val actions = notification.actions?.map { action ->
            NotificationAction(
                label = action.title.toString(),
                actionId = action.actionIntent.toString()
            )
        }

        val notificationMessage = NotificationMessage(
            appName = appName,
            title = title,
            text = text,
            actions = actions,
            appIcon = appIcon,
            largeIcon = largeIcon,
            bigPicture = picture,
            tag = sbn.key,
            groupKey = sbn.groupKey,
            notificationType = notificationType
        )
        scope.launch {
            try {
                Log.d("NotificationService", "${notificationMessage.appName} ${notificationMessage.title} ${notificationMessage.text} ${notificationMessage.tag} ${notificationMessage.groupKey}")
                webSocketRepository.sendMessage(notificationMessage)
            } catch (e: Exception) {
                Log.e("NotificationService", "Failed to send notification message", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationService", "Notification removed: ${sbn.packageName}")
        // Handle notification removal if necessary
    }
}


private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

// Helper function to convert a Drawable to Bitmap
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}