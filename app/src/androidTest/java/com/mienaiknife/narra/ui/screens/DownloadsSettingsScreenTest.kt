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

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.DownloadsSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DownloadsSettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val autoImportLabel = context.getString(R.string.settings_downloads_auto_import)
    private val syncLocationLabel = context.getString(R.string.settings_downloads_sync_location)

    @Test
    fun autoImportRow_isDisplayed_whenAutoExportDisabled() {
        composeTestRule.setContent {
            NarraTheme {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(),
                    onDownloadOverWifiOnlyChange = {},
                    onRefreshIntervalChange = {},
                    onInboxInitialLimitChange = {},
                    onImportOpml = {},
                    onExportOpml = {},
                    onBackupDatabase = {},
                    onRestoreDatabase = {},
                    onAutoExportEnabledChange = {},
                    onAutoImportEnabledChange = {},
                    onSetSyncLocation = {},
                    onDeleteDatabase = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText(autoImportLabel).assertIsDisplayed()
    }

    @Test
    fun syncLocationRow_isDisplayed_whenOnlyAutoImportEnabled() {
        composeTestRule.setContent {
            NarraTheme {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(autoImportEnabled = true),
                    onDownloadOverWifiOnlyChange = {},
                    onRefreshIntervalChange = {},
                    onInboxInitialLimitChange = {},
                    onImportOpml = {},
                    onExportOpml = {},
                    onBackupDatabase = {},
                    onRestoreDatabase = {},
                    onAutoExportEnabledChange = {},
                    onAutoImportEnabledChange = {},
                    onSetSyncLocation = {},
                    onDeleteDatabase = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText(syncLocationLabel).assertIsDisplayed()
    }

    @Test
    fun autoImportRow_toggles_whenNoSyncFileConfiguredYet() {
        var requestedEnabled: Boolean? = null

        composeTestRule.setContent {
            NarraTheme {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(autoExportUri = null),
                    onDownloadOverWifiOnlyChange = {},
                    onRefreshIntervalChange = {},
                    onInboxInitialLimitChange = {},
                    onImportOpml = {},
                    onExportOpml = {},
                    onBackupDatabase = {},
                    onRestoreDatabase = {},
                    onAutoExportEnabledChange = {},
                    onAutoImportEnabledChange = { requestedEnabled = it },
                    onSetSyncLocation = {},
                    onDeleteDatabase = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText(autoImportLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, requestedEnabled)
    }
}
