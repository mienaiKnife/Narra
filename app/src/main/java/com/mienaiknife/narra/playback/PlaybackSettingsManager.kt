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

package com.mienaiknife.narra.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mienaiknife.narra.ui.theme.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val chimeSoundKey = stringPreferencesKey("chime_sound")

    val chimeSound: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[chimeSoundKey] ?: "music_box_chime_positive"
    }

    suspend fun setChimeSound(soundName: String) {
        context.dataStore.edit { prefs ->
            prefs[chimeSoundKey] = soundName
        }
    }
}
