package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.adhantimes.AdhanTimesScreen
import com.example.ui.screens.adhkar.AdhkarScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { SabeelBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.AdhanTimes,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Route.AdhanTimes> {
                AdhanTimesScreen()
            }
            composable<Route.Adhkar> {
                AdhkarScreen()
            }
            composable<Route.Settings> {
                SettingsScreen()
            }
        }
    }
}
