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

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.mienaiknife.narra.ui.components.flashHighlight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.PlaybackSettingsUiState
import com.mienaiknife.narra.ui.viewmodels.PlaybackSettingsViewModel

@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit,
    highlightSetting: String? = null,
    viewModel: PlaybackSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaybackSettingsContent(
        onBack = onBack,
        uiState = uiState,
        highlightSetting = highlightSetting,
        onPauseOnDisconnectChange = { viewModel.setPauseOnDisconnect(it) },
        onPauseForInterruptionsChange = { viewModel.setPauseForInterruptions(it) },
        onAutoPlayNextChange = { viewModel.setAutoPlayNext(it) },
        onPlayChimeAndTitleChange = { viewModel.setPlayChimeAndTitle(it) },
        onChimeSoundChange = { viewModel.setChimeSound(it) },
        onFastForwardTimeChange = { viewModel.setFastForwardSkipTime(it) },
        onRewindTimeChange = { viewModel.setRewindSkipTime(it) },
        onFastForwardHardwareButtonChange = { viewModel.setFastForwardHardwareButton(it) },
        onRewindHardwareButtonChange = { viewModel.setRewindHardwareButton(it) }
    )
}

@Composable
fun PlaybackSettingsContent(
    onBack: () -> Unit,
    uiState: PlaybackSettingsUiState,
    highlightSetting: String? = null,
    onPauseOnDisconnectChange: (Boolean) -> Unit,
    onPauseForInterruptionsChange: (Boolean) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onPlayChimeAndTitleChange: (Boolean) -> Unit,
    onChimeSoundChange: (String) -> Unit,
    onFastForwardTimeChange: (String) -> Unit,
    onRewindTimeChange: (String) -> Unit,
    onFastForwardHardwareButtonChange: (String) -> Unit,
    onRewindHardwareButtonChange: (String) -> Unit
) {
    val pauseOnDisconnectRequester = remember { BringIntoViewRequester() }
    val pauseForInterruptionsRequester = remember { BringIntoViewRequester() }
    val fastForwardSkipTimeRequester = remember { BringIntoViewRequester() }
    val rewindSkipTimeRequester = remember { BringIntoViewRequester() }
    val fastForwardHardwareButtonRequester = remember { BringIntoViewRequester() }
    val rewindHardwareButtonRequester = remember { BringIntoViewRequester() }
    val autoPlayNextRequester = remember { BringIntoViewRequester() }
    val playChimeAndTitleRequester = remember { BringIntoViewRequester() }
    val chimeSoundRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(highlightSetting) {
        when (highlightSetting) {
            "pauseOnDisconnect" -> pauseOnDisconnectRequester.bringIntoView()
            "pauseForInterruptions" -> pauseForInterruptionsRequester.bringIntoView()
            "fastForwardSkipTime" -> fastForwardSkipTimeRequester.bringIntoView()
            "rewindSkipTime" -> rewindSkipTimeRequester.bringIntoView()
            "fastForwardHardwareButton" -> fastForwardHardwareButtonRequester.bringIntoView()
            "rewindHardwareButton" -> rewindHardwareButtonRequester.bringIntoView()
            "autoPlayNext" -> autoPlayNextRequester.bringIntoView()
            "playChimeAndTitle" -> playChimeAndTitleRequester.bringIntoView()
            "chimeSound" -> chimeSoundRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Playback",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Interruptions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(pauseOnDisconnectRequester)
                    .flashHighlight(highlightSetting == "pauseOnDisconnect")
                    .clickable { onPauseOnDisconnectChange(!uiState.pauseOnDisconnect) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Pause on disconnect",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Pause playback when headphones or Bluetooth devices are disconnected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.pauseOnDisconnect,
                    onCheckedChange = onPauseOnDisconnectChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(pauseForInterruptionsRequester)
                    .flashHighlight(highlightSetting == "pauseForInterruptions")
                    .clickable { onPauseForInterruptionsChange(!uiState.pauseForInterruptions) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Pause for interruptions",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Pause playback instead of lowering volume for notification sounds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.pauseForInterruptions,
                    onCheckedChange = onPauseForInterruptionsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            Text(
                text = "Playback controls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            SettingDropDownItem(
                title = "Fast forward skip time",
                subtitle = "Customize how much the fast forward button skips forward",
                selectedValue = uiState.fastForwardSkipTime,
                options = listOf("10s", "15s", "30s", "60s"),
                onValueChange = onFastForwardTimeChange,
                modifier = Modifier
                    .bringIntoViewRequester(fastForwardSkipTimeRequester)
                    .flashHighlight(highlightSetting == "fastForwardSkipTime")
            )

            SettingDropDownItem(
                title = "Rewind skip time",
                subtitle = "Customize how much the rewind button skips backwards",
                selectedValue = uiState.rewindSkipTime,
                options = listOf("10s", "15s", "30s", "60s"),
                onValueChange = onRewindTimeChange,
                modifier = Modifier
                    .bringIntoViewRequester(rewindSkipTimeRequester)
                    .flashHighlight(highlightSetting == "rewindSkipTime")
            )

            SettingDropDownItem(
                title = "Fast forward hardware button",
                subtitle = "Customize what the fast forward hardware button does",
                selectedValue = uiState.fastForwardHardwareButton,
                options = listOf("Fast forward", "Skip article", "Rewind", "Restart article"),
                onValueChange = onFastForwardHardwareButtonChange,
                modifier = Modifier
                    .bringIntoViewRequester(fastForwardHardwareButtonRequester)
                    .flashHighlight(highlightSetting == "fastForwardHardwareButton")
            )

            SettingDropDownItem(
                title = "Rewind hardware button",
                subtitle = "Customize what the rewind hardware button does",
                selectedValue = uiState.rewindHardwareButton,
                options = listOf("Fast forward", "Skip article", "Rewind", "Restart article"),
                onValueChange = onRewindHardwareButtonChange,
                modifier = Modifier
                    .bringIntoViewRequester(rewindHardwareButtonRequester)
                    .flashHighlight(highlightSetting == "rewindHardwareButton")
            )
            
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(autoPlayNextRequester)
                    .flashHighlight(highlightSetting == "autoPlayNext")
                    .clickable { onAutoPlayNextChange(!uiState.autoPlayNext) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Autoplay next text",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Automatically play the next text in the queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoPlayNext,
                    onCheckedChange = onAutoPlayNextChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(playChimeAndTitleRequester)
                    .flashHighlight(highlightSetting == "playChimeAndTitle")
                    .clickable { onPlayChimeAndTitleChange(!uiState.playChimeAndTitle) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Play chime and title",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "When autoplaying the next text in the queue, start by playing a chime and the title of the text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.playChimeAndTitle,
                    onCheckedChange = onPlayChimeAndTitleChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            SettingDropDownItem(
                title = "Chime sound",
                subtitle = "Choose the sound to play before the title",
                selectedValue = uiState.chimeSound,
                options = listOf("music_box_chime_positive", "vibraphone_chime_positive"),
                onValueChange = onChimeSoundChange,
                modifier = Modifier
                    .bringIntoViewRequester(chimeSoundRequester)
                    .flashHighlight(highlightSetting == "chimeSound")
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PlaybackSettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                PlaybackSettingsContent(
                    onBack = {},
                    uiState = PlaybackSettingsUiState(),
                    highlightSetting = null,
                    onPauseOnDisconnectChange = {},
                    onPauseForInterruptionsChange = {},
                    onAutoPlayNextChange = {},
                    onPlayChimeAndTitleChange = {},
                    onChimeSoundChange = {},
                    onFastForwardTimeChange = {},
                    onRewindTimeChange = {},
                    onFastForwardHardwareButtonChange = {},
                    onRewindHardwareButtonChange = {}
                )
            }
        }
    }
}
