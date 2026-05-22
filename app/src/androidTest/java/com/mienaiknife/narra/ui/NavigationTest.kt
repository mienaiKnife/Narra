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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mienaiknife.narra.ui.screens.HomeScreenContent
import androidx.compose.material3.SnackbarHostState
import com.mienaiknife.narra.ui.viewmodels.HomeUiState
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysEmptyState_whenNoArticles() {
        composeTestRule.setContent {
            HomeScreenContent(
                uiState = HomeUiState(isLoading = false),
                snackbarHostState = SnackbarHostState(),
                onArticleClick = {},
                onAddClick = {}
            )
        }

        composeTestRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Content").assertIsDisplayed()
    }

    @Test
    fun homeScreen_callsOnAddClick_whenAddContentClicked() {
        var addClicked = false
        composeTestRule.setContent {
            HomeScreenContent(
                uiState = HomeUiState(isLoading = false),
                snackbarHostState = SnackbarHostState(),
                onArticleClick = {},
                onAddClick = { addClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Add Content").performClick()
        assert(addClicked)
    }
}
