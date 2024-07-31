package com.komu.sekia.services

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.domain.models.NotificationAction
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Service created")
        sendActiveNotifications()
    }

    private fun sendActiveNotifications() {
        val activeNotifications = activeNotifications
        if (activeNotifications.isNullOrEmpty()) {
            Log.d("NotificationService", "No active notifications found.")
        } else {
            Log.d("NotificationService", "Active notifications found: ${activeNotifications.size}")
            val rankingMap = currentRanking
            activeNotifications.forEach { sbn ->
                sendNotification(sbn, rankingMap)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationService", "Notification posted: ${sbn.packageName}")
        sendNotification(sbn, currentRanking)
    }

    private fun sendNotification(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        val notification = sbn.notification
        val packageName = sbn.packageName
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: "No Text"
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
                actionId = action.actionIntent?.creatorPackage ?: "unknown"
            )
        }

        val notificationMessage = NotificationMessage(
            packageName = packageName,
            title = title,
            text = text,
            actions = actions,
        )

        scope.launch {
            try {
                Log.d("NotificationService", "Sending notification: $notificationMessage")
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
