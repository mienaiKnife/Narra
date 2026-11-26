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
package com.mienaiknife.narra.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SherpaVoiceSettingsTest {
    @Test
    fun `vits supports every tuning knob`() {
        val capabilities = TtsModelType.VITS.voiceCapabilities

        assertTrue(capabilities.noiseScale)
        assertTrue(capabilities.noiseScaleW)
        assertTrue(capabilities.lengthScale)
        assertTrue(capabilities.pauseScale)
    }

    @Test
    fun `kokoro and kitten only expose length and pause`() {
        listOf(TtsModelType.KOKORO, TtsModelType.KITTEN).forEach { type ->
            val capabilities = type.voiceCapabilities
            assertFalse("$type should hide noise", capabilities.noiseScale)
            assertFalse("$type should hide noise W", capabilities.noiseScaleW)
            assertTrue("$type should expose length", capabilities.lengthScale)
            assertTrue("$type should expose pause", capabilities.pauseScale)
        }
    }

    @Test
    fun `supertonic exposes no native tuning knobs`() {
        val capabilities = TtsModelType.SUPERTONIC.voiceCapabilities

        assertFalse(capabilities.noiseScale)
        assertFalse(capabilities.noiseScaleW)
        assertFalse(capabilities.lengthScale)
        assertFalse(capabilities.pauseScale)
    }

    @Test
    fun `matcha keeps its upstream noise default`() {
        assertEquals(1.0f, TtsModelType.MATCHA.defaultVoiceParams().noiseScale, 0.001f)
        assertEquals(0.667f, TtsModelType.VITS.defaultVoiceParams().noiseScale, 0.001f)
    }

    @Test
    fun `overrides are clamped to the supported ranges`() {
        val params = SherpaVoiceOverrides(
            noiseScale = 9f,
            lengthScale = 0f,
            pauseScale = 99f,
            extraPauseMs = 5000,
            cpuThreads = 0,
            speakerId = -3,
        ).applyTo(TtsModelType.VITS.defaultVoiceParams())

        assertEquals(SherpaVoiceRanges.NOISE_SCALE.endInclusive, params.noiseScale, 0.001f)
        assertEquals(SherpaVoiceRanges.LENGTH_SCALE.start, params.lengthScale, 0.001f)
        assertEquals(SherpaVoiceRanges.PAUSE_SCALE.endInclusive, params.pauseScale, 0.001f)
        assertEquals(SherpaVoiceRanges.EXTRA_PAUSE_MS.endInclusive, params.extraPauseMs)
        assertEquals(SherpaVoiceRanges.CPU_THREADS.start, params.cpuThreads)
        assertEquals(0, params.speakerId)
    }

    @Test
    fun `render key ignores hidden and non-native settings`() {
        val defaults = TtsModelType.KOKORO.defaultVoiceParams()
        val tuned = defaults.copy(
            noiseScale = 1.4f,
            noiseScaleW = 1.3f,
            lengthScale = 1.4f,
            extraPauseMs = 800,
            speakerId = 7,
        )

        // Kokoro reads lengthScale but ignores noise, extra pause and speaker.
        val kokoroKey = tuned.renderKeyFor(TtsModelType.KOKORO)
        assertEquals(defaults.noiseScale, kokoroKey.noiseScale, 0.001f)
        assertEquals(defaults.noiseScaleW, kokoroKey.noiseScaleW, 0.001f)
        assertEquals(1.4f, kokoroKey.lengthScale, 0.001f)
        assertEquals(0, kokoroKey.extraPauseMs)
        assertEquals(0, kokoroKey.speakerId)

        // VITS reads the noise knobs, so the same values must produce a different key.
        val vitsKey = tuned.renderKeyFor(TtsModelType.VITS)
        assertEquals(1.4f, vitsKey.noiseScale, 0.001f)
        assertEquals(1.3f, vitsKey.noiseScaleW, 0.001f)
    }
}
