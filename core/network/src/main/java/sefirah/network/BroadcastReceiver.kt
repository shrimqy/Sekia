package sefirah.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import sefirah.domain.interfaces.NetworkManager
import javax.inject.Inject

@AndroidEntryPoint
class BroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var networkManager: NetworkManager

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                ACTION_START -> networkManager.startService()
                ACTION_STOP -> networkManager.stopService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle action: ${intent.action}", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"
    }
}
