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

package com.mienaiknife.narra

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination {
    @Serializable
    data object Home : NavDestination

    @Serializable
    data object Queue : NavDestination

    @Serializable
    data object History : NavDestination

    @Serializable
    data object Add : NavDestination

    @Serializable
    data object Inbox : NavDestination

    @Serializable
    data object Feeds : NavDestination

    @Serializable
    data class Feed(val feedTitle: String) : NavDestination

    @Serializable
    data object Settings : NavDestination

    @Serializable
    data object SettingsUi : NavDestination

    @Serializable
    data object SettingsPlayback : NavDestination

    @Serializable
    data object SettingsVoices : NavDestination

    @Serializable
    data object SettingsDownloads : NavDestination

    @Serializable
    data object SettingsAbout : NavDestination

    @Serializable
    data object SettingsLicenses : NavDestination

    @Serializable
    data class Reader(val articleId: String) : NavDestination
}
