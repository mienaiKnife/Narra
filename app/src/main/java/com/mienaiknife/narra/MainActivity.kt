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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mienaiknife.narra.service.SyncManager
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import com.mienaiknife.narra.ui.theme.getFontFamily
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var syncManager: SyncManager

    private val themeViewModel: ThemeViewModel by viewModels()

    private var initialArticleId: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            // Handle permission result if needed
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        initialArticleId = intent.getStringExtra("article_id")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        themeViewModel.initialize(this)
        syncManager.applyStagedDatabaseIfNecessary(this)
        syncManager.start()
        enableEdgeToEdge()

        setContent {
            val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
            val isDarkMode = uiState.isDarkMode
            val isDynamicColor = uiState.isDynamicColor
            val useSystemTheme = uiState.useSystemTheme
            val fontFamily = getFontFamily(uiState.readerFontFamily)

            val darkTheme = if (useSystemTheme) androidx.compose.foundation.isSystemInDarkTheme() else isDarkMode

            NarraTheme(darkTheme = darkTheme, dynamicColor = isDynamicColor, fontFamily = fontFamily) {
                AppNavigation(themeViewModel = themeViewModel, initialArticleId = initialArticleId)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val articleId = intent.getStringExtra("article_id")
        if (articleId != null) {
            // Trigger navigation if possible
        }
    }
}
