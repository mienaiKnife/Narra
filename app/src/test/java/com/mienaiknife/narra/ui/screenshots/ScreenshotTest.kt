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
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.UiText
import com.mienaiknife.narra.ui.components.ErrorView
import com.mienaiknife.narra.ui.screens.AboutContent
import com.mienaiknife.narra.ui.screens.AddScreenContent
import com.mienaiknife.narra.ui.screens.DownloadsSettingsContent
import com.mienaiknife.narra.ui.screens.FeedArticlesScreenContent
import com.mienaiknife.narra.ui.screens.FeedsScreenContent
import com.mienaiknife.narra.ui.screens.HistoryScreenContent
import com.mienaiknife.narra.ui.screens.HomeScreenContent
import com.mienaiknife.narra.ui.screens.InboxScreenContent
import com.mienaiknife.narra.ui.screens.Library
import com.mienaiknife.narra.ui.screens.LicensesContent
import com.mienaiknife.narra.ui.screens.PlaybackSettingsContent
import com.mienaiknife.narra.ui.screens.QueueScreenContent
import com.mienaiknife.narra.ui.screens.SettingsScreen
import com.mienaiknife.narra.ui.screens.UserInterfaceSettingsContent
import com.mienaiknife.narra.ui.theme.ThemeUiState
import com.mienaiknife.narra.ui.viewmodels.DownloadsSettingsUiState
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesUiState
import com.mienaiknife.narra.ui.viewmodels.HomeUiState
import com.mienaiknife.narra.ui.viewmodels.PlaybackSettingsUiState
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
        )

    @Test
    fun homeScreen_Success() {
        paparazzi.snapshot {
            SnapshotSurface {
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
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun homeScreen_SuccessDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
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
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun homeScreen_Populated() {
        paparazzi.snapshot {
            SnapshotSurface {
                HomeScreenContent(
                    uiState =
                    HomeUiState.Success(
                        continueListening = SampleArticles.all.take(5),
                        newFromFeeds = SampleArticles.all.filter { it.isFromFeed }.take(5),
                        favoriteArticles = SampleArticles.all.filter { it.isFavorite }.take(5),
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onArticleClick = {},
                    onAddClick = {},
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun homeScreen_PopulatedDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                HomeScreenContent(
                    uiState =
                    HomeUiState.Success(
                        continueListening = SampleArticles.all.take(5),
                        newFromFeeds = SampleArticles.all.filter { it.isFromFeed }.take(5),
                        favoriteArticles = SampleArticles.all.filter { it.isFavorite }.take(5),
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onArticleClick = {},
                    onAddClick = {},
                    onRefresh = {},
                )
            }
        }
    }

    @Test
    fun queueScreen_Empty() {
        paparazzi.snapshot {
            SnapshotSurface {
                QueueScreenContent(articles = emptyList())
            }
        }
    }

    @Test
    fun queueScreen_WithArticles() {
        paparazzi.snapshot {
            SnapshotSurface {
                QueueScreenContent(articles = listOf(SampleArticles.sampleArticle1))
            }
        }
    }

    @Test
    fun queueScreen_WithArticlesDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                QueueScreenContent(articles = listOf(SampleArticles.sampleArticle1))
            }
        }
    }

    @Test
    fun inboxScreen_Populated() {
        paparazzi.snapshot {
            SnapshotSurface {
                InboxScreenContent(
                    articles = SampleArticles.all.filter { it.isFromFeed },
                    onAddToQueue = {},
                )
            }
        }
    }

    @Test
    fun inboxScreen_PopulatedDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                InboxScreenContent(
                    articles = SampleArticles.all.filter { it.isFromFeed },
                    onAddToQueue = {},
                )
            }
        }
    }

    @Test
    fun historyScreen_Empty() {
        paparazzi.snapshot {
            SnapshotSurface {
                HistoryScreenContent(
                    articles = emptyList(),
                    onBackClick = {},
                    onPlayPauseClick = {},
                )
            }
        }
    }

    @Test
    fun historyScreen_PopulatedDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                HistoryScreenContent(
                    articles = SampleArticles.all.take(5),
                    onBackClick = {},
                    onPlayPauseClick = {},
                )
            }
        }
    }

    @Test
    fun feedsScreen_Empty() {
        paparazzi.snapshot {
            SnapshotSurface {
                FeedsScreenContent(
                    onFeedClick = { _, _ -> },
                    feeds = emptyList(),
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun feedsScreen_EmptyDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                FeedsScreenContent(
                    onFeedClick = { _, _ -> },
                    feeds = emptyList(),
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun feedsScreen_WithRefreshError() {
        paparazzi.snapshot {
            SnapshotSurface {
                FeedsScreenContent(
                    onFeedClick = { _, _ -> },
                    feeds = listOf(
                        FeedEntity(
                            url = "https://working.example.com/feed",
                            title = "Working Feed",
                            description = "Refreshing normally",
                        ),
                        FeedEntity(
                            url = "https://blocked.example.com/feed",
                            title = "Blocked Feed",
                            description = "Cloudflare challenge",
                            lastRefreshErrorAt = 1L,
                        ),
                    ),
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun feedArticlesScreen_Populated() {
        paparazzi.snapshot {
            SnapshotSurface {
                FeedArticlesScreenContent(
                    uiState =
                    FeedArticlesUiState(
                        articles = SampleArticles.all.take(5),
                        feedTitle = "The Verge",
                    ),
                    onBackClick = {},
                    onAddToQueue = {},
                    onDeleteArticle = {},
                )
            }
        }
    }

    @Test
    fun addScreen_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                AddScreenContent(
                    url = "",
                    onUrlChange = {},
                    onDownloadClick = {},
                    onSubscribeClick = {},
                    onUploadClick = {},
                )
            }
        }
    }

    @Test
    fun settingsScreen_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                SettingsScreen(
                    onNavigateToUserInterface = {},
                    onNavigateToPlayback = {},
                    onNavigateToVoices = {},
                    onNavigateToDownloads = {},
                    onNavigateToAbout = {},
                )
            }
        }
    }

    @Test
    fun settingsScreen_Dark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                SettingsScreen(
                    onNavigateToUserInterface = {},
                    onNavigateToPlayback = {},
                    onNavigateToVoices = {},
                    onNavigateToDownloads = {},
                    onNavigateToAbout = {},
                )
            }
        }
    }

    @Test
    fun userInterfaceSettings_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                UserInterfaceSettingsContent(
                    uiState = ThemeUiState(),
                    highlightSetting = null,
                    onBack = {},
                    onSetUseSystemTheme = {},
                    onSetDarkMode = {},
                    onSetDynamicColor = {},
                    onSetReaderFontFamily = {},
                    onSetLineSpacing = {},
                    onSetAutoFullscreen = {},
                    onSetTapToShowControls = {},
                    onSetReaderFontSize = {},
                    onSetShowRemainingTime = {},
                )
            }
        }
    }

    @Test
    fun userInterfaceSettings_Dark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                UserInterfaceSettingsContent(
                    uiState = ThemeUiState(),
                    highlightSetting = null,
                    onBack = {},
                    onSetUseSystemTheme = {},
                    onSetDarkMode = {},
                    onSetDynamicColor = {},
                    onSetReaderFontFamily = {},
                    onSetLineSpacing = {},
                    onSetAutoFullscreen = {},
                    onSetTapToShowControls = {},
                    onSetReaderFontSize = {},
                    onSetShowRemainingTime = {},
                )
            }
        }
    }

    @Test
    fun downloadsSettings_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(),
                    highlightSetting = null,
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
    }

    @Test
    fun downloadsSettings_Dark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                DownloadsSettingsContent(
                    uiState = DownloadsSettingsUiState(),
                    highlightSetting = null,
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
    }

    @Test
    fun playbackSettings_Success() {
        paparazzi.snapshot {
            SnapshotSurface {
                PlaybackSettingsContent(
                    onBack = {},
                    uiState = PlaybackSettingsUiState(),
                    onPauseOnDisconnectChange = {},
                    onPauseForInterruptionsChange = {},
                    onAutoPlayNextChange = {},
                    onPlayChimeAndTitleChange = {},
                    onChimeSoundChange = {},
                    onFastForwardTimeChange = {},
                    onRewindTimeChange = {},
                    onFastForwardHardwareButtonChange = {},
                    onRewindHardwareButtonChange = {},
                    onReadAltTextChange = {},
                    onShortenHyperlinksChange = {},
                )
            }
        }
    }

    @Test
    fun playbackSettings_SuccessDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                PlaybackSettingsContent(
                    onBack = {},
                    uiState = PlaybackSettingsUiState(),
                    onPauseOnDisconnectChange = {},
                    onPauseForInterruptionsChange = {},
                    onAutoPlayNextChange = {},
                    onPlayChimeAndTitleChange = {},
                    onChimeSoundChange = {},
                    onFastForwardTimeChange = {},
                    onRewindTimeChange = {},
                    onFastForwardHardwareButtonChange = {},
                    onRewindHardwareButtonChange = {},
                    onReadAltTextChange = {},
                    onShortenHyperlinksChange = {},
                )
            }
        }
    }

    @Test
    fun aboutScreen_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                AboutContent(
                    versionName = "1.0.0",
                    onBack = {},
                    onOpenRepository = {},
                    onOpenPrivacyPolicy = {},
                    onNavigateToLicenses = {},
                )
            }
        }
    }

    @Test
    fun licensesScreen_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                LicensesContent(
                    libraries = listOf(
                        Library("Kotlin", "JetBrains", "Apache 2.0"),
                        Library("Coil", "Coil Contributors", "Apache 2.0"),
                        Library("Jsoup", "Jonathan Hedley", "MIT"),
                    ),
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun reader_Error() {
        paparazzi.snapshot {
            SnapshotSurface {
                ErrorView(
                    error = UiText.StringResource(R.string.error_generic),
                    onRetry = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun reader_ErrorDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                ErrorView(
                    error = UiText.StringResource(R.string.error_generic),
                    onRetry = {},
                    onBack = {},
                )
            }
        }
    }
}
