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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
import com.mienaiknife.narra.ui.screens.PronunciationsSettingsScreen
import com.mienaiknife.narra.ui.screens.QueueScreen
import com.mienaiknife.narra.ui.screens.ReaderScreen
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.screens.UserInterfaceSettingsScreen
import com.mienaiknife.narra.ui.screens.VoicesSettingsScreen
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay

/** Duration of the reader's dismiss slide and the bottom bar's handoff animation. */
private const val ReaderTransitionMillis = 300

/** How long before the reader's slide ends the mini-player and navigation bar start returning. */
private const val ReaderBarHeadStartMillis = 100

@Composable
fun AppNavigation(
    themeViewModel: ThemeViewModel,
    initialArticleId: String? = null,
    onArticleIdConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(initialArticleId) {
        if (initialArticleId != null) {
            navController.navigate(NavDestination.Reader(initialArticleId)) {
                launchSingleTop = true
            }
            onArticleIdConsumed()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val isReaderDestination = navBackStackEntry?.destination?.hasRoute<NavDestination.Reader>() == true

    // Keep the reader full-bleed until its dismiss slide has finished. If the bottom bar and the
    // NavHost padding came back at the same frame the pop commits, the reader would be clipped from
    // the bottom while it slides, making its bottom edge trail the top instead of moving together.
    var isReaderLayoutHeld by remember { mutableStateOf(isReaderDestination) }
    var isBottomBarVisible by remember { mutableStateOf(!isReaderDestination) }
    LaunchedEffect(isReaderDestination) {
        // Compose scales animations by the system animator duration scale, so scale the handoff
        // delays to match; with animations turned off the bar comes back immediately.
        val motionScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        val slideMillis = (ReaderTransitionMillis * motionScale).toLong()
        val headStartMillis = (ReaderBarHeadStartMillis * motionScale).toLong()
        if (isReaderDestination) {
            isBottomBarVisible = false
            isReaderLayoutHeld = true
        } else if (isReaderLayoutHeld) {
            // Let the bar start rising while the reader's last sliver is still on screen; the
            // layout stays full-bleed underneath, so the reader is never clipped mid-slide.
            delay((slideMillis - headStartMillis).coerceAtLeast(0L))
            isBottomBarVisible = true
            delay(headStartMillis)
            isReaderLayoutHeld = false
        }
    }
    val isReaderChromeVisible = isReaderDestination || isReaderLayoutHeld

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible && !isReaderDestination,
                enter = fadeIn(tween(ReaderTransitionMillis)) +
                    slideInVertically(animationSpec = tween(ReaderTransitionMillis)) { it },
                // The reader covers the bar as it opens, so hiding it instantly is invisible and
                // avoids keeping the bar's space reserved while the reader slides in on top.
                exit = ExitTransition.None,
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
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPaddingTarget = innerPadding.calculateBottomPadding()
        val bottomPadding = remember {
            Animatable(
                initialValue = if (isReaderChromeVisible) 0.dp else bottomPaddingTarget,
                typeConverter = Dp.VectorConverter,
            )
        }
        // The reader keeps a full-height layout while it slides away; the bar's space comes back
        // only after the slide, so neither edge of the reader is clipped mid-flight.
        LaunchedEffect(isReaderChromeVisible, bottomPaddingTarget) {
            if (isReaderChromeVisible) {
                bottomPadding.snapTo(0.dp)
            } else {
                bottomPadding.animateTo(
                    targetValue = bottomPaddingTarget,
                    animationSpec = tween(ReaderTransitionMillis),
                )
            }
        }
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            predictivePopEnterTransition = { EnterTransition.None },
            predictivePopExitTransition = {
                if (initialState.destination.hasRoute<NavDestination.Reader>()) {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ReaderTransitionMillis, easing = FastOutSlowInEasing),
                    )
                } else {
                    ExitTransition.None
                }
            },
            modifier = Modifier.padding(
                start = if (isReaderChromeVisible) 0.dp else innerPadding.calculateStartPadding(layoutDirection),
                end = if (isReaderChromeVisible) 0.dp else innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomPadding.value,
            ),
        ) {
            composable<NavDestination.Home> {
                HomeScreen(
                    snackbarHostState = snackbarHostState,
                    onArticleClick = { articleId -> navController.navigate(NavDestination.Reader(articleId)) },
                    onAddClick = {
                        navController.navigate(NavDestination.Add) {
                            popUpTo(NavDestination.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<NavDestination.Queue> {
                QueueScreen(
                    onNavigateToHistory = { navController.navigate(NavDestination.History) },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable<NavDestination.History> {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable<NavDestination.Add> {
                AddScreen(
                    snackbarHostState = snackbarHostState,
                    onArticleAdded = { },
                )
            }
            composable<NavDestination.Inbox> {
                InboxScreen(
                    onNavigateToFeeds = { navController.navigate(NavDestination.Feeds) },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable<NavDestination.Feeds> {
                FeedsScreen(
                    onNavigateToFeed = { feedUrl, feedTitle -> navController.navigate(NavDestination.Feed(feedUrl, feedTitle)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NavDestination.Feed> {
                FeedArticlesScreen(
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable<NavDestination.Settings> {
                SettingsScreen(
                    onNavigateToUserInterface = { navController.navigate(NavDestination.SettingsUi()) },
                    onNavigateToPlayback = { navController.navigate(NavDestination.SettingsPlayback()) },
                    onNavigateToVoices = { navController.navigate(NavDestination.SettingsVoices()) },
                    onNavigateToDownloads = { navController.navigate(NavDestination.SettingsDownloads()) },
                    onNavigateToAbout = { navController.navigate(NavDestination.SettingsAbout) },
                    navigateToDestination = { destination ->
                        navController.navigate(destination)
                    },
                )
            }
            composable<NavDestination.SettingsUi> { backStackEntry ->
                val route = backStackEntry.toRoute<NavDestination.SettingsUi>()
                UserInterfaceSettingsScreen(
                    themeViewModel = themeViewModel,
                    highlightSetting = route.highlightSetting,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NavDestination.SettingsPlayback> { backStackEntry ->
                val route = backStackEntry.toRoute<NavDestination.SettingsPlayback>()
                PlaybackSettingsScreen(
                    highlightSetting = route.highlightSetting,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NavDestination.SettingsVoices> { backStackEntry ->
                val route = backStackEntry.toRoute<NavDestination.SettingsVoices>()
                VoicesSettingsScreen(
                    highlightSetting = route.highlightSetting,
                    onBack = { navController.popBackStack() },
                    onNavigateToPronunciations = {
                        navController.navigate(NavDestination.SettingsPronunciations)
                    },
                )
            }
            composable<NavDestination.SettingsPronunciations> {
                PronunciationsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.SettingsDownloads> { backStackEntry ->
                val route = backStackEntry.toRoute<NavDestination.SettingsDownloads>()
                DownloadsSettingsScreen(
                    highlightSetting = route.highlightSetting,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NavDestination.SettingsAbout> {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLicenses = { navController.navigate(NavDestination.SettingsLicenses) },
                )
            }
            composable<NavDestination.SettingsLicenses> {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
            composable<NavDestination.Reader>(
                // Mirror the dismiss slide so opening and closing the reader share one motion: the
                // page rises from the bottom edge and settles, then falls back down on pop. The
                // reader paints the same background as the tab underneath, so only its content (top
                // bar, text, controls) appears to move. Swapping one article for another (a deep
                // link while the reader is open) stays instant instead of sliding over itself.
                enterTransition = {
                    if (initialState.destination.hasRoute<NavDestination.Reader>()) {
                        EnterTransition.None
                    } else {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(ReaderTransitionMillis, easing = FastOutSlowInEasing),
                        )
                    }
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ReaderTransitionMillis, easing = FastOutSlowInEasing),
                    )
                },
            ) {
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                    themeViewModel = themeViewModel,
                )
            }
        }
    }
}
