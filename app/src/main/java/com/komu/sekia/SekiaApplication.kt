package com.komu.sekia

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SekiaApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "WebSocket_Foreground_Service",
            "Device Connected",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}