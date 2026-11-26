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

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSettingsManagerTest {
    @Test
    fun `parses values per model and ignores unrelated keys`() {
        val prefs = mutablePreferencesOf(
            floatPreferencesKey("sherpa_voice_noise_scale:model-a") to 0.4f,
            floatPreferencesKey("sherpa_voice_length_scale:model-a") to 1.4f,
            intPreferencesKey("sherpa_voice_extra_pause_ms:model-a") to 200,
            intPreferencesKey("sherpa_voice_speaker_id:model-b") to 5,
            booleanPreferencesKey("sherpa_voice_settings_migrated") to true,
            floatPreferencesKey("sherpa_noise_scale") to 0.9f,
            stringPreferencesKey("some_other_setting") to "value",
        )

        val overrides = parseVoiceOverrides(prefs)

        assertEquals(setOf("model-a", "model-b"), overrides.keys)
        assertEquals(0.4f, overrides.getValue("model-a").noiseScale)
        assertEquals(1.4f, overrides.getValue("model-a").lengthScale)
        assertEquals(200, overrides.getValue("model-a").extraPauseMs)
        assertNull(overrides.getValue("model-a").noiseScaleW)
        assertEquals(5, overrides.getValue("model-b").speakerId)
        assertNull(overrides.getValue("model-b").noiseScale)
    }

    @Test
    fun `malformed or unknown voice keys are ignored`() {
        val prefs = mutablePreferencesOf(
            floatPreferencesKey("sherpa_voice_noise_scale") to 0.4f,
            floatPreferencesKey("sherpa_voice_mystery_field:model-a") to 0.4f,
            floatPreferencesKey("sherpa_voice_noise_scale:") to 0.4f,
            floatPreferencesKey("sherpa_voice_noise_scale:model-a") to 0.5f,
        )

        val overrides = parseVoiceOverrides(prefs)

        assertEquals(1, overrides.size)
        assertEquals(0.5f, overrides.getValue("model-a").noiseScale)
    }

    @Test
    fun `empty preferences produce no overrides`() {
        assertTrue(parseVoiceOverrides(mutablePreferencesOf()).isEmpty())
    }
}
