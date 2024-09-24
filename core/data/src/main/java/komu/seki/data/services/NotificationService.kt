package komu.seki.data.services

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.common.util.Constants.ACTION_CLEAR_NOTIFICATIONS
import komu.seki.common.util.Constants.ACTION_REMOVE_NOTIFICATION
import komu.seki.common.util.Constants.ACTION_SEND_ACTIVE_NOTIFICATIONS
import komu.seki.common.util.Constants.ACTION_STOP_NOTIFICATION_SERVICE
import komu.seki.common.util.Constants.NOTIFICATION_ID
import komu.seki.common.util.bitmapToBase64
import komu.seki.common.util.drawableToBitmap
import komu.seki.domain.models.Message
import komu.seki.domain.models.NotificationAction
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.NotificationType
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.getDefault
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    class NotificationsBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SEND_ACTIVE_NOTIFICATIONS -> {
                    val service = context as? NotificationService
                    service?.sendActiveNotifications()
                }

                ACTION_REMOVE_NOTIFICATION -> {
                    Log.d("Notification", "Notification Remove Action Received")
                    val notificationId = intent.getStringExtra(NOTIFICATION_ID)
                    Log.d("NotificationService", "Notification to remove: $notificationId")
                    val service = context as? NotificationService
                    service?.removeNotification(notificationId)
                }

                ACTION_CLEAR_NOTIFICATIONS -> {
                    val service = context as? NotificationService
                    service?.cancelAllNotifications()
                }
            }
        }
    }

    private val broadcastReceiver = NotificationsBroadcastReceiver()

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Service created")
        val filter = IntentFilter().apply {
            addAction(ACTION_SEND_ACTIVE_NOTIFICATIONS)
            addAction(ACTION_REMOVE_NOTIFICATION)
            addAction(ACTION_CLEAR_NOTIFICATIONS)
            addAction(ACTION_STOP_NOTIFICATION_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            val intent = Intent(ACTION_SEND_ACTIVE_NOTIFICATIONS)
            intent.setClassName(this, "komu.seki.data.services.NotificationService\$NotificationsBroadcastReceiver")
            sendBroadcast(intent)
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
        scope.launch {
            val activeNotifications = activeNotifications
            if (activeNotifications.isNullOrEmpty()) {
                Log.d("activeNotification", "No active notifications found.")
            } else {
                Log.d("activeNotification", "Active notifications found: ${activeNotifications.size}")
                val rankingMap = currentRanking
                activeNotifications.forEach { sbn ->
                    sendNotification(sbn, rankingMap, NotificationType.ACTIVE)
                    delay(50)
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationService", "Notification posted: ${sbn.packageName}")
        sendNotification(sbn, currentRanking, NotificationType.NEW)
    }

    private fun sendNotification(sbn: StatusBarNotification, rankingMap: RankingMap?, notificationType: NotificationType) {
        val notification = sbn.notification
        val extras = notification.extras
        // Check for progress-related extras
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        val hasProgress = (progress >= 1 || maxProgress >= 1) || isIndeterminate
        // Check if the notification is ongoing, media-style, or belongs to the 'progress' category
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT != 0 && notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0)
            || notification.isMediaStyle()
            || hasProgress) {
            return
        }

        val context = this
        scope.launch {
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

            val notificationKey = sbn.key

            // Get the notification large icon
            val largeIcon = notification.getLargeIcon()?.let { icon ->
                val largeIconBitmap = icon.loadDrawable(context)?.let { (it as BitmapDrawable).bitmap }
                largeIconBitmap?.let { bitmapToBase64(it) }
            }

            // Get picture (if available)
            val picture = notification.extras.get(Notification.EXTRA_PICTURE)?.let { pictureBitmap ->
                bitmapToBase64(pictureBitmap as Bitmap)
            }

            // Use the utility function to get text from SpannableString
            val title = getSpannableText(notification.extras.getCharSequence(Notification.EXTRA_TITLE))
                ?: getSpannableText(notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG))

            val text = getSpannableText(notification.extras.getCharSequence(Notification.EXTRA_TEXT))
                ?: getSpannableText(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                ?: getSpannableText(notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))

            val messages = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.mapNotNull {
                val bundle = it as? Bundle
                val sender = bundle?.getCharSequence("sender")?.toString() // Get the sender's name
                val messageText = bundle?.getCharSequence("text")?.toString()

                if (sender != null && messageText != null) {
                    Message(sender = sender, text = messageText)
                } else {
                    null
                }
            } ?: emptyList()

            // Get the timestamp of the notification
            val timestamp = notification.`when`

            // Convert timestamp to a human-readable format if needed
            val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getDefault()).format(Date(timestamp))

            Log.d("message", "$appName $title $text messages: $messages $formattedTimestamp")
            Log.d("NotificationService", notification.toString())

            // Retrieve the ranking of the notification
            val ranking = Ranking()
            val rankingImportance = if (rankingMap?.getRanking(sbn.key, ranking) == true) {
                ranking.importance
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }

            // Retrieve the actions from the notification
            val actions = notification.actions?.map { action ->
                try {
                    // Extract action label and intent if they exist
                    val actionLabel = action.title?.toString() ?: "Unknown Action"
                    val actionIntent = action.actionIntent?.toString() ?: "No Action Intent"

                    Log.d("NotificationService", "Found action: $actionLabel with intent: $actionIntent")

                    // Return a new NotificationAction object with the label and intent
                    NotificationAction(
                        label = actionLabel,
                        actionId = actionIntent
                    )
                } catch (e: Exception) {
                    // Log any error that occurs while processing the actions
                    Log.e("NotificationService", "Error retrieving action: ${e.localizedMessage}")
                    null
                }
            } ?: emptyList()

            val notificationMessage = NotificationMessage(
                notificationKey = notificationKey,
                appName = appName,
                title = title,
                text = text,
                messages = messages,
                actions = actions,
                timestamp = formattedTimestamp,
                appIcon = appIcon,
                largeIcon = largeIcon,
                bigPicture = picture,
                tag = sbn.key,
                groupKey = sbn.groupKey,
                notificationType = notificationType
            )

            if (notificationMessage.appName == "WhatsApp" && notificationMessage.messages?.isEmpty() == true
                || notificationMessage.appName == "Spotify" && notificationMessage.timestamp == "1970-01-01 05:30:00") {
                Log.d("NotificationService", "Duplicate notification, ignoring...")
                return@launch
            }

            try {
//                Log.d("NotificationService", "${notificationMessage.appName} ${notificationMessage.title} ${notificationMessage.text} ${notificationMessage.tag} ${notificationMessage.groupKey}")
                webSocketRepository.sendMessage(notificationMessage)
            } catch (e: Exception) {
                Log.e("NotificationService", "Failed to send notification message", e)
            }
        }
    }

    fun removeNotification(notificationId: String?) {
        try {
            cancelNotification(notificationId)
            Log.d("NotificationService", "Notification removed: $notificationId")
        } catch (e: Exception) {
            Log.e("NotificationService", "${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationService", "Notification removed: ${sbn.packageName}")

        val notificationKey = sbn.key
        val notificationId = sbn.id
        val notificationTag = sbn.tag


        // Send a message to your desktop app to remove the notification
        val removeNotificationMessage = NotificationMessage(
            notificationKey = notificationKey,
            notificationType = NotificationType.REMOVED,
            tag = notificationTag,
        )


        scope.launch {
            webSocketRepository.sendMessage(removeNotificationMessage)
        }
    }
    private fun getSpannableText(charSequence: CharSequence?): String? {
        return when (charSequence) {
            is SpannableString -> charSequence.toString()
            else -> charSequence?.toString()
        }
    }
    private fun Notification.isMediaStyle(): Boolean {
        val mediaStyleClassName = "android.app.Notification\$MediaStyle"
        return mediaStyleClassName == this.extras.getString(Notification.EXTRA_TEMPLATE)
    }
}


