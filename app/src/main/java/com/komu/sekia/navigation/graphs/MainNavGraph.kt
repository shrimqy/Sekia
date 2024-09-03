package com.komu.sekia.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.komu.presentation.devices.DevicesScreen
import com.komu.presentation.home.HomeScreen
import com.komu.presentation.settings.SettingsScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.MainRouteScreen

@Composable
fun MainNavGraph(
    rootNavController: NavHostController,
    homeNavController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = homeNavController,
        route = Graph.MainScreenGraph,
        startDestination = MainRouteScreen.HomeScreen.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(route = MainRouteScreen.HomeScreen.route) {
            HomeScreen()
        }
        composable(route = MainRouteScreen.DevicesScreen.route) {
            DevicesScreen(rootNavController)
        }
        composable(route = MainRouteScreen.SettingsScreen.route) {
            SettingsScreen(rootNavController)
        }
    }
}