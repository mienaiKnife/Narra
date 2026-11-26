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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.ui.screens.PronunciationEditorDialog
import com.mienaiknife.narra.ui.screens.PronunciationsSettingsContent
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.PronunciationsUiState
import org.junit.Rule
import org.junit.Test

class PronunciationsScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
        )

    private val entries = listOf(
        CustomPronunciation(word = "kokoro", arpabet = "K OW0 K AO1 R OW0"),
        CustomPronunciation(word = "narra", arpabet = "N AA1 R AH0"),
        CustomPronunciation(word = "sherpa", arpabet = "SH ER1 P AH0"),
    )

    @Test
    fun pronunciationsSettings_Empty() {
        paparazzi.snapshot {
            SnapshotSurface {
                PronunciationsSettingsContent(
                    uiState = PronunciationsUiState(engineState = TtsState.Ready),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                    onPreview = {},
                )
            }
        }
    }

    @Test
    fun pronunciationsSettings_WithEntries() {
        paparazzi.snapshot {
            SnapshotSurface {
                PronunciationsSettingsContent(
                    uiState = PronunciationsUiState(
                        entries = entries,
                        engineState = TtsState.Ready,
                    ),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                    onPreview = {},
                )
            }
        }
    }

    @Test
    fun pronunciationsSettings_WithEntriesDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                PronunciationsSettingsContent(
                    uiState = PronunciationsUiState(
                        entries = entries,
                        engineState = TtsState.Ready,
                    ),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                    onPreview = {},
                )
            }
        }
    }

    @Test
    fun pronunciationEditorDialog_Add() {
        paparazzi.snapshot {
            SnapshotSurface {
                PronunciationEditorDialog(
                    initialEntry = null,
                    onDismiss = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}

/** Mirrors the host scaffold background so snapshots do not render on Paparazzi's raw canvas. */
@Composable
private fun SnapshotSurface(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    NarraTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            content()
        }
    }
}
