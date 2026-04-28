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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mienaiknife.narra.ui.theme.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val autoExportEnabledKey = booleanPreferencesKey("auto_export_enabled")
    private val autoExportUriKey = stringPreferencesKey("auto_export_uri")
    private val lastExportTimestampKey = longPreferencesKey("last_export_timestamp")
    private val autoImportEnabledKey = booleanPreferencesKey("auto_import_enabled")
    private val pendingImportKey = booleanPreferencesKey("pending_import")
    private val remoteLastModifiedKey = longPreferencesKey("remote_last_modified")

    val autoExportEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoExportEnabledKey] ?: false
    }

    val autoExportUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[autoExportUriKey]
    }

    val lastExportTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[lastExportTimestampKey] ?: 0L
    }

    val autoImportEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoImportEnabledKey] ?: false
    }

    val pendingImport: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pendingImportKey] ?: false
    }

    val remoteLastModified: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[remoteLastModifiedKey] ?: 0L
    }

    suspend fun setAutoExportEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoExportEnabledKey] = enabled
        }
    }

    suspend fun setAutoExportUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(autoExportUriKey)
            } else {
                prefs[autoExportUriKey] = uri
            }
        }
    }

    suspend fun setAutoImportEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoImportEnabledKey] = enabled
        }
    }

    suspend fun setPendingImport(pending: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[pendingImportKey] = pending
        }
    }

    suspend fun setRemoteLastModified(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[remoteLastModifiedKey] = timestamp
        }
    }

    suspend fun updateLastExportTimestamp() {
        context.dataStore.edit { prefs ->
            prefs[lastExportTimestampKey] = System.currentTimeMillis()
        }
    }
}
