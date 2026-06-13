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

package com.mienaiknife.narra.ui.screenshots

import androidx.compose.material3.SnackbarHostState
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.mienaiknife.narra.ui.screens.HomeScreenContent
import com.mienaiknife.narra.ui.viewmodels.HomeUiState
import com.mienaiknife.narra.ui.theme.NarraTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5
    )

    @Test
    fun homeScreen_Success() {
        paparazzi.snapshot {
            NarraTheme {
                HomeScreenContent(
                    uiState = HomeUiState.Success(
                        continueListening = emptyList(),
                        newFromFeeds = emptyList(),
                        favoriteArticles = emptyList()
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onArticleClick = {},
                    onAddClick = {},
                    onRefresh = {}
                )
            }
        }
    }
}
