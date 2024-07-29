package com.komu.presentation.home

import android.app.ActivityManager.RunningServiceInfo
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.coroutineContext

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,

    ) {
    val viewModel: HomeViewModel = hiltViewModel()
    val deviceDetails by viewModel.deviceDetails.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            text = if (isConnected) "Connected to the device" else "Not connected",
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = { viewModel.disconnect() }) {
            Text(text = "Disconnect")
        }
        Spacer(modifier = Modifier.height(8.dp))
        deviceDetails?.let {
            Text(text = "Device Name: ${it.deviceName}")
            Text(text = "Host Address: ${it.hostAddress}")
            Text(text = "Port: ${it.port}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Messages:")
    }
}