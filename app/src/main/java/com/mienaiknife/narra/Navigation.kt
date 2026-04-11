package com.mienaiknife.narra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.MiniPlayer
import com.mienaiknife.narra.ui.screens.AddScreen
import com.mienaiknife.narra.ui.screens.FeedsScreen
import com.mienaiknife.narra.ui.screens.HistoryScreen
import com.mienaiknife.narra.ui.screens.HomeScreen
import com.mienaiknife.narra.ui.screens.InboxScreen
import com.mienaiknife.narra.ui.screens.QueueScreen
import com.mienaiknife.narra.ui.screens.ReaderScreen
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.screens.UserInterfaceSettingsScreen
import com.mienaiknife.narra.ui.screens.PlaceholderSettingsScreen
import com.mienaiknife.narra.ui.theme.ThemeViewModel

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isReaderScreen = currentRoute?.startsWith("reader/") == true

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !isReaderScreen,
                enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
                exit = fadeOut(tween(400)) + slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
            ) {
                Column {
                    MiniPlayer(onExpand = { articleId ->
                        navController.navigate("reader/$articleId") {
                            launchSingleTop = true
                        }
                    })
                    BottomNavBar(navController)
                }
            }
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPadding by animateDpAsState(
            targetValue = if (isReaderScreen) 0.dp else innerPadding.calculateBottomPadding(),
            animationSpec = tween(400),
            label = "bottomPadding"
        )
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(
                start = if (isReaderScreen) 0.dp else innerPadding.calculateStartPadding(layoutDirection),
                end = if (isReaderScreen) 0.dp else innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomPadding
            )
        ) {
            composable("home")    { HomeScreen(onArticleClick = { articleId -> navController.navigate("reader/$articleId") }) }
            composable("queue")   { QueueScreen(navController, onArticleClick = { articleId -> navController.navigate("reader/$articleId") }) }
            composable("history") { HistoryScreen(navController) }
            composable("add")     { AddScreen(onArticleAdded = { navController.navigate("home") }) }
            composable("inbox")   {
                InboxScreen(
                    navController = navController,
                    onNavigateToFeeds = { navController.navigate("feeds") }
                )
            }
            composable("feeds")   { FeedsScreen(navController) }
            composable("settings"){
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onNavigateToUserInterface = { navController.navigate("settings/ui") },
                    onNavigateToPlayback = { navController.navigate("settings/playback") },
                    onNavigateToVoices = { navController.navigate("settings/voices") },
                    onNavigateToDownloads = { navController.navigate("settings/downloads") },
                    onNavigateToAbout = { navController.navigate("settings/about") }
                )
            }
            composable("settings/ui") {
                UserInterfaceSettingsScreen(themeViewModel = themeViewModel, onBack = { navController.popBackStack() })
            }
            composable("settings/playback") {
                PlaceholderSettingsScreen("Playback", onBack = { navController.popBackStack() })
            }
            composable("settings/voices") {
                PlaceholderSettingsScreen("Voices", onBack = { navController.popBackStack() })
            }
            composable("settings/downloads") {
                PlaceholderSettingsScreen("Downloads", onBack = { navController.popBackStack() })
            }
            composable("settings/about") {
                PlaceholderSettingsScreen("About Narra", onBack = { navController.popBackStack() })
            }
            composable(
                "reader/{articleId}",
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(400))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(400))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(400)
                    ) + fadeOut(animationSpec = tween(400))
                }
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
