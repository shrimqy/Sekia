package com.komu.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.komu.sekia.navigation.graphs.MainNavGraph

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    homeNavController: NavHostController = rememberNavController()
) {
    Scaffold (modifier = Modifier.fillMaxSize(),
        topBar = {
            // TODO
        },
        bottomBar = {
            // TODO
        }
    ) { innerPadding ->
        MainNavGraph(rootNavController = rootNavController, homeNavController = homeNavController, innerPadding = innerPadding)
    }
}