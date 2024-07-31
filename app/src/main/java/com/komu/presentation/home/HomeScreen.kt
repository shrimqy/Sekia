package com.komu.presentation.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komu.presentation.home.components.DeviceCard
import com.komu.sekia.services.Actions
import com.komu.sekia.services.WebSocketService
import komu.seki.data.repository.dataStore

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    ) {
    val viewModel: HomeViewModel = hiltViewModel()
    val deviceDetails by viewModel.deviceDetails.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        DeviceCard(
            deviceDetails = deviceDetails,
            onSyncAction = { deviceDetails?.hostAddress?.let { toggleSync(context, syncStatus, hostAddress = it) } },
            syncStatus = syncStatus
        )
    }
}

fun toggleSync(context: Context, syncStatus: Boolean, hostAddress: String) {
    val intent = Intent(context, WebSocketService::class.java).apply {
        action = if (syncStatus) Actions.STOP.name else Actions.START.name

        putExtra(WebSocketService.EXTRA_HOST_ADDRESS, hostAddress) // Replace with actual host address
    }
    context.startService(intent)
}