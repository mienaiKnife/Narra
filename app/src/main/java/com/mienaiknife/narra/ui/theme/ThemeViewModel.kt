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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
open class ThemeViewModel
@Inject
constructor(
    private val themeManager: ThemeManager,
) : ViewModel() {
    open val uiState: StateFlow<ThemeUiState> = themeManager.uiState

    // Kept for backward compatibility if needed by Compose previews or other manual initializations
    open fun initialize(context: Context) {
        // No-op as it's now handled by Hilt injection
    }

    fun setDarkMode(enabled: Boolean) {
        themeManager.setDarkMode(enabled)
    }

    fun setDynamicColor(enabled: Boolean) {
        themeManager.setDynamicColor(enabled)
    }

    fun setUseSystemTheme(enabled: Boolean) {
        themeManager.setUseSystemTheme(enabled)
    }

    fun setReaderFontFamily(fontFamily: String) {
        themeManager.setReaderFontFamily(fontFamily)
    }

    fun setLineSpacing(lineSpacing: String) {
        themeManager.setLineSpacing(lineSpacing)
    }

    fun setReaderFontSize(fontSize: Float) {
        themeManager.setReaderFontSize(fontSize)
    }

    fun setShowRemainingTime(showRemainingTime: Boolean) {
        themeManager.setShowRemainingTime(showRemainingTime)
    }

    fun setTapToShowControls(enabled: Boolean) {
        themeManager.setTapToShowControls(enabled)
    }

    fun setAutoFullscreen(enabled: Boolean) {
        themeManager.setAutoFullscreen(enabled)
    }
}
