package com.mienaiknife.narra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        themeViewModel.initialize(this)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val isDynamicColor by themeViewModel.isDynamicColor.collectAsState()
            val useSystemTheme by themeViewModel.useSystemTheme.collectAsState()

            val darkTheme = if (useSystemTheme) androidx.compose.foundation.isSystemInDarkTheme() else isDarkMode

            NarraTheme(darkTheme = darkTheme, dynamicColor = isDynamicColor) {
                AppNavigation(themeViewModel = themeViewModel)
            }
        }
    }
}