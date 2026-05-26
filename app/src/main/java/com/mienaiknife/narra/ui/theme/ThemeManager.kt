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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeManager(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val useSystemThemeKey = booleanPreferencesKey("use_system_theme")
    private val readerFontFamilyKey = stringPreferencesKey("reader_font_family")
    private val lineSpacingKey = stringPreferencesKey("line_spacing")
    private val readerFontSizeKey = floatPreferencesKey("reader_font_size")
    private val showRemainingTimeKey = booleanPreferencesKey("show_remaining_time")
    private val tapToShowControlsKey = booleanPreferencesKey("tap_to_show_controls")
    private val autoFullscreenKey = booleanPreferencesKey("auto_fullscreen")

    private val _isDarkMode = MutableStateFlow(value = true)
    private val _isDynamicColor = MutableStateFlow(value = false)
    private val _useSystemTheme = MutableStateFlow(value = true)
    private val _readerFontFamily = MutableStateFlow("Roboto")
    private val _lineSpacing = MutableStateFlow("1.0")
    private val _readerFontSize = MutableStateFlow(18.0f)
    private val _showRemainingTime = MutableStateFlow(value = true)
    private val _tapToShowControls = MutableStateFlow(value = true)
    private val _autoFullscreen = MutableStateFlow(value = true)

    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor.asStateFlow()
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()
    val readerFontFamily: StateFlow<String> = _readerFontFamily.asStateFlow()
    val lineSpacing: StateFlow<String> = _lineSpacing.asStateFlow()
    val readerFontSize: StateFlow<Float> = _readerFontSize.asStateFlow()
    val showRemainingTime: StateFlow<Boolean> = _showRemainingTime.asStateFlow()
    val tapToShowControls: StateFlow<Boolean> = _tapToShowControls.asStateFlow()
    val autoFullscreen: StateFlow<Boolean> = _autoFullscreen.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { preferences ->
                _isDarkMode.value = preferences[darkModeKey] ?: true
                _isDynamicColor.value = preferences[dynamicColorKey] ?: false
                _useSystemTheme.value = preferences[useSystemThemeKey] ?: true
                _readerFontFamily.value = preferences[readerFontFamilyKey] ?: "Roboto"
                _lineSpacing.value = preferences[lineSpacingKey] ?: "1.0"
                _readerFontSize.value = preferences[readerFontSizeKey] ?: 18.0f
                _showRemainingTime.value = preferences[showRemainingTimeKey] ?: true
                _tapToShowControls.value = preferences[tapToShowControlsKey] ?: true
                _autoFullscreen.value = preferences[autoFullscreenKey] ?: true
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[darkModeKey] = enabled }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[dynamicColorKey] = enabled }
        }
    }

    fun setUseSystemTheme(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[useSystemThemeKey] = enabled }
        }
    }

    fun setReaderFontFamily(fontFamily: String) {
        scope.launch {
            context.dataStore.edit { it[readerFontFamilyKey] = fontFamily }
        }
    }

    fun setLineSpacing(lineSpacing: String) {
        scope.launch {
            context.dataStore.edit { it[lineSpacingKey] = lineSpacing }
        }
    }

    fun setReaderFontSize(fontSize: Float) {
        scope.launch {
            context.dataStore.edit { it[readerFontSizeKey] = fontSize }
        }
    }

    fun setShowRemainingTime(showRemainingTime: Boolean) {
        scope.launch {
            context.dataStore.edit { it[showRemainingTimeKey] = showRemainingTime }
        }
    }

    fun setTapToShowControls(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[tapToShowControlsKey] = enabled }
        }
    }

    fun setAutoFullscreen(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[autoFullscreenKey] = enabled }
        }
    }
}
