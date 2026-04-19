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

package com.mienaiknife.narra.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mienaiknife.narra.ui.theme.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val downloadOverWifiOnlyKey = booleanPreferencesKey("download_over_wifi_only")

    val downloadOverWifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[downloadOverWifiOnlyKey] ?: true
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[downloadOverWifiOnlyKey] = enabled
        }
    }
}
