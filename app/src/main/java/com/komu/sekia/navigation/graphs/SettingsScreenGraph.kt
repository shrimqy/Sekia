package com.komu.sekia.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.komu.presentation.settings.SettingsScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.SettingsRouteScreen

fun NavGraphBuilder.settingsNavGraph(rootNavController: NavHostController) {
    navigation(
        route = Graph.SettingsGraph,
        startDestination = SettingsRouteScreen.SettingsScreen.route
    ) {
        composable(route = SettingsRouteScreen.SettingsScreen.route) {
            SettingsScreen()
        }
    }
}
