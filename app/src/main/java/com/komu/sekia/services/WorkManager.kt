package com.komu.sekia.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class StartWebSocketWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val hostAddress = inputData.getString(NetworkService.EXTRA_HOST_ADDRESS)
        val deviceName = inputData.getString(NetworkService.DEVICE_NAME)

        if (hostAddress != null) {
            val intent = Intent(applicationContext, NetworkService::class.java).apply {
                action = Actions.START.name
                putExtra(NetworkService.EXTRA_HOST_ADDRESS, hostAddress)
                putExtra(NetworkService.DEVICE_NAME, deviceName)
            }
            Log.d("worker", "Worker started Service")
            applicationContext.startForegroundService(intent)
        }

        return Result.success()
    }
}