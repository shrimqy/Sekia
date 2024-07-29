package com.komu.sekia.navigation.graphs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.komu.presentation.sync.SyncScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.SyncRoute

@Composable
fun SyncNavGraph(
    rootNavController: NavHostController,
    homeNavController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = homeNavController,
        route = Graph.SyncGraph,
        startDestination = SyncRoute.SyncScreen.route,
        modifier = Modifier.padding(innerPadding),
    ) {
        composable(route = SyncRoute.SyncScreen.route) {
            SyncScreen(rootNavController = rootNavController)
        }
    }
}