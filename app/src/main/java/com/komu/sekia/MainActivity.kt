package com.komu.sekia

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.komu.sekia.navigation.graphs.RootNavGraph
import com.komu.sekia.services.Actions
import com.komu.sekia.services.WebSocketService
import com.komu.sekia.ui.base.BaseActivity
import com.komu.sekia.ui.theme.SekiraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.startWebSocketService(this)
        checkNotificationListenerPermission()
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.splashCondition }
        }
        enableEdgeToEdge()
        setContent {
            SekiraTheme {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                        .fillMaxSize()
                ) {
                    val startDestination = viewModel.startDestination
                    RootNavGraph(startDestination = startDestination)
                }
            }
        }
    }

    private fun checkNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            showNotificationListenerDialog()
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun showNotificationListenerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notification Access Required")
            .setMessage("This app requires access to your notifications. Please enable it in the settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}