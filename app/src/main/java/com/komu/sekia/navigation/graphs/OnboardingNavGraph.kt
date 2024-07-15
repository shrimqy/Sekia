package com.komu.sekia.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.komu.presentation.onboarding.OnboardingScreen
import com.komu.sekia.navigation.Graph
import com.komu.sekia.navigation.OnboardingRoute

fun NavGraphBuilder.onboardingNavGraph(rootNavHostController: NavHostController) {
    navigation(
        route = Graph.OnboardingGraph,
        startDestination = OnboardingRoute.OnboardingScreen.route
    ) {
        composable(route = OnboardingRoute.OnboardingScreen.route) {
            OnboardingScreen()
        }
    }
}