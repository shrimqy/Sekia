package com.komu.sekia.navigation.graphs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.komu.presentation.home.HomeScreen
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
    ) {
        composable(route = MainRouteScreen.HomeScreen.route) {
            HomeScreen()
        }
    }
}