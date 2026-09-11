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

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.mienaiknife.narra.NavDestination
import com.mienaiknife.narra.R

data class SearchableSetting(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val destination: NavDestination,
    @ArrayRes val keywordsRes: Int,
)

val allSearchableSettings =
    listOf(
        // User Interface
        SearchableSetting(
            "useSystemTheme",
            R.string.settings_ui_use_system_theme,
            R.string.settings_ui_use_system_theme_desc,
            NavDestination.SettingsUi("useSystemTheme"),
            R.array.settings_search_keywords_use_system_theme,
        ),
        SearchableSetting(
            "darkMode",
            R.string.settings_ui_dark_mode,
            R.string.settings_ui_dark_mode_desc,
            NavDestination.SettingsUi("darkMode"),
            R.array.settings_search_keywords_dark_mode,
        ),
        SearchableSetting(
            "dynamicColors",
            R.string.settings_ui_dynamic_colors,
            R.string.settings_ui_dynamic_colors_desc,
            NavDestination.SettingsUi("dynamicColors"),
            R.array.settings_search_keywords_dynamic_colors,
        ),
        SearchableSetting(
            "readerFontFamily",
            R.string.settings_ui_reader_font_family,
            R.string.settings_ui_reader_font_family_desc,
            NavDestination.SettingsUi("readerFontFamily"),
            R.array.settings_search_keywords_reader_font_family,
        ),
        SearchableSetting(
            "tapToShowControls",
            R.string.settings_ui_tap_to_show,
            R.string.settings_ui_tap_to_show_desc,
            NavDestination.SettingsUi("tapToShowControls"),
            R.array.settings_search_keywords_tap_to_show,
        ),
        SearchableSetting(
            "readerFontSize",
            R.string.settings_ui_reader_font_size,
            R.string.settings_ui_reader_font_size_desc,
            NavDestination.SettingsUi("readerFontSize"),
            R.array.settings_search_keywords_reader_font_size,
        ),
        SearchableSetting(
            "lineSpacing",
            R.string.settings_ui_line_spacing,
            R.string.settings_ui_line_spacing_desc,
            NavDestination.SettingsUi("lineSpacing"),
            R.array.settings_search_keywords_line_spacing,
        ),
        SearchableSetting(
            "showRemainingTime",
            R.string.settings_ui_show_remaining_time,
            R.string.settings_ui_show_remaining_time_desc,
            NavDestination.SettingsUi("showRemainingTime"),
            R.array.settings_search_keywords_show_remaining_time,
        ),
        SearchableSetting(
            "autoFullscreen",
            R.string.settings_ui_auto_fullscreen,
            R.string.settings_ui_auto_fullscreen_desc,
            NavDestination.SettingsUi("autoFullscreen"),
            R.array.settings_search_keywords_auto_fullscreen,
        ),
        // Playback
        SearchableSetting(
            "pauseOnDisconnect",
            R.string.settings_playback_pause_on_disconnect,
            R.string.settings_playback_pause_on_disconnect_desc,
            NavDestination.SettingsPlayback("pauseOnDisconnect"),
            R.array.settings_search_keywords_pause_on_disconnect,
        ),
        SearchableSetting(
            "pauseForInterruptions",
            R.string.settings_playback_pause_for_interruptions,
            R.string.settings_playback_pause_for_interruptions_desc,
            NavDestination.SettingsPlayback("pauseForInterruptions"),
            R.array.settings_search_keywords_pause_for_interruptions,
        ),
        SearchableSetting(
            "fastForwardSkipTime",
            R.string.settings_playback_ff_skip,
            R.string.settings_playback_ff_skip_desc,
            NavDestination.SettingsPlayback("fastForwardSkipTime"),
            R.array.settings_search_keywords_fast_forward_skip,
        ),
        SearchableSetting(
            "rewindSkipTime",
            R.string.settings_playback_rw_skip,
            R.string.settings_playback_rw_skip_desc,
            NavDestination.SettingsPlayback("rewindSkipTime"),
            R.array.settings_search_keywords_rewind_skip,
        ),
        SearchableSetting(
            "fastForwardHardwareButton",
            R.string.settings_playback_ff_hardware,
            R.string.settings_playback_ff_hardware_desc,
            NavDestination.SettingsPlayback("fastForwardHardwareButton"),
            R.array.settings_search_keywords_fast_forward_hardware,
        ),
        SearchableSetting(
            "rewindHardwareButton",
            R.string.settings_playback_rw_hardware,
            R.string.settings_playback_rw_hardware_desc,
            NavDestination.SettingsPlayback("rewindHardwareButton"),
            R.array.settings_search_keywords_rewind_hardware,
        ),
        SearchableSetting(
            "autoPlayNext",
            R.string.settings_playback_autoplay_next,
            R.string.settings_playback_autoplay_next_desc,
            NavDestination.SettingsPlayback("autoPlayNext"),
            R.array.settings_search_keywords_auto_play_next,
        ),
        SearchableSetting(
            "playChimeAndTitle",
            R.string.settings_playback_play_chime,
            R.string.settings_playback_play_chime_desc,
            NavDestination.SettingsPlayback("playChimeAndTitle"),
            R.array.settings_search_keywords_play_chime,
        ),
        SearchableSetting(
            "chimeSound",
            R.string.settings_playback_chime_sound,
            R.string.settings_playback_chime_sound_desc,
            NavDestination.SettingsPlayback("chimeSound"),
            R.array.settings_search_keywords_chime_sound,
        ),
        // Voices
        SearchableSetting(
            "engineSelection",
            R.string.settings_voices_engine_selection,
            R.string.settings_voices_engine_selection_desc,
            NavDestination.SettingsVoices("engineSelection"),
            R.array.settings_search_keywords_engine_selection,
        ),
        SearchableSetting(
            "androidTtsSettings",
            R.string.settings_voices_android_settings,
            R.string.settings_voices_android_settings_desc,
            NavDestination.SettingsVoices("androidTtsSettings"),
            R.array.settings_search_keywords_android_tts,
        ),
        SearchableSetting(
            "noiseScale",
            R.string.settings_voices_noise_scale_title,
            R.string.settings_voices_noise_scale_desc,
            NavDestination.SettingsVoices("noiseScale"),
            R.array.settings_search_keywords_noise_scale,
        ),
        SearchableSetting(
            "lengthScale",
            R.string.settings_voices_length_scale_title,
            R.string.settings_voices_length_scale_desc,
            NavDestination.SettingsVoices("lengthScale"),
            R.array.settings_search_keywords_length_scale,
        ),
        SearchableSetting(
            "kokoroVoice",
            R.string.settings_voices_kokoro_voice,
            R.string.settings_voices_kokoro_voice_desc,
            NavDestination.SettingsVoices("kokoroVoice"),
            R.array.settings_search_keywords_kokoro_voice,
        ),
        SearchableSetting(
            "voiceData",
            R.string.settings_voices_voice_data,
            R.string.settings_voices_voice_data_desc,
            NavDestination.SettingsVoices("voiceData"),
            R.array.settings_search_keywords_voice_data,
        ),
        // Downloads
        SearchableSetting(
            "downloadOverWifiOnly",
            R.string.settings_downloads_wifi_only,
            R.string.settings_downloads_wifi_only_desc,
            NavDestination.SettingsDownloads("downloadOverWifiOnly"),
            R.array.settings_search_keywords_wifi_only,
        ),
        SearchableSetting(
            "refreshInterval",
            R.string.settings_downloads_refresh_inbox,
            R.string.settings_downloads_refresh_inbox_desc,
            NavDestination.SettingsDownloads("refreshInterval"),
            R.array.settings_search_keywords_refresh_interval,
        ),
        SearchableSetting(
            "exportDatabase",
            R.string.settings_downloads_export_db,
            R.string.settings_downloads_export_db_desc,
            NavDestination.SettingsDownloads("exportDatabase"),
            R.array.settings_search_keywords_export_database,
        ),
        SearchableSetting(
            "importDatabase",
            R.string.settings_downloads_import_db,
            R.string.settings_downloads_import_db_desc,
            NavDestination.SettingsDownloads("importDatabase"),
            R.array.settings_search_keywords_import_database,
        ),
        SearchableSetting(
            "autoExportDatabase",
            R.string.settings_downloads_auto_export,
            R.string.settings_downloads_auto_export_desc,
            NavDestination.SettingsDownloads("autoExportDatabase"),
            R.array.settings_search_keywords_auto_export,
        ),
        SearchableSetting(
            "autoImportDatabase",
            R.string.settings_downloads_auto_import,
            R.string.settings_downloads_auto_import_desc,
            NavDestination.SettingsDownloads("autoImportDatabase"),
            R.array.settings_search_keywords_auto_import,
        ),
        SearchableSetting(
            "autoExportLocation",
            R.string.settings_downloads_auto_export_location,
            R.string.settings_downloads_auto_export_location,
            NavDestination.SettingsDownloads("autoExportLocation"),
            R.array.settings_search_keywords_auto_export_location,
        ),
        SearchableSetting(
            "deleteDatabase",
            R.string.settings_downloads_delete_db,
            R.string.settings_downloads_delete_db_desc,
            NavDestination.SettingsDownloads("deleteDatabase"),
            R.array.settings_search_keywords_delete_database,
        ),
        SearchableSetting(
            "importFeeds",
            R.string.settings_downloads_import_feeds,
            R.string.settings_downloads_import_feeds_desc,
            NavDestination.SettingsDownloads("importFeeds"),
            R.array.settings_search_keywords_import_feeds,
        ),
        SearchableSetting(
            "exportFeeds",
            R.string.settings_downloads_export_feeds,
            R.string.settings_downloads_export_feeds_desc,
            NavDestination.SettingsDownloads("exportFeeds"),
            R.array.settings_search_keywords_export_feeds,
        ),
    )
