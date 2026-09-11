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
package com.mienaiknife.narra.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mienaiknife.narra.data.settings.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeUiState(
    val isDarkMode: Boolean = true,
    val isDynamicColor: Boolean = false,
    val useSystemTheme: Boolean = true,
    val readerFontFamily: String = "Roboto",
    val lineSpacing: String = "1.0",
    val readerFontSize: Float = 18.0f,
    val showRemainingTime: Boolean = true,
    val tapToShowControls: Boolean = true,
    val autoFullscreen: Boolean = true,
)

class ThemeManager(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val useSystemThemeKey = booleanPreferencesKey("use_system_theme")
    private val readerFontFamilyKey = stringPreferencesKey("reader_font_family")
    private val lineSpacingKey = stringPreferencesKey("line_spacing")
    private val readerFontSizeKey = floatPreferencesKey("reader_font_size")
    private val showRemainingTimeKey = booleanPreferencesKey("show_remaining_time")
    private val tapToShowControlsKey = booleanPreferencesKey("tap_to_show_controls")
    private val autoFullscreenKey = booleanPreferencesKey("auto_fullscreen")

    val uiState: StateFlow<ThemeUiState> =
        context.settingsDataStore.data
            .map { preferences ->
                ThemeUiState(
                    isDarkMode = preferences[darkModeKey] ?: true,
                    isDynamicColor = preferences[dynamicColorKey] ?: false,
                    useSystemTheme = preferences[useSystemThemeKey] ?: true,
                    readerFontFamily = preferences[readerFontFamilyKey] ?: "Roboto",
                    lineSpacing = preferences[lineSpacingKey] ?: "1.0",
                    readerFontSize = preferences[readerFontSizeKey] ?: 18.0f,
                    showRemainingTime = preferences[showRemainingTimeKey] ?: true,
                    tapToShowControls = preferences[tapToShowControlsKey] ?: true,
                    autoFullscreen = preferences[autoFullscreenKey] ?: true,
                )
            }.stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeUiState(),
            )

    fun setDarkMode(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[darkModeKey] = enabled }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[dynamicColorKey] = enabled }
        }
    }

    fun setUseSystemTheme(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[useSystemThemeKey] = enabled }
        }
    }

    fun setReaderFontFamily(fontFamily: String) {
        scope.launch {
            context.settingsDataStore.edit { it[readerFontFamilyKey] = fontFamily }
        }
    }

    fun setLineSpacing(lineSpacing: String) {
        scope.launch {
            context.settingsDataStore.edit { it[lineSpacingKey] = lineSpacing }
        }
    }

    fun setReaderFontSize(fontSize: Float) {
        scope.launch {
            context.settingsDataStore.edit { it[readerFontSizeKey] = fontSize }
        }
    }

    fun setShowRemainingTime(showRemainingTime: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[showRemainingTimeKey] = showRemainingTime }
        }
    }

    fun setTapToShowControls(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[tapToShowControlsKey] = enabled }
        }
    }

    fun setAutoFullscreen(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[autoFullscreenKey] = enabled }
        }
    }
}
