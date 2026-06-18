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
package com.mienaiknife.narra.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontFamily
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState
import org.junit.Rule
import org.junit.Test

class PlaybackScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playbackScreen_displaysArticleDetails() {
        val article = SampleArticles.all.first()

        composeTestRule.setContent {
            ReaderContent(
                uiState =
                ReaderUiState(
                    article = article,
                    isPlaying = false,
                    currentPosition = 0L,
                    duration = 100000L,
                ),
                readerFontFamily = FontFamily.Default,
                readerFontSize = 18f,
                lineSpacing = 1.5f,
                tapToShowControls = true,
                autoFullscreen = false,
                onBack = {},
                onTogglePlayPause = {},
                onSeekToWord = { _, _ -> },
                onSkipForward = {},
                onSkipBackward = {},
                onSkipNext = {},
                onCycleSpeed = {},
                onToggleFavorite = {},
                onSetSleepTimer = {},
                onSetSearchQuery = {},
            )
        }

        composeTestRule.onNodeWithText(article.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(article.source).assertIsDisplayed()
    }

    @Test
    fun playbackScreen_callsTogglePlayPause_whenClicked() {
        var toggleClicked = false

        composeTestRule.setContent {
            ReaderContent(
                uiState =
                ReaderUiState(
                    article = SampleArticles.all.first(),
                    isPlaying = false,
                ),
                readerFontFamily = FontFamily.Default,
                readerFontSize = 18f,
                lineSpacing = 1.5f,
                tapToShowControls = true,
                autoFullscreen = false,
                onBack = {},
                onTogglePlayPause = { toggleClicked = true },
                onSeekToWord = { _, _ -> },
                onSkipForward = {},
                onSkipBackward = {},
                onSkipNext = {},
                onCycleSpeed = {},
                onToggleFavorite = {},
                onSetSleepTimer = {},
                onSetSearchQuery = {},
            )
        }

        // Search for play button by content description
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assert(toggleClicked)
    }
}
