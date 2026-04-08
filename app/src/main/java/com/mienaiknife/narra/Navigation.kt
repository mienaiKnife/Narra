package com.mienaiknife.narra

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.screens.AddScreen
import com.mienaiknife.narra.ui.screens.HomeScreen
import com.mienaiknife.narra.ui.screens.InboxScreen
import com.mienaiknife.narra.ui.screens.QueueScreen
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.theme.ThemeViewModel

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home")    { HomeScreen() }
            composable("queue")   { QueueScreen() }
            composable("add")     { AddScreen() }
            composable("inbox")   { InboxScreen() }
            composable("settings"){ SettingsScreen(themeViewModel) }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(name)
    }
}