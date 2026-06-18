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

import androidx.annotation.StringRes
import com.mienaiknife.narra.NavDestination
import com.mienaiknife.narra.R

data class SearchableSetting(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val destination: NavDestination,
    val keywords: List<String> = emptyList(),
)

val allSearchableSettings =
    listOf(
        // User Interface
        SearchableSetting(
            "useSystemTheme",
            R.string.settings_ui_use_system_theme,
            R.string.settings_ui_use_system_theme_desc,
            NavDestination.SettingsUi("useSystemTheme"),
            listOf("appearance", "dark mode", "light mode"),
        ),
        SearchableSetting(
            "darkMode",
            R.string.settings_ui_dark_mode,
            R.string.settings_ui_dark_mode_desc,
            NavDestination.SettingsUi("darkMode"),
            listOf("appearance", "theme"),
        ),
        SearchableSetting(
            "dynamicColors",
            R.string.settings_ui_dynamic_colors,
            R.string.settings_ui_dynamic_colors_desc,
            NavDestination.SettingsUi("dynamicColors"),
            listOf("appearance", "theme", "material you"),
        ),
        SearchableSetting(
            "readerFontFamily",
            R.string.settings_ui_reader_font_family,
            R.string.settings_ui_reader_font_family_desc,
            NavDestination.SettingsUi("readerFontFamily"),
            listOf("reader", "typography"),
        ),
        SearchableSetting(
            "tapToShowControls",
            R.string.settings_ui_tap_to_show,
            R.string.settings_ui_tap_to_show_desc,
            NavDestination.SettingsUi("tapToShowControls"),
            listOf("reader", "fullscreen"),
        ),
        SearchableSetting(
            "readerFontSize",
            R.string.settings_ui_reader_font_size,
            R.string.settings_ui_reader_font_size_desc,
            NavDestination.SettingsUi("readerFontSize"),
            listOf("reader", "text size", "typography"),
        ),
        SearchableSetting(
            "showRemainingTime",
            R.string.settings_ui_show_remaining_time,
            R.string.settings_ui_show_remaining_time_desc,
            NavDestination.SettingsUi("showRemainingTime"),
            listOf("playback", "reader"),
        ),
        // Playback
        SearchableSetting(
            "pauseOnDisconnect",
            R.string.settings_playback_pause_on_disconnect,
            R.string.settings_playback_pause_on_disconnect_desc,
            NavDestination.SettingsPlayback("pauseOnDisconnect"),
            listOf("headphones", "bluetooth"),
        ),
        SearchableSetting(
            "pauseForInterruptions",
            R.string.settings_playback_pause_for_interruptions,
            R.string.settings_playback_pause_for_interruptions_desc,
            NavDestination.SettingsPlayback("pauseForInterruptions"),
            listOf("notifications", "audio focus"),
        ),
        SearchableSetting(
            "fastForwardSkipTime",
            R.string.settings_playback_ff_skip,
            R.string.settings_playback_ff_skip_desc,
            NavDestination.SettingsPlayback("fastForwardSkipTime"),
            listOf("controls", "skip"),
        ),
        SearchableSetting(
            "rewindSkipTime",
            R.string.settings_playback_rw_skip,
            R.string.settings_playback_rw_skip_desc,
            NavDestination.SettingsPlayback("rewindSkipTime"),
            listOf("controls", "skip"),
        ),
        SearchableSetting(
            "fastForwardHardwareButton",
            R.string.settings_playback_ff_hardware,
            R.string.settings_playback_ff_hardware_desc,
            NavDestination.SettingsPlayback("fastForwardHardwareButton"),
            listOf("controls", "hardware", "remote"),
        ),
        SearchableSetting(
            "rewindHardwareButton",
            R.string.settings_playback_rw_hardware,
            R.string.settings_playback_rw_hardware_desc,
            NavDestination.SettingsPlayback("rewindHardwareButton"),
            listOf("controls", "hardware", "remote"),
        ),
        SearchableSetting(
            "autoPlayNext",
            R.string.settings_playback_autoplay_next,
            R.string.settings_playback_autoplay_next_desc,
            NavDestination.SettingsPlayback("autoPlayNext"),
            listOf("queue", "continuous"),
        ),
        SearchableSetting(
            "playChimeAndTitle",
            R.string.settings_playback_play_chime,
            R.string.settings_playback_play_chime_desc,
            NavDestination.SettingsPlayback("playChimeAndTitle"),
            listOf("queue", "announcement"),
        ),
        SearchableSetting(
            "chimeSound",
            R.string.settings_playback_chime_sound,
            R.string.settings_playback_chime_sound_desc,
            NavDestination.SettingsPlayback("chimeSound"),
            listOf("queue", "notification"),
        ),
        // Voices
        SearchableSetting(
            "engineSelection",
            R.string.settings_voices_engine_selection,
            R.string.settings_voices_engine_selection_desc,
            NavDestination.SettingsVoices("engineSelection"),
            listOf("tts", "sherpa", "onnx"),
        ),
        SearchableSetting(
            "androidTtsSettings",
            R.string.settings_voices_android_settings,
            R.string.settings_voices_android_settings_desc,
            NavDestination.SettingsVoices("androidTtsSettings"),
            listOf("tts", "system"),
        ),
        SearchableSetting(
            "noiseScale",
            R.string.settings_voices_noise_scale_title,
            R.string.settings_voices_noise_scale_desc,
            NavDestination.SettingsVoices("noiseScale"),
            listOf("tts", "ai", "sherpa"),
        ),
        SearchableSetting(
            "lengthScale",
            R.string.settings_voices_length_scale_title,
            R.string.settings_voices_length_scale_desc,
            NavDestination.SettingsVoices("lengthScale"),
            listOf("tts", "ai", "sherpa"),
        ),
        SearchableSetting(
            "kokoroVoice",
            R.string.settings_voices_kokoro_voice,
            R.string.settings_voices_kokoro_voice_desc,
            NavDestination.SettingsVoices("kokoroVoice"),
            listOf("tts", "ai", "kokoro"),
        ),
        SearchableSetting(
            "voiceData",
            R.string.settings_voices_voice_data,
            R.string.settings_voices_voice_data_desc,
            NavDestination.SettingsVoices("voiceData"),
            listOf("tts", "models", "download"),
        ),
        // Downloads
        SearchableSetting(
            "downloadOverWifiOnly",
            R.string.settings_downloads_wifi_only,
            R.string.settings_downloads_wifi_only_desc,
            NavDestination.SettingsDownloads("downloadOverWifiOnly"),
            listOf("network", "data"),
        ),
        SearchableSetting(
            "refreshInterval",
            R.string.settings_downloads_refresh_inbox,
            R.string.settings_downloads_refresh_inbox_desc,
            NavDestination.SettingsDownloads("refreshInterval"),
            listOf("automation", "feeds"),
        ),
        SearchableSetting(
            "exportDatabase",
            R.string.settings_downloads_export_db,
            R.string.settings_downloads_export_db_desc,
            NavDestination.SettingsDownloads("exportDatabase"),
            listOf("backup", "sync"),
        ),
        SearchableSetting(
            "importDatabase",
            R.string.settings_downloads_import_db,
            R.string.settings_downloads_import_db_desc,
            NavDestination.SettingsDownloads("importDatabase"),
            listOf("backup", "sync"),
        ),
        SearchableSetting(
            "autoExportDatabase",
            R.string.settings_downloads_auto_export,
            R.string.settings_downloads_auto_export_desc,
            NavDestination.SettingsDownloads("autoExportDatabase"),
            listOf("backup", "sync", "syncthing"),
        ),
        SearchableSetting(
            "autoImportDatabase",
            R.string.settings_downloads_auto_import,
            R.string.settings_downloads_auto_import_desc,
            NavDestination.SettingsDownloads("autoImportDatabase"),
            listOf("backup", "sync", "syncthing"),
        ),
        SearchableSetting(
            "autoExportLocation",
            R.string.settings_downloads_auto_export_location,
            R.string.settings_downloads_auto_export_location,
            NavDestination.SettingsDownloads("autoExportLocation"),
            listOf("backup", "sync"),
        ),
        SearchableSetting(
            "deleteDatabase",
            R.string.settings_downloads_delete_db,
            R.string.settings_downloads_delete_db_desc,
            NavDestination.SettingsDownloads("deleteDatabase"),
            listOf("data", "reset", "clear"),
        ),
        SearchableSetting(
            "importFeeds",
            R.string.settings_downloads_import_feeds,
            R.string.settings_downloads_import_feeds_desc,
            NavDestination.SettingsDownloads("importFeeds"),
            listOf("opml", "rss"),
        ),
        SearchableSetting(
            "exportFeeds",
            R.string.settings_downloads_export_feeds,
            R.string.settings_downloads_export_feeds_desc,
            NavDestination.SettingsDownloads("exportFeeds"),
            listOf("opml", "rss"),
        ),
    )
