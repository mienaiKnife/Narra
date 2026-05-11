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

import com.mienaiknife.narra.NavDestination

data class SearchableSetting(
    val id: String,
    val title: String,
    val subtitle: String,
    val destination: NavDestination,
    val keywords: List<String> = emptyList()
)

val allSearchableSettings = listOf(
    // User Interface
    SearchableSetting("useSystemTheme", "Use system theme", "Make Narra match your system theme", NavDestination.SettingsUi("useSystemTheme"), listOf("appearance", "dark mode", "light mode")),
    SearchableSetting("darkMode", "Dark mode", "Make Narra always use dark mode", NavDestination.SettingsUi("darkMode"), listOf("appearance", "theme")),
    SearchableSetting("dynamicColors", "Dynamic colors", "Make the theme match your wallpaper", NavDestination.SettingsUi("dynamicColors"), listOf("appearance", "theme", "material you")),
    SearchableSetting("readerFontFamily", "Font family", "Choose the font used for article content", NavDestination.SettingsUi("readerFontFamily"), listOf("reader", "typography")),
    SearchableSetting("tapToShowControls", "Tap to show controls", "Tapping anywhere will show the controls", NavDestination.SettingsUi("tapToShowControls"), listOf("reader", "fullscreen")),
    SearchableSetting("readerFontSize", "Font size", "Adjust the text size for reading articles", NavDestination.SettingsUi("readerFontSize"), listOf("reader", "text size", "typography")),
    SearchableSetting("showRemainingTime", "Show remaining time", "Toggle between remaining time or total duration", NavDestination.SettingsUi("showRemainingTime"), listOf("playback", "reader")),

    // Playback
    SearchableSetting("pauseOnDisconnect", "Pause on disconnect", "Pause playback when headphones are disconnected", NavDestination.SettingsPlayback("pauseOnDisconnect"), listOf("headphones", "bluetooth")),
    SearchableSetting("pauseForInterruptions", "Pause for interruptions", "Pause instead of lowering volume for notifications", NavDestination.SettingsPlayback("pauseForInterruptions"), listOf("notifications", "audio focus")),
    SearchableSetting("fastForwardSkipTime", "Fast forward skip time", "Customize fast forward button skip time", NavDestination.SettingsPlayback("fastForwardSkipTime"), listOf("controls", "skip")),
    SearchableSetting("rewindSkipTime", "Rewind skip time", "Customize rewind button skip time", NavDestination.SettingsPlayback("rewindSkipTime"), listOf("controls", "skip")),
    SearchableSetting("fastForwardHardwareButton", "Fast forward hardware button", "Customize fast forward hardware button behavior", NavDestination.SettingsPlayback("fastForwardHardwareButton"), listOf("controls", "hardware", "remote")),
    SearchableSetting("rewindHardwareButton", "Rewind hardware button", "Customize rewind hardware button behavior", NavDestination.SettingsPlayback("rewindHardwareButton"), listOf("controls", "hardware", "remote")),
    SearchableSetting("autoPlayNext", "Autoplay next text", "Automatically play the next text in the queue", NavDestination.SettingsPlayback("autoPlayNext"), listOf("queue", "continuous")),
    SearchableSetting("playChimeAndTitle", "Play chime and title", "Play a chime and title before next text", NavDestination.SettingsPlayback("playChimeAndTitle"), listOf("queue", "announcement")),
    SearchableSetting("chimeSound", "Chime sound", "Choose the sound to play before the title", NavDestination.SettingsPlayback("chimeSound"), listOf("queue", "notification")),

    // Voices
    SearchableSetting("engineSelection", "Engine selection", "Choose between Android TTS and on-device AI", NavDestination.SettingsVoices("engineSelection"), listOf("tts", "sherpa", "onnx")),
    SearchableSetting("androidTtsSettings", "Android's TTS settings", "Open system text-to-speech settings", NavDestination.SettingsVoices("androidTtsSettings"), listOf("tts", "system")),
    SearchableSetting("noiseScale", "Noise Scale", "Adjust expressiveness for AI voices", NavDestination.SettingsVoices("noiseScale"), listOf("tts", "ai", "sherpa")),
    SearchableSetting("lengthScale", "Length Scale", "Adjust playback speed for AI voices", NavDestination.SettingsVoices("lengthScale"), listOf("tts", "ai", "sherpa")),
    SearchableSetting("kokoroVoice", "Kokoro Voice", "Select specific Kokoro AI voice", NavDestination.SettingsVoices("kokoroVoice"), listOf("tts", "ai", "kokoro")),
    SearchableSetting("voiceData", "Voice data", "Download or delete TTS models", NavDestination.SettingsVoices("voiceData"), listOf("tts", "models", "download")),

    // Downloads
    SearchableSetting("downloadOverWifiOnly", "Download over Wi-Fi only", "Prevent Narra from using mobile data", NavDestination.SettingsDownloads("downloadOverWifiOnly"), listOf("network", "data")),
    SearchableSetting("refreshInterval", "Refresh inbox", "Automatic inbox refresh interval", NavDestination.SettingsDownloads("refreshInterval"), listOf("automation", "feeds")),
    SearchableSetting("exportDatabase", "Export database", "Back up your data", NavDestination.SettingsDownloads("exportDatabase"), listOf("backup", "sync")),
    SearchableSetting("importDatabase", "Import database", "Restore your data from backup", NavDestination.SettingsDownloads("importDatabase"), listOf("backup", "sync")),
    SearchableSetting("autoExportDatabase", "Auto-export database", "Enable automatic database export", NavDestination.SettingsDownloads("autoExportDatabase"), listOf("backup", "sync", "syncthing")),
    SearchableSetting("autoImportDatabase", "Auto-import database", "Enable automatic database import", NavDestination.SettingsDownloads("autoImportDatabase"), listOf("backup", "sync", "syncthing")),
    SearchableSetting("autoExportLocation", "Auto-export location", "Choose where to auto-export data", NavDestination.SettingsDownloads("autoExportLocation"), listOf("backup", "sync")),
    SearchableSetting("deleteDatabase", "Delete database", "Delete all your data", NavDestination.SettingsDownloads("deleteDatabase"), listOf("data", "reset", "clear")),
    SearchableSetting("importFeeds", "Import feeds", "Import subscriptions from OPML", NavDestination.SettingsDownloads("importFeeds"), listOf("opml", "rss")),
    SearchableSetting("exportFeeds", "Export feeds", "Export subscriptions to OPML", NavDestination.SettingsDownloads("exportFeeds"), listOf("opml", "rss"))
)
