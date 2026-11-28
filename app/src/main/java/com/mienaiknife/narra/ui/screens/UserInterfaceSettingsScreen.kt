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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.SettingDropDownItem
import com.mienaiknife.narra.ui.components.SettingsScaffold
import com.mienaiknife.narra.ui.components.SettingsSectionLabel
import com.mienaiknife.narra.ui.components.SettingsSliderRow
import com.mienaiknife.narra.ui.components.SettingsSwitchRow
import com.mienaiknife.narra.ui.components.flashHighlight
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeUiState
import com.mienaiknife.narra.ui.theme.ThemeViewModel

@Composable
fun UserInterfaceSettingsScreen(
    themeViewModel: ThemeViewModel,
    highlightSetting: String? = null,
    onBack: () -> Unit,
) {
    val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()

    UserInterfaceSettingsContent(
        uiState = uiState,
        highlightSetting = highlightSetting,
        onBack = onBack,
        onSetUseSystemTheme = themeViewModel::setUseSystemTheme,
        onSetDarkMode = themeViewModel::setDarkMode,
        onSetDynamicColor = themeViewModel::setDynamicColor,
        onSetReaderFontFamily = themeViewModel::setReaderFontFamily,
        onSetLineSpacing = themeViewModel::setLineSpacing,
        onSetAutoFullscreen = themeViewModel::setAutoFullscreen,
        onSetTapToShowControls = themeViewModel::setTapToShowControls,
        onSetReaderFontSize = themeViewModel::setReaderFontSize,
        onSetShowRemainingTime = themeViewModel::setShowRemainingTime,
    )
}

@Composable
fun UserInterfaceSettingsContent(
    uiState: ThemeUiState,
    highlightSetting: String?,
    onBack: () -> Unit,
    onSetUseSystemTheme: (Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetReaderFontFamily: (String) -> Unit,
    onSetLineSpacing: (String) -> Unit,
    onSetAutoFullscreen: (Boolean) -> Unit,
    onSetTapToShowControls: (Boolean) -> Unit,
    onSetReaderFontSize: (Float) -> Unit,
    onSetShowRemainingTime: (Boolean) -> Unit,
) {
    val useSystemThemeRequester = remember { BringIntoViewRequester() }
    val darkModeRequester = remember { BringIntoViewRequester() }
    val dynamicColorsRequester = remember { BringIntoViewRequester() }
    val readerFontFamilyRequester = remember { BringIntoViewRequester() }
    val lineSpacingRequester = remember { BringIntoViewRequester() }
    val tapToShowControlsRequester = remember { BringIntoViewRequester() }
    val readerFontSizeRequester = remember { BringIntoViewRequester() }
    val showRemainingTimeRequester = remember { BringIntoViewRequester() }
    val autoFullscreenRequester = remember { BringIntoViewRequester() }

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
            "autoFullscreen" -> autoFullscreenRequester.bringIntoView()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_ui_title),
        onBack = onBack,
    ) {
        SettingsSectionLabel(
            text = stringResource(R.string.settings_ui_theme_section),
            first = true,
        )

        SettingsSwitchRow(
            title = stringResource(R.string.settings_ui_use_system_theme),
            subtitle = stringResource(R.string.settings_ui_use_system_theme_desc),
            checked = uiState.useSystemTheme,
            onCheckedChange = onSetUseSystemTheme,
            modifier = Modifier
                .bringIntoViewRequester(useSystemThemeRequester)
                .flashHighlight(highlightSetting == "useSystemTheme"),
        )

        if (!uiState.useSystemTheme) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_ui_dark_mode),
                subtitle = stringResource(R.string.settings_ui_dark_mode_desc),
                checked = uiState.isDarkMode,
                onCheckedChange = onSetDarkMode,
                modifier = Modifier
                    .bringIntoViewRequester(darkModeRequester)
                    .flashHighlight(highlightSetting == "darkMode"),
                enabled = true,
            )
        }

        SettingsSwitchRow(
            title = stringResource(R.string.settings_ui_dynamic_colors),
            subtitle = stringResource(R.string.settings_ui_dynamic_colors_desc),
            checked = uiState.isDynamicColor,
            onCheckedChange = onSetDynamicColor,
            modifier = Modifier
                .bringIntoViewRequester(dynamicColorsRequester)
                .flashHighlight(highlightSetting == "dynamicColors"),
        )

        SettingsSectionLabel(text = stringResource(R.string.settings_ui_reader_section))

        SettingDropDownItem(
            title = stringResource(R.string.settings_ui_reader_font_family),
            subtitle = stringResource(R.string.settings_ui_reader_font_family_desc),
            selectedValue = uiState.readerFontFamily,
            options = listOf("Roboto", "OpenDyslexic3"),
            onValueChange = onSetReaderFontFamily,
            modifier = Modifier
                .bringIntoViewRequester(readerFontFamilyRequester)
                .flashHighlight(highlightSetting == "readerFontFamily"),
        )

        SettingDropDownItem(
            title = stringResource(R.string.settings_ui_line_spacing),
            subtitle = stringResource(R.string.settings_ui_line_spacing_desc),
            selectedValue = uiState.lineSpacing,
            options = listOf("1.0", "1.2", "1.4", "1.6", "1.8", "2.0"),
            onValueChange = onSetLineSpacing,
            modifier = Modifier
                .bringIntoViewRequester(lineSpacingRequester)
                .flashHighlight(highlightSetting == "lineSpacing"),
        )

        SettingsSwitchRow(
            title = stringResource(R.string.settings_ui_auto_fullscreen),
            subtitle = stringResource(R.string.settings_ui_auto_fullscreen_desc),
            checked = uiState.autoFullscreen,
            onCheckedChange = onSetAutoFullscreen,
            modifier = Modifier
                .bringIntoViewRequester(autoFullscreenRequester)
                .flashHighlight(highlightSetting == "autoFullscreen"),
        )

        if (uiState.autoFullscreen) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_ui_tap_to_show),
                subtitle = stringResource(R.string.settings_ui_tap_to_show_desc),
                checked = uiState.tapToShowControls,
                onCheckedChange = onSetTapToShowControls,
                modifier = Modifier
                    .bringIntoViewRequester(tapToShowControlsRequester)
                    .flashHighlight(highlightSetting == "tapToShowControls"),
            )
        }

        SettingsSliderRow(
            title = stringResource(R.string.settings_ui_font_size_label, uiState.readerFontSize.toInt()),
            subtitle = stringResource(R.string.settings_ui_font_size_desc),
            value = uiState.readerFontSize,
            valueRange = 12f..36f,
            steps = 23, // Increments of 1
            onValueChange = onSetReaderFontSize,
            modifier = Modifier
                .bringIntoViewRequester(readerFontSizeRequester)
                .flashHighlight(highlightSetting == "readerFontSize"),
        )

        SettingsSectionLabel(text = stringResource(R.string.settings_ui_text_info_section))

        SettingsSwitchRow(
            title = stringResource(R.string.settings_ui_show_remaining_time),
            subtitle = stringResource(R.string.settings_ui_show_remaining_time_desc),
            checked = uiState.showRemainingTime,
            onCheckedChange = onSetShowRemainingTime,
            modifier = Modifier
                .bringIntoViewRequester(showRemainingTimeRequester)
                .flashHighlight(highlightSetting == "showRemainingTime"),
        )
    }
}

@PreviewLightDark
@Composable
fun UserInterfaceSettingsScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
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
}
