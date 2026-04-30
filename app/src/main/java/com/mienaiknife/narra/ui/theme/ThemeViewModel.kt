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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThemeUiState(
    val isDarkMode: Boolean = true,
    val isDynamicColor: Boolean = false,
    val useSystemTheme: Boolean = true,
    val readerFontFamily: String = "Roboto",
    val readerFontSize: Float = 18.0f,
    val showRemainingTime: Boolean = true
)

open class ThemeViewModel : ViewModel() {
    private var themeManager: ThemeManager? = null
    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    open fun initialize(context: Context) {
        if (themeManager == null) {
            themeManager = ThemeManager(context, viewModelScope)
            viewModelScope.launch {
                themeManager?.isDarkMode?.collect { isDark ->
                    _uiState.update { it.copy(isDarkMode = isDark) }
                }
            }
            viewModelScope.launch {
                themeManager?.isDynamicColor?.collect { isDynamic ->
                    _uiState.update { it.copy(isDynamicColor = isDynamic) }
                }
            }
            viewModelScope.launch {
                themeManager?.useSystemTheme?.collect { useSystem ->
                    _uiState.update { it.copy(useSystemTheme = useSystem) }
                }
            }
            viewModelScope.launch {
                themeManager?.readerFontFamily?.collect { fontFamily ->
                    _uiState.update { it.copy(readerFontFamily = fontFamily) }
                }
            }
            viewModelScope.launch {
                themeManager?.readerFontSize?.collect { fontSize ->
                    _uiState.update { it.copy(readerFontSize = fontSize) }
                }
            }
            viewModelScope.launch {
                themeManager?.showRemainingTime?.collect { showRemaining ->
                    _uiState.update { it.copy(showRemainingTime = showRemaining) }
                }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        themeManager?.setDarkMode(enabled)
    }

    fun setDynamicColor(enabled: Boolean) {
        themeManager?.setDynamicColor(enabled)
    }

    fun setUseSystemTheme(enabled: Boolean) {
        themeManager?.setUseSystemTheme(enabled)
    }

    fun setReaderFontFamily(fontFamily: String) {
        themeManager?.setReaderFontFamily(fontFamily)
    }

    fun setReaderFontSize(fontSize: Float) {
        themeManager?.setReaderFontSize(fontSize)
    }

    fun setShowRemainingTime(enabled: Boolean) {
        themeManager?.setShowRemainingTime(enabled)
    }
}
