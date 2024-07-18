package com.komu.presentation.onboarding

import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.komu.sekia.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val services by viewModel.services.collectAsState()
    val showDialog = remember { mutableStateOf(false) }
    val selectedService = remember { mutableStateOf<NsdServiceInfo?>(null) }
    Log.d("NsdService","$services")
    Column(modifier = modifier) {
        LazyColumn {
            items(services) { service->
                Log.d("Service", "Connect button clicked, service: $services")
                TextButton(onClick = {
                    Log.d("Service", "Connect button clicked, service: $services")
                    selectedService.value = service
                    showDialog.value = true
                }) {
                    Text(text = "Connect to ${service.serviceName}")
                }
            }
        }
        LaunchedEffect(services) {
            if (services.isNotEmpty()) {
                Log.d("NsdService", "Services discovered: ${services.map { it.serviceName }}")
            }
        }
    }

    if (showDialog.value && selectedService.value != null) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Connect to Service") },
            text = { Text("Do you want to connect to ${selectedService.value?.serviceName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("Service", "Connecting to service: ${selectedService.value}")
                        viewModel.connectToService(selectedService.value!!)
                        showDialog.value = false
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}