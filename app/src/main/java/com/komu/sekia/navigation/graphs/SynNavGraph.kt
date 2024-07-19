package com.komu.sekia.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.komu.presentation.sync.SyncScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.SyncRoute

fun NavGraphBuilder.onboardingNavGraph(rootNavHostController: NavHostController) {
    navigation(
        route = Graph.SyncGraph,
        startDestination = SyncRoute.SyncScreen.route
    ) {
        composable(route = SyncRoute.SyncScreen.route) {
            SyncScreen()
        }
    }
}