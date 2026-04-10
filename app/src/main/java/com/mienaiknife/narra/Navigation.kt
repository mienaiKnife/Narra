package com.mienaiknife.narra

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.screens.AddScreen
import com.mienaiknife.narra.ui.screens.HistoryScreen
import com.mienaiknife.narra.ui.screens.HomeScreen
import com.mienaiknife.narra.ui.screens.InboxScreen
import com.mienaiknife.narra.ui.screens.QueueScreen
import com.mienaiknife.narra.ui.screens.ReaderScreen
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.theme.ThemeViewModel

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isReaderScreen = currentRoute?.startsWith("reader/") == true

    Scaffold(
        bottomBar = {
            if (!isReaderScreen) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (isReaderScreen) PaddingValues(0.dp) else innerPadding)
        ) {
            composable("home")    { HomeScreen(onArticleClick = { articleId -> navController.navigate("reader/$articleId") }) }
            composable("queue")   { QueueScreen(navController, onArticleClick = { articleId -> navController.navigate("reader/$articleId") }) }
            composable("history") { HistoryScreen(navController) }
            composable("add")     { AddScreen(onArticleAdded = { navController.navigate("home") }) }
            composable("inbox")   { InboxScreen() }
            composable("settings"){ SettingsScreen(themeViewModel) }
            composable(
                "reader/{articleId}",
                arguments = listOf(navArgument("articleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                ReaderScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
