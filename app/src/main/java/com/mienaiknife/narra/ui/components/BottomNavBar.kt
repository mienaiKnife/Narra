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
package com.mienaiknife.narra.ui.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.NavDestination
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.theme.NarraTheme

private val BottomNavMaxContentWidth = 400.dp
private const val TABLET_MIN_SMALLEST_WIDTH_DP = 600

sealed class BottomNavItem<T : Any>(val route: T, val icon: ImageVector, @StringRes val labelRes: Int) {
    data object Home : BottomNavItem<NavDestination.Home>(NavDestination.Home, Icons.Filled.Home, R.string.nav_home)
    data object Queue : BottomNavItem<NavDestination.Queue>(NavDestination.Queue, Icons.AutoMirrored.Filled.PlaylistPlay, R.string.nav_queue)
    data object Add : BottomNavItem<NavDestination.Add>(NavDestination.Add, Icons.Filled.Add, R.string.nav_add)
    data object Inbox : BottomNavItem<NavDestination.Inbox>(NavDestination.Inbox, Icons.Filled.Inbox, R.string.nav_inbox)
    data object Settings : BottomNavItem<NavDestination.Settings>(NavDestination.Settings, Icons.Filled.Settings, R.string.nav_settings)
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Queue,
        BottomNavItem.Add,
        BottomNavItem.Inbox,
        BottomNavItem.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems: @Composable RowScope.() -> Unit = {
        items.forEach { item ->
            val isSelected = when (item) {
                BottomNavItem.Home -> currentDestination?.hasRoute<NavDestination.Home>() == true
                BottomNavItem.Add -> currentDestination?.hasRoute<NavDestination.Add>() == true
                BottomNavItem.Settings -> {
                    listOf(
                        NavDestination.Settings::class,
                        NavDestination.SettingsUi::class,
                        NavDestination.SettingsPlayback::class,
                        NavDestination.SettingsVoices::class,
                        NavDestination.SettingsDownloads::class,
                        NavDestination.SettingsAbout::class,
                        NavDestination.SettingsLicenses::class,
                    ).any { currentDestination?.hasRoute(it) == true }
                }

                BottomNavItem.Queue -> {
                    listOf(
                        NavDestination.Queue::class,
                        NavDestination.History::class,
                    ).any { currentDestination?.hasRoute(it) == true }
                }

                BottomNavItem.Inbox -> {
                    listOf(
                        NavDestination.Inbox::class,
                        NavDestination.Feeds::class,
                        NavDestination.Feed::class,
                    ).any { currentDestination?.hasRoute(it) == true }
                }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(item.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                label = { Text(stringResource(item.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }

    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP
    if (isTablet) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavigationBarDefaults.containerColor)
                .windowInsetsPadding(NavigationBarDefaults.windowInsets),
            contentAlignment = Alignment.Center,
        ) {
            NavigationBar(
                modifier = Modifier.widthIn(max = BottomNavMaxContentWidth),
                windowInsets = WindowInsets(0, 0, 0, 0),
                content = navItems,
            )
        }
    } else {
        NavigationBar(content = navItems)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BottomNavBarPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        BottomNavBar(rememberNavController())
    }
}
