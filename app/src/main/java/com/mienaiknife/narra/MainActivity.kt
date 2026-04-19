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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
            val isDarkMode = uiState.isDarkMode
            val isDynamicColor = uiState.isDynamicColor
            val useSystemTheme = uiState.useSystemTheme

            val darkTheme = if (useSystemTheme) androidx.compose.foundation.isSystemInDarkTheme() else isDarkMode

            NarraTheme(darkTheme = darkTheme, dynamicColor = isDynamicColor) {
                AppNavigation(themeViewModel = themeViewModel)
            }
        }
    }
}