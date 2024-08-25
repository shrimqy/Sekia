package com.komu.presentation.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komu.presentation.home.components.DeviceCard
import com.komu.presentation.home.components.MediaPlaybackCard
import komu.seki.presentation.components.PullRefresh

@Composable
fun HomeScreen(
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val deviceDetails by viewModel.deviceDetails.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current

    val playbackData by viewModel.playbackData.collectAsState()

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        item(
            key = "devices",
            contentType = { 0 }
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
                DeviceCard(
                    deviceDetails = deviceDetails,
                    onSyncAction = { deviceDetails?.hostAddress?.let { viewModel.toggleSync(context, syncStatus, hostAddress = it) } },
                    syncStatus = syncStatus
                )
            }
        }
        item(key = "media_playback") {
            MediaPlaybackCard(
                playbackData = playbackData,
                onPlayPauseClick = { /* Handle Play/Pause */ },
                onSkipNextClick = { /* Handle Skip Next */ },
                onSkipPreviousClick = { /* Handle Skip Previous */ }
            )
        }
    }

}

