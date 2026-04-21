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
    @param:ApplicationContext private val context: Context
) {
    private val chimeSoundKey = stringPreferencesKey("chime_sound")
    private val fastForwardSkipTimeKey = stringPreferencesKey("fast_forward_skip_time")
    private val rewindSkipTimeKey = stringPreferencesKey("rewind_skip_time")
    private val pauseOnDisconnectKey = androidx.datastore.preferences.core.booleanPreferencesKey("pause_on_disconnect")
    private val pauseForInterruptionsKey = androidx.datastore.preferences.core.booleanPreferencesKey("pause_for_interruptions")
    private val autoPlayNextKey = androidx.datastore.preferences.core.booleanPreferencesKey("auto_play_next")
    private val lastArticleIdKey = stringPreferencesKey("last_article_id")
    private val ttsEngineKey = stringPreferencesKey("tts_engine")
    private val ttsModelIdKey = stringPreferencesKey("tts_model_id")
    private val ttsSpeakerIdKey = androidx.datastore.preferences.core.intPreferencesKey("tts_speaker_id")
    private val sherpaSpeedKey = androidx.datastore.preferences.core.floatPreferencesKey("sherpa_speed")
    private val sherpaNoiseScaleKey = androidx.datastore.preferences.core.floatPreferencesKey("sherpa_noise_scale")
    private val sherpaLengthScaleKey = androidx.datastore.preferences.core.floatPreferencesKey("sherpa_length_scale")

    val chimeSound: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[chimeSoundKey] ?: "music_box_chime_positive"
    }

    val fastForwardSkipTime: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[fastForwardSkipTimeKey] ?: "30s"
    }

    val rewindSkipTime: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[rewindSkipTimeKey] ?: "10s"
    }

    val pauseOnDisconnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pauseOnDisconnectKey] ?: true
    }

    val pauseForInterruptions: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pauseForInterruptionsKey] ?: true
    }

    val autoPlayNext: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoPlayNextKey] ?: true
    }

    val lastArticleId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[lastArticleIdKey]
    }

    val ttsEngine: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ttsEngineKey] ?: "android"
    }

    val ttsModelId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ttsModelIdKey]
    }

    val ttsSpeakerId: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ttsSpeakerIdKey] ?: 0
    }

    val sherpaSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[sherpaSpeedKey] ?: 1.0f
    }

    val sherpaNoiseScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[sherpaNoiseScaleKey] ?: 0.667f
    }

    val sherpaLengthScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[sherpaLengthScaleKey] ?: 1.0f
    }

    suspend fun setFastForwardSkipTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[fastForwardSkipTimeKey] = time
        }
    }

    suspend fun setRewindSkipTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[rewindSkipTimeKey] = time
        }
    }

    suspend fun setPauseOnDisconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[pauseOnDisconnectKey] = enabled
        }
    }

    suspend fun setPauseForInterruptions(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[pauseForInterruptionsKey] = enabled
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoPlayNextKey] = enabled
        }
    }

    suspend fun setLastArticleId(articleId: String?) {
        context.dataStore.edit { prefs ->
            if (articleId == null) {
                prefs.remove(lastArticleIdKey)
            } else {
                prefs[lastArticleIdKey] = articleId
            }
        }
    }

    suspend fun setTtsEngine(engine: String) {
        context.dataStore.edit { prefs ->
            prefs[ttsEngineKey] = engine
        }
    }

    suspend fun setTtsModelId(modelId: String?) {
        context.dataStore.edit { prefs ->
            if (modelId == null) {
                prefs.remove(ttsModelIdKey)
            } else {
                prefs[ttsModelIdKey] = modelId
            }
        }
    }

    suspend fun setTtsSpeakerId(speakerId: Int) {
        context.dataStore.edit { prefs ->
            prefs[ttsSpeakerIdKey] = speakerId
        }
    }

    suspend fun setSherpaSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[sherpaSpeedKey] = speed
        }
    }

    suspend fun setSherpaNoiseScale(noiseScale: Float) {
        context.dataStore.edit { prefs ->
            prefs[sherpaNoiseScaleKey] = noiseScale
        }
    }

    suspend fun setSherpaLengthScale(lengthScale: Float) {
        context.dataStore.edit { prefs ->
            prefs[sherpaLengthScaleKey] = lengthScale
        }
    }
}
