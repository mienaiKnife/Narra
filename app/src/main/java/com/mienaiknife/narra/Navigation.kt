/*
 * Copyright 2025 Narra Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.MiniPlayer
import com.mienaiknife.narra.ui.screens.AboutScreen
import com.mienaiknife.narra.ui.screens.AddScreen
import com.mienaiknife.narra.ui.screens.DownloadsSettingsScreen
import com.mienaiknife.narra.ui.screens.FeedArticlesScreen
import com.mienaiknife.narra.ui.screens.FeedsScreen
import com.mienaiknife.narra.ui.screens.HistoryScreen
import com.mienaiknife.narra.ui.screens.HomeScreen
import com.mienaiknife.narra.ui.screens.InboxScreen
import com.mienaiknife.narra.ui.screens.LicensesScreen
import com.mienaiknife.narra.ui.screens.PlaybackSettingsScreen
import com.mienaiknife.narra.ui.screens.QueueScreen
import com.mienaiknife.narra.ui.screens.ReaderScreen
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.screens.UserInterfaceSettingsScreen
import com.mienaiknife.narra.ui.screens.VoicesSettingsScreen
import com.mienaiknife.narra.ui.theme.ThemeViewModel

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel, initialArticleId: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(initialArticleId) {
        if (initialArticleId != null) {
            navController.navigate(NavDestination.Reader(initialArticleId)) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    val isReaderScreen = navBackStackEntry?.destination?.hasRoute<NavDestination.Reader>() == true

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !isReaderScreen,
                enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { it }),
                exit = fadeOut(tween(400)) + slideOutVertically(animationSpec = tween(400), targetOffsetY = { it })
            ) {
                Column {
                    MiniPlayer(onExpand = { articleId ->
                        navController.navigate(NavDestination.Reader(articleId)) {
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
            startDestination = NavDestination.Home,
            modifier = Modifier.padding(
                start = if (isReaderScreen) 0.dp else innerPadding.calculateStartPadding(layoutDirection),
                end = if (isReaderScreen) 0.dp else innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomPadding
            )
        ) {
            composable<NavDestination.Home> { HomeScreen(onArticleClick = { articleId -> navController.navigate(NavDestination.Reader(articleId)) }) }
            composable<NavDestination.Queue> { 
                QueueScreen(onNavigateToHistory = { navController.navigate(NavDestination.History) }) 
            }
            composable<NavDestination.History> { 
                HistoryScreen(onBack = { navController.popBackStack() }) 
            }
            composable<NavDestination.Add> { AddScreen(onArticleAdded = { navController.navigate(NavDestination.Home) }) }
            composable<NavDestination.Inbox> {
                InboxScreen(
                    onNavigateToFeeds = { navController.navigate(NavDestination.Feeds) }
                )
            }
            composable<NavDestination.Feeds> { 
                FeedsScreen(
                    onNavigateToFeed = { feedTitle -> navController.navigate(NavDestination.Feed(feedTitle)) },
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable<NavDestination.Feed> {
                FeedArticlesScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.Settings> {
                SettingsScreen(
                    onNavigateToUserInterface = { navController.navigate(NavDestination.SettingsUi) },
                    onNavigateToPlayback = { navController.navigate(NavDestination.SettingsPlayback) },
                    onNavigateToVoices = { navController.navigate(NavDestination.SettingsVoices) },
                    onNavigateToDownloads = { navController.navigate(NavDestination.SettingsDownloads) },
                    onNavigateToAbout = { navController.navigate(NavDestination.SettingsAbout) }
                )
            }
            composable<NavDestination.SettingsUi> {
                UserInterfaceSettingsScreen(themeViewModel = themeViewModel, onBack = { navController.popBackStack() })
            }
            composable<NavDestination.SettingsPlayback> {
                PlaybackSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.SettingsVoices> {
                VoicesSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.SettingsDownloads> {
                DownloadsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.SettingsAbout> {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLicenses = { navController.navigate(NavDestination.SettingsLicenses) }
                )
            }
            composable<NavDestination.SettingsLicenses> {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.Reader>(
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
            ) {
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
