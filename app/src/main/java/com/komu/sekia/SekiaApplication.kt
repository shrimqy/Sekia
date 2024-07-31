package com.komu.sekia

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat.startForeground
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SekiaApplication: Application() {

}