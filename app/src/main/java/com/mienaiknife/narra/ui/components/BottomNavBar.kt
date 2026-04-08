package com.mienaiknife.narra.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.theme.NarraTheme

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home    : BottomNavItem("home",    Icons.Filled.Home,    "Home")
    object Queue   : BottomNavItem("queue",   Icons.AutoMirrored.Filled.PlaylistPlay, "Queue")
    object Add     : BottomNavItem("add",     Icons.Filled.Add,     "Add")
    object Inbox   : BottomNavItem("inbox",   Icons.Filled.Inbox,   "Inbox")
    object Settings: BottomNavItem("settings", Icons.Filled.Settings, "Settings")
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Queue,
        BottomNavItem.Add,
        BottomNavItem.Inbox,
        BottomNavItem.Settings
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BottomNavBarPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        BottomNavBar(rememberNavController())
    }
}
