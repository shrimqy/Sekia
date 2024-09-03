package com.komu.sekia.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.komu.presentation.MainScreen
import com.komu.presentation.sync.SyncScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.SyncRoute

@Composable
fun RootNavGraph(startDestination: String) {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
    ) {
        composable(route = SyncRoute.SyncScreen.route) {
            SyncScreen(rootNavController = rootNavController)
        }
        composable(route = Graph.MainScreenGraph) {
            MainScreen(rootNavController = rootNavController)
        }
        settingsNavGraph(rootNavController)
    }
}

