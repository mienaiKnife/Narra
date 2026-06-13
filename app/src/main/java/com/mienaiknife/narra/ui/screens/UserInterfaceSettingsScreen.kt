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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.flashHighlight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeManager
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import com.mienaiknife.narra.ui.theme.getFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInterfaceSettingsScreen(
    themeViewModel: ThemeViewModel,
    highlightSetting: String? = null,
    onBack: () -> Unit
) {
    val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = uiState.isDarkMode
    val isDynamicColor = uiState.isDynamicColor
    val useSystemTheme = uiState.useSystemTheme

    val useSystemThemeRequester = remember { BringIntoViewRequester() }
    val darkModeRequester = remember { BringIntoViewRequester() }
    val dynamicColorsRequester = remember { BringIntoViewRequester() }
    val readerFontFamilyRequester = remember { BringIntoViewRequester() }
    val lineSpacingRequester = remember { BringIntoViewRequester() }
    val tapToShowControlsRequester = remember { BringIntoViewRequester() }
    val readerFontSizeRequester = remember { BringIntoViewRequester() }
    val showRemainingTimeRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(highlightSetting) {
        when (highlightSetting) {
            "useSystemTheme" -> useSystemThemeRequester.bringIntoView()
            "darkMode" -> darkModeRequester.bringIntoView()
            "dynamicColors" -> dynamicColorsRequester.bringIntoView()
            "readerFontFamily" -> readerFontFamilyRequester.bringIntoView()
            "lineSpacing" -> lineSpacingRequester.bringIntoView()
            "tapToShowControls" -> tapToShowControlsRequester.bringIntoView()
            "readerFontSize" -> readerFontSizeRequester.bringIntoView()
            "showRemainingTime" -> showRemainingTimeRequester.bringIntoView()
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
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.settings_ui_title),
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
                text = stringResource(R.string.settings_ui_theme_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(useSystemThemeRequester)
                    .flashHighlight(highlightSetting == "useSystemTheme")
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ui_use_system_theme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_ui_use_system_theme_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useSystemTheme,
                    onCheckedChange = { themeViewModel.setUseSystemTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            if (!useSystemTheme) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(darkModeRequester)
                        .flashHighlight(highlightSetting == "darkMode")
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ui_dark_mode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.settings_ui_dark_mode_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { themeViewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f)
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(dynamicColorsRequester)
                    .flashHighlight(highlightSetting == "dynamicColors")
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ui_dynamic_colors),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_ui_dynamic_colors_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDynamicColor,
                    onCheckedChange = { themeViewModel.setDynamicColor(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            Text(
                text = stringResource(R.string.settings_ui_reader_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_ui_reader_font_family),
                subtitle = stringResource(R.string.settings_ui_reader_font_family_desc),
                selectedValue = uiState.readerFontFamily,
                options = listOf("Roboto", "OpenDyslexic3"),
                onValueChange = themeViewModel::setReaderFontFamily,
                modifier = Modifier
                    .bringIntoViewRequester(readerFontFamilyRequester)
                    .flashHighlight(highlightSetting == "readerFontFamily")
            )

            SettingDropDownItem(
                title = stringResource(R.string.settings_ui_line_spacing),
                subtitle = stringResource(R.string.settings_ui_line_spacing_desc),
                selectedValue = uiState.lineSpacing,
                options = listOf("1.0", "1.2", "1.4", "1.6", "1.8", "2.0"),
                onValueChange = themeViewModel::setLineSpacing,
                modifier = Modifier
                    .bringIntoViewRequester(lineSpacingRequester)
                    .flashHighlight(highlightSetting == "lineSpacing")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(showRemainingTimeRequester)
                    .flashHighlight(highlightSetting == "showRemainingTime")
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ui_auto_fullscreen),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_ui_auto_fullscreen_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoFullscreen,
                    onCheckedChange = { themeViewModel.setAutoFullscreen(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }

            if (uiState.autoFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(tapToShowControlsRequester)
                        .flashHighlight(highlightSetting == "tapToShowControls")
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ui_tap_to_show),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.settings_ui_tap_to_show_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.tapToShowControls,
                        onCheckedChange = { themeViewModel.setTapToShowControls(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sliderColors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier
                .bringIntoViewRequester(readerFontSizeRequester)
                .flashHighlight(highlightSetting == "readerFontSize")
                .padding(bottom = 16.dp)) {
                Text(
                    text = stringResource(R.string.settings_ui_font_size_label, uiState.readerFontSize.toInt()),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_ui_font_size_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = uiState.readerFontSize,
                    onValueChange = { themeViewModel.setReaderFontSize(it) },
                    valueRange = 12f..36f,
                    steps = 24, // Increments of 1
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors,
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = sliderColors
                        )
                    }
                )
            }

            Text(
                text = stringResource(R.string.settings_ui_text_info_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(showRemainingTimeRequester)
                    .flashHighlight(highlightSetting == "showRemainingTime")
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ui_show_remaining_time),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_ui_show_remaining_time_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.showRemainingTime,
                    onCheckedChange = { themeViewModel.setShowRemainingTime(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun UserInterfaceSettingsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val themeViewModel = remember { ThemeViewModel(themeManager) }
    val navController = rememberNavController()
    val fontFamily = getFontFamily("Roboto")
    NarraTheme(darkTheme = true, dynamicColor = false, fontFamily = fontFamily) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                UserInterfaceSettingsScreen(themeViewModel, highlightSetting = null, onBack = {})
            }
        }
    }
}
