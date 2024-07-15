package com.komu.sekia.navigation

object Graph {
    const val RootGraph = "rootGraph"
    const val OnboardingGraph = "onboardingGraph"
    const val MainScreenGraph = "mainScreenGraph"
    const val SettingsGraph = "settingsGraph"
}

sealed class MainRouteScreen(var route: String) {
    data object HomeScreen: MainRouteScreen("home")
}

sealed class OnboardingRoute(var route: String) {
    data object OnboardingScreen: OnboardingRoute("onboarding")
}

sealed class SettingsRouteScreen(var route: String) {
    data object SettingsScreen: SettingsRouteScreen("settings")
}