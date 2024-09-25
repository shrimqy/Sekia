package com.komu.presentation.about

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import com.komu.presentation.settings.components.LogoHeader
import com.komu.presentation.settings.components.TextPreferenceWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(rootNavController: NavController, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                title = { Text(text = "About") },
                navigationIcon = {
                    IconButton(
                        onClick = { rootNavController.navigateUp() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        }
    ) { contentPadding ->
        LazyColumn(contentPadding = contentPadding) {
            item {
                LogoHeader()
            }

            item {
                TextPreferenceWidget(
                    title = "Version",
                    subtitle = "0.3.0, Sep 25, 2024",
                    onPreferenceClick = { uriHandler.openUri("https://github.com/shrimqy/Sekia") },
                )
            }

            item {
                TextPreferenceWidget(
                    title = "What's new",
                    onPreferenceClick = { uriHandler.openUri("https://github.com/shrimqy/Sekia/releases") },
                )
            }

            item {
                TextPreferenceWidget(
                    title = "Github",
                    onPreferenceClick = { uriHandler.openUri("https://github.com/shrimqy/Sekia") },
                )
            }
        }
    }
}