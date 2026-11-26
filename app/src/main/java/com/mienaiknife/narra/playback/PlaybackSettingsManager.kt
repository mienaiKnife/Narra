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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mienaiknife.narra.data.settings.settingsDataStore
import com.mienaiknife.narra.domain.models.SherpaVoiceOverrides
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsManager
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val chimeSoundKey = stringPreferencesKey("chime_sound")
    private val fastForwardSkipTimeKey = stringPreferencesKey("fast_forward_skip_time")
    private val rewindSkipTimeKey = stringPreferencesKey("rewind_skip_time")
    private val fastForwardHardwareButtonKey = stringPreferencesKey("fast_forward_hardware_button")
    private val rewindHardwareButtonKey = stringPreferencesKey("rewind_hardware_button")
    private val pauseOnDisconnectKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("pause_on_disconnect")
    private val pauseForInterruptionsKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("pause_for_interruptions")
    private val autoPlayNextKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("auto_play_next")
    private val playChimeAndTitleKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("play_chime_and_title")
    private val readAltTextKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("read_alt_text")
    private val shortenHyperlinksKey =
        androidx.datastore.preferences.core
            .booleanPreferencesKey("shorten_hyperlinks")
    private val lastArticleIdKey = stringPreferencesKey("last_article_id")
    private val ttsEngineKey = stringPreferencesKey("tts_engine")
    private val ttsModelIdKey = stringPreferencesKey("tts_model_id")
    private val ttsSpeakerIdKey =
        androidx.datastore.preferences.core
            .intPreferencesKey("tts_speaker_id")
    private val sherpaNoiseScaleKey =
        androidx.datastore.preferences.core
            .floatPreferencesKey("sherpa_noise_scale")
    private val sherpaLengthScaleKey =
        androidx.datastore.preferences.core
            .floatPreferencesKey("sherpa_length_scale")
    private val voiceSettingsMigratedKey = booleanPreferencesKey("sherpa_voice_settings_migrated")

    private fun voiceOverrideKey(
        field: String,
        modelId: String,
    ) = "${VOICE_OVERRIDE_PREFIX}${field}${VOICE_OVERRIDE_SEPARATOR}$modelId"

    private fun noiseScaleKey(modelId: String) = floatPreferencesKey(voiceOverrideKey(FIELD_NOISE_SCALE, modelId))

    private fun noiseScaleWKey(modelId: String) = floatPreferencesKey(voiceOverrideKey(FIELD_NOISE_SCALE_W, modelId))

    private fun lengthScaleKey(modelId: String) = floatPreferencesKey(voiceOverrideKey(FIELD_LENGTH_SCALE, modelId))

    private fun pauseScaleKey(modelId: String) = floatPreferencesKey(voiceOverrideKey(FIELD_PAUSE_SCALE, modelId))

    private fun extraPauseKey(modelId: String) = intPreferencesKey(voiceOverrideKey(FIELD_EXTRA_PAUSE, modelId))

    private fun cpuThreadsKey(modelId: String) = intPreferencesKey(voiceOverrideKey(FIELD_CPU_THREADS, modelId))

    private fun speakerKey(modelId: String) = intPreferencesKey(voiceOverrideKey(FIELD_SPEAKER, modelId))

    private fun voiceOverrideKeys(modelId: String): List<Preferences.Key<*>> = listOf(
        noiseScaleKey(modelId),
        noiseScaleWKey(modelId),
        lengthScaleKey(modelId),
        pauseScaleKey(modelId),
        extraPauseKey(modelId),
        cpuThreadsKey(modelId),
        speakerKey(modelId),
    )

    val chimeSound: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[chimeSoundKey] ?: "music_box_chime_positive"
        }

    val fastForwardSkipTime: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[fastForwardSkipTimeKey] ?: "30s"
        }

    val rewindSkipTime: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[rewindSkipTimeKey] ?: "10s"
        }

    val fastForwardHardwareButton: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[fastForwardHardwareButtonKey] ?: "fast_forward"
        }

    val rewindHardwareButton: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[rewindHardwareButtonKey] ?: "rewind"
        }

    val pauseOnDisconnect: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[pauseOnDisconnectKey] ?: true
        }

    val pauseForInterruptions: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[pauseForInterruptionsKey] ?: true
        }

    val autoPlayNext: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[autoPlayNextKey] ?: true
        }

    val playChimeAndTitle: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[playChimeAndTitleKey] ?: true
        }

    val readAltText: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[readAltTextKey] ?: true
        }

    val shortenHyperlinks: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[shortenHyperlinksKey] ?: true
        }

    val lastArticleId: Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[lastArticleIdKey]
        }

    val ttsEngine: Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[ttsEngineKey] ?: "android"
        }

    val ttsModelId: Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[ttsModelIdKey]
        }

    /**
     * Voice tuning values for every model that has been explicitly customized. Models without an
     * entry fall back to their model-type defaults, so switching voices never carries one model's
     * tuning over to another.
     */
    val sherpaVoiceOverrides: Flow<Map<String, SherpaVoiceOverrides>> =
        context.settingsDataStore.data.map { prefs -> parseVoiceOverrides(prefs) }

    suspend fun setFastForwardSkipTime(time: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[fastForwardSkipTimeKey] = time
        }
    }

    suspend fun setRewindSkipTime(time: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[rewindSkipTimeKey] = time
        }
    }

    suspend fun setFastForwardHardwareButton(action: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[fastForwardHardwareButtonKey] = action
        }
    }

    suspend fun setRewindHardwareButton(action: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[rewindHardwareButtonKey] = action
        }
    }

    suspend fun setPauseOnDisconnect(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[pauseOnDisconnectKey] = enabled
        }
    }

    suspend fun setPauseForInterruptions(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[pauseForInterruptionsKey] = enabled
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[autoPlayNextKey] = enabled
        }
    }

    suspend fun setChimeSound(sound: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[chimeSoundKey] = sound
        }
    }

    suspend fun setPlayChimeAndTitle(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[playChimeAndTitleKey] = enabled
        }
    }

    suspend fun setReadAltText(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[readAltTextKey] = enabled
        }
    }

    suspend fun setShortenHyperlinks(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[shortenHyperlinksKey] = enabled
        }
    }

    suspend fun setLastArticleId(articleId: String?) {
        context.settingsDataStore.edit { prefs ->
            if (articleId == null) {
                prefs.remove(lastArticleIdKey)
            } else {
                prefs[lastArticleIdKey] = articleId
            }
        }
    }

    suspend fun setTtsEngine(engine: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[ttsEngineKey] = engine
        }
    }

    suspend fun setTtsModelId(modelId: String?) {
        context.settingsDataStore.edit { prefs ->
            if (modelId == null) {
                prefs.remove(ttsModelIdKey)
            } else {
                prefs[ttsModelIdKey] = modelId
            }
        }
    }

    /**
     * Copies the pre-existing global tuning into the currently selected model once, so an upgrade
     * keeps the user's sound instead of resetting it to the model defaults.
     */
    suspend fun migrateLegacyVoiceSettings() {
        context.settingsDataStore.edit { prefs ->
            if (prefs[voiceSettingsMigratedKey] == true) return@edit
            // Defer until a model is selected: the old values belong to whichever model is active.
            val modelId = prefs[ttsModelIdKey] ?: return@edit
            prefs[voiceSettingsMigratedKey] = true
            prefs[sherpaNoiseScaleKey]?.let { prefs[noiseScaleKey(modelId)] = it }
            prefs[sherpaLengthScaleKey]?.let { prefs[lengthScaleKey(modelId)] = it }
            prefs[ttsSpeakerIdKey]?.let { prefs[speakerKey(modelId)] = it.coerceAtLeast(0) }
        }
    }

    suspend fun setVoiceNoiseScale(
        modelId: String,
        value: Float,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[noiseScaleKey(modelId)] = value }
    }

    suspend fun setVoiceNoiseScaleW(
        modelId: String,
        value: Float,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[noiseScaleWKey(modelId)] = value }
    }

    suspend fun setVoiceLengthScale(
        modelId: String,
        value: Float,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[lengthScaleKey(modelId)] = value }
    }

    suspend fun setVoicePauseScale(
        modelId: String,
        value: Float,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[pauseScaleKey(modelId)] = value }
    }

    suspend fun setVoiceExtraPause(
        modelId: String,
        value: Int,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[extraPauseKey(modelId)] = value }
    }

    suspend fun setVoiceCpuThreads(
        modelId: String,
        value: Int,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[cpuThreadsKey(modelId)] = value }
    }

    suspend fun setVoiceSpeaker(
        modelId: String,
        value: Int,
    ) {
        context.settingsDataStore.edit { prefs -> prefs[speakerKey(modelId)] = value }
    }

    suspend fun resetVoiceParams(modelId: String) {
        context.settingsDataStore.edit { prefs ->
            voiceOverrideKeys(modelId).forEach { prefs.remove(it) }
        }
    }
}

internal fun parseVoiceOverrides(prefs: Preferences): Map<String, SherpaVoiceOverrides> {
    val builders = mutableMapOf<String, VoiceOverridesBuilder>()
    prefs.asMap().forEach { (key, value) ->
        val name = key.name
        if (!name.startsWith(VOICE_OVERRIDE_PREFIX)) return@forEach
        val payload = name.removePrefix(VOICE_OVERRIDE_PREFIX)
        val separator = payload.indexOf(VOICE_OVERRIDE_SEPARATOR)
        if (separator <= 0 || separator == payload.lastIndex) return@forEach
        val field = payload.substring(0, separator)
        val modelId = payload.substring(separator + 1)
        val builder = builders.getOrPut(modelId) { VoiceOverridesBuilder() }
        when (field) {
            FIELD_NOISE_SCALE -> builder.noiseScale = value as? Float
            FIELD_NOISE_SCALE_W -> builder.noiseScaleW = value as? Float
            FIELD_LENGTH_SCALE -> builder.lengthScale = value as? Float
            FIELD_PAUSE_SCALE -> builder.pauseScale = value as? Float
            FIELD_EXTRA_PAUSE -> builder.extraPauseMs = value as? Int
            FIELD_CPU_THREADS -> builder.cpuThreads = value as? Int
            FIELD_SPEAKER -> builder.speakerId = value as? Int
        }
    }
    return builders.mapValues { (_, builder) -> builder.build() }
}

private class VoiceOverridesBuilder {
    var noiseScale: Float? = null
    var noiseScaleW: Float? = null
    var lengthScale: Float? = null
    var pauseScale: Float? = null
    var extraPauseMs: Int? = null
    var cpuThreads: Int? = null
    var speakerId: Int? = null

    fun build() = SherpaVoiceOverrides(
        noiseScale = noiseScale,
        noiseScaleW = noiseScaleW,
        lengthScale = lengthScale,
        pauseScale = pauseScale,
        extraPauseMs = extraPauseMs,
        cpuThreads = cpuThreads,
        speakerId = speakerId,
    )
}

private const val VOICE_OVERRIDE_PREFIX = "sherpa_voice_"
private const val VOICE_OVERRIDE_SEPARATOR = ':'
private const val FIELD_NOISE_SCALE = "noise_scale"
private const val FIELD_NOISE_SCALE_W = "noise_scale_w"
private const val FIELD_LENGTH_SCALE = "length_scale"
private const val FIELD_PAUSE_SCALE = "pause_scale"
private const val FIELD_EXTRA_PAUSE = "extra_pause_ms"
private const val FIELD_CPU_THREADS = "cpu_threads"
private const val FIELD_SPEAKER = "speaker_id"
