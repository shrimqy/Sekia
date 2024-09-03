package com.komu.presentation.sync

import android.Manifest
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.komu.presentation.sync.components.DeviceItem
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.MainRouteScreen
import komu.seki.presentation.components.PullRefresh
import komu.seki.presentation.screens.EmptyScreen
import komu.seki.presentation.screens.LoadingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    rootNavController: NavHostController,
) {
    val scope = rememberCoroutineScope()

    val viewModel: SyncViewModel = hiltViewModel()
    val services by viewModel.services.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showDialog = remember { mutableStateOf(false) }
    val selectedService = remember { mutableStateOf<NsdServiceInfo?>(null) }
    val notificationPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    val context = LocalContext.current

    PullRefresh(
        refreshing = isRefreshing,
        enabled = true,
        onRefresh = { viewModel.findServices() }
    ) {
        Scaffold(
            topBar ={
                TopAppBar(title = { Text(text = "Open the app on your pc to connect") })
            }
        ) { contentPadding->
            when {
                isRefreshing -> LoadingScreen()
                services.isEmpty() -> EmptyScreen(message = "No Devices found")
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        contentPadding = contentPadding
                    ) {
                        items(services) { service ->
                            DeviceItem(
                                service = service,
                                onClick = {
                                    selectedService.value = service
                                    showDialog.value = true
                                })
                        }
                    }
                }
            }
        }
    }


    if (showDialog.value && selectedService.value != null) {

        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Connect") },
            text = { Text("Do you want to connect to ${selectedService.value?.serviceName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("Service", "Connecting to service: ${selectedService.value}")
                        viewModel.saveDevice(context, selectedService.value!!)
                        rootNavController.navigate(route = Graph.MainScreenGraph)
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