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

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ThemeViewModelTest {
    private val themeManager: ThemeManager = mock()

    @Before
    fun setUp() {
        whenever(themeManager.uiState).thenReturn(MutableStateFlow(ThemeUiState()))
    }

    @Test
    fun `setters delegate to the theme manager`() {
        val viewModel = ThemeViewModel(themeManager)

        viewModel.setDarkMode(false)
        viewModel.setDynamicColor(true)
        viewModel.setUseSystemTheme(false)
        viewModel.setReaderFontFamily("OpenDyslexic3")
        viewModel.setLineSpacing("1.4")
        viewModel.setReaderFontSize(22f)
        viewModel.setShowRemainingTime(false)
        viewModel.setTapToShowControls(false)
        viewModel.setAutoFullscreen(false)

        verify(themeManager).setDarkMode(false)
        verify(themeManager).setDynamicColor(true)
        verify(themeManager).setUseSystemTheme(false)
        verify(themeManager).setReaderFontFamily("OpenDyslexic3")
        verify(themeManager).setLineSpacing("1.4")
        verify(themeManager).setReaderFontSize(22f)
        verify(themeManager).setShowRemainingTime(false)
        verify(themeManager).setTapToShowControls(false)
        verify(themeManager).setAutoFullscreen(false)
    }
}
