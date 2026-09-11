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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.playback.HardwareButtonAction
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.components.SettingsSwitchRow
import com.mienaiknife.narra.ui.components.flashHighlight
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.PlaybackSettingsUiState
import com.mienaiknife.narra.ui.viewmodels.PlaybackSettingsViewModel

@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit,
    highlightSetting: String? = null,
    viewModel: PlaybackSettingsViewModel = hiltViewModel(),
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
        onRewindHardwareButtonChange = { viewModel.setRewindHardwareButton(it) },
        onReadAltTextChange = { viewModel.setReadAltText(it) },
        onShortenHyperlinksChange = { viewModel.setShortenHyperlinks(it) },
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
    onRewindHardwareButtonChange: (String) -> Unit,
    onReadAltTextChange: (Boolean) -> Unit,
    onShortenHyperlinksChange: (Boolean) -> Unit,
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
    val readAltTextRequester = remember { BringIntoViewRequester() }
    val shortenHyperlinksRequester = remember { BringIntoViewRequester() }

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
            "readAltText" -> readAltTextRequester.bringIntoView()
            "shortenHyperlinks" -> shortenHyperlinksRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ScreenHeader(
            title = stringResource(R.string.settings_playback_title),
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_playback_interruptions_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_pause_on_disconnect),
                subtitle = stringResource(R.string.settings_playback_pause_on_disconnect_desc),
                checked = uiState.pauseOnDisconnect,
                onCheckedChange = onPauseOnDisconnectChange,
                modifier = Modifier
                    .bringIntoViewRequester(pauseOnDisconnectRequester)
                    .flashHighlight(highlightSetting == "pauseOnDisconnect"),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_pause_for_interruptions),
                subtitle = stringResource(R.string.settings_playback_pause_for_interruptions_desc),
                checked = uiState.pauseForInterruptions,
                onCheckedChange = onPauseForInterruptionsChange,
                modifier = Modifier
                    .bringIntoViewRequester(pauseForInterruptionsRequester)
                    .flashHighlight(highlightSetting == "pauseForInterruptions"),
            )

            Text(
                text = stringResource(R.string.settings_playback_controls_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_playback_ff_skip),
                subtitle = stringResource(R.string.settings_playback_ff_skip_desc),
                selectedValue = uiState.fastForwardSkipTime,
                options = listOf("10s", "15s", "30s", "60s"),
                onValueChange = onFastForwardTimeChange,
                optionLabel = { skipTimeLabel(it) },
                modifier = Modifier
                    .bringIntoViewRequester(fastForwardSkipTimeRequester)
                    .flashHighlight(highlightSetting == "fastForwardSkipTime"),
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_playback_rw_skip),
                subtitle = stringResource(R.string.settings_playback_rw_skip_desc),
                selectedValue = uiState.rewindSkipTime,
                options = listOf("10s", "15s", "30s", "60s"),
                onValueChange = onRewindTimeChange,
                optionLabel = { skipTimeLabel(it) },
                modifier = Modifier
                    .bringIntoViewRequester(rewindSkipTimeRequester)
                    .flashHighlight(highlightSetting == "rewindSkipTime"),
            )

            val fastForwardOptionLabels = HardwareButtonAction.fastForwardOptions.map { hardwareActionLabel(it) to it.key }
            val rewindOptionLabels = HardwareButtonAction.rewindOptions.map { hardwareActionLabel(it) to it.key }

            SettingDropDownItem(
                title = stringResource(R.string.settings_playback_ff_hardware),
                subtitle = stringResource(R.string.settings_playback_ff_hardware_desc),
                selectedValue = fastForwardOptionLabels.find { it.second == uiState.fastForwardHardwareButton }?.first ?: uiState.fastForwardHardwareButton,
                options = fastForwardOptionLabels.map { it.first },
                onValueChange = { selectedDisplay ->
                    val key = fastForwardOptionLabels.find { it.first == selectedDisplay }?.second ?: selectedDisplay
                    onFastForwardHardwareButtonChange(key)
                },
                modifier = Modifier
                    .bringIntoViewRequester(fastForwardHardwareButtonRequester)
                    .flashHighlight(highlightSetting == "fastForwardHardwareButton"),
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_playback_rw_hardware),
                subtitle = stringResource(R.string.settings_playback_rw_hardware_desc),
                selectedValue = rewindOptionLabels.find { it.second == uiState.rewindHardwareButton }?.first ?: uiState.rewindHardwareButton,
                options = rewindOptionLabels.map { it.first },
                onValueChange = { selectedDisplay ->
                    val key = rewindOptionLabels.find { it.first == selectedDisplay }?.second ?: selectedDisplay
                    onRewindHardwareButtonChange(key)
                },
                modifier = Modifier
                    .bringIntoViewRequester(rewindHardwareButtonRequester)
                    .flashHighlight(highlightSetting == "rewindHardwareButton"),
            )

            Text(
                text = stringResource(R.string.settings_playback_queue_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_autoplay_next),
                subtitle = stringResource(R.string.settings_playback_autoplay_next_desc),
                checked = uiState.autoPlayNext,
                onCheckedChange = onAutoPlayNextChange,
                modifier = Modifier
                    .bringIntoViewRequester(autoPlayNextRequester)
                    .flashHighlight(highlightSetting == "autoPlayNext"),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_play_chime),
                subtitle = stringResource(R.string.settings_playback_play_chime_desc),
                checked = uiState.playChimeAndTitle,
                onCheckedChange = onPlayChimeAndTitleChange,
                modifier = Modifier
                    .bringIntoViewRequester(playChimeAndTitleRequester)
                    .flashHighlight(highlightSetting == "playChimeAndTitle"),
            )

            val chimeOptions = listOf(
                stringResource(R.string.settings_playback_chime_music_box) to "music_box_chime_positive",
                stringResource(R.string.settings_playback_chime_vibraphone) to "vibraphone_chime_positive",
            )
            SettingDropDownItem(
                title = stringResource(R.string.settings_playback_chime_sound),
                subtitle = stringResource(R.string.settings_playback_chime_sound_desc),
                selectedValue = chimeOptions.firstOrNull { it.second == uiState.chimeSound }?.first
                    ?: uiState.chimeSound,
                options = chimeOptions.map { it.first },
                onValueChange = { label ->
                    chimeOptions.firstOrNull { it.first == label }?.second?.let(onChimeSoundChange)
                },
                modifier = Modifier
                    .bringIntoViewRequester(chimeSoundRequester)
                    .flashHighlight(highlightSetting == "chimeSound"),
            )

            Text(
                text = stringResource(R.string.settings_playback_voice_behavior_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_read_alt_text),
                subtitle = stringResource(R.string.settings_playback_read_alt_text_desc),
                checked = uiState.readAltText,
                onCheckedChange = onReadAltTextChange,
                modifier = Modifier
                    .bringIntoViewRequester(readAltTextRequester)
                    .flashHighlight(highlightSetting == "readAltText"),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_playback_shorten_hyperlinks),
                subtitle = stringResource(R.string.settings_playback_shorten_hyperlinks_desc),
                checked = uiState.shortenHyperlinks,
                onCheckedChange = onShortenHyperlinksChange,
                modifier = Modifier
                    .bringIntoViewRequester(shortenHyperlinksRequester)
                    .flashHighlight(highlightSetting == "shortenHyperlinks"),
            )
        }
    }
}

@Composable
private fun hardwareActionLabel(action: HardwareButtonAction): String = when (action) {
    HardwareButtonAction.FAST_FORWARD -> stringResource(R.string.setting_ff)
    HardwareButtonAction.SKIP_ARTICLE -> stringResource(R.string.setting_skip_article)
    HardwareButtonAction.REWIND -> stringResource(R.string.setting_rewind)
    HardwareButtonAction.RESTART_ARTICLE -> stringResource(R.string.setting_restart_article)
}

@Composable
private fun skipTimeLabel(storedValue: String): String {
    val seconds = storedValue.removeSuffix("s").toIntOrNull() ?: return storedValue
    return pluralStringResource(R.plurals.unit_seconds, seconds, seconds)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PlaybackSettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
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
                    onRewindHardwareButtonChange = {},
                    onReadAltTextChange = {},
                    onShortenHyperlinksChange = {},
                )
            }
        }
    }
}
