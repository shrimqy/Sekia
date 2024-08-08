package com.komu.presentation.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import komu.seki.presentation.screens.EmptyScreen

@Composable
fun DevicesScreen(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier
) {
    EmptyScreen(message = "To Do")
}