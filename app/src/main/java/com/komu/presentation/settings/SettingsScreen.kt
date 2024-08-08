package com.komu.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import komu.seki.presentation.screens.EmptyScreen

@Composable
fun SettingsScreen(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier
) {
    EmptyScreen(message = "To Do")
}