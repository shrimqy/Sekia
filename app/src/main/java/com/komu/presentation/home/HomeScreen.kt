package com.komu.presentation.home

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.komu.presentation.home.components.DeviceCard
import com.komu.presentation.home.components.MediaPlaybackCard
import com.komu.presentation.home.components.VolumeSlider
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.SyncRoute
import dagger.hilt.android.qualifiers.ApplicationContext

@Composable
fun HomeScreen(
    navController: NavController
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
                    device = deviceDetails,
                    onSyncAction = { viewModel.toggleSync(syncStatus) },
                    syncStatus = syncStatus,
                    navController = navController
                )
            }
        }
        item(key = "media_playback") {
            MediaPlaybackCard(
                playbackData = playbackData,
                onPlayPauseClick = { viewModel.onPlayPause() },
                onSkipNextClick = { viewModel.onNext() },
                onSkipPreviousClick = { viewModel.onPrevious() }
            )
        }

        item(key = "volume_slider") {
            playbackData?.volume?.let { volume -> VolumeSlider(volume = volume, onVolumeChange = { viewModel.onVolumeChange(it) }) }
        }
    }
}

