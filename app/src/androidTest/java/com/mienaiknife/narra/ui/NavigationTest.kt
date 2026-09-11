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
package com.mienaiknife.narra.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.screens.HomeScreenContent
import com.mienaiknife.narra.ui.viewmodels.HomeUiState
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val emptyTitle = context.getString(R.string.home_empty_title)
    private val addContent = context.getString(R.string.home_add_content)

    @Test
    fun homeScreen_displaysEmptyState_whenNoArticles() {
        composeTestRule.setContent {
            HomeScreenContent(
                uiState =
                HomeUiState.Success(
                    continueListening = emptyList(),
                    newFromFeeds = emptyList(),
                    favoriteArticles = emptyList(),
                ),
                snackbarHostState = SnackbarHostState(),
                onArticleClick = {},
                onAddClick = {},
            )
        }

        composeTestRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(addContent).assertIsDisplayed()
    }

    @Test
    fun homeScreen_callsOnAddClick_whenAddContentClicked() {
        var addClicked = false
        composeTestRule.setContent {
            HomeScreenContent(
                uiState =
                HomeUiState.Success(
                    continueListening = emptyList(),
                    newFromFeeds = emptyList(),
                    favoriteArticles = emptyList(),
                ),
                snackbarHostState = SnackbarHostState(),
                onArticleClick = {},
                onAddClick = { addClicked = true },
            )
        }

        composeTestRule.onNodeWithText(addContent).performClick()
        assert(addClicked)
    }
}
