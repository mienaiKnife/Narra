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
package com.mienaiknife.narra.data.local.entities

import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsModelEntityTest {
    @Test
    fun `extraUrls roundtrip handles colons in URLs`() {
        val originalExtraUrls =
            mapOf(
                "voices.bin" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/voices.bin",
                "config.json" to "http://example.com/config",
            )

        val model =
            TtsModel(
                id = "test-model",
                name = "Test Model",
                language = "en",
                type = TtsModelType.KOKORO,
                modelUrl = "https://example.com/model.tar.bz2",
                extraUrls = originalExtraUrls,
            )

        val entity = TtsModelEntity.fromDomain(model)
        println("Serialized extraUrls: ${entity.extraUrls}")
        val domain = entity.toDomain()

        assertEquals(originalExtraUrls, domain.extraUrls)
    }

    @Test
    fun `voice profile roundtrips through the entity`() {
        val profile = VoiceProfile(
            quality = VoiceQuality.HIGH,
            gender = VoiceGender.MIXED,
            locale = "en-US",
            speakerCount = 11,
        )
        val model = TtsModel(
            id = "kokoro",
            name = "Kokoro",
            language = "en",
            voice = profile,
            type = TtsModelType.KOKORO,
            modelUrl = "",
        )

        assertEquals(profile, TtsModelEntity.fromDomain(model).toDomain().voice)
    }

    @Test
    fun `unknown voice enums fall back instead of throwing`() {
        val entity =
            TtsModelEntity(
                id = "legacy",
                name = "Legacy",
                language = "en",
                type = "BOGUS_TYPE",
                modelUrl = "",
                tokensUrl = "",
                dataDir = null,
                sizeBytes = 0,
                isDownloaded = false,
                progress = 0f,
                voiceQuality = "PREMIUM",
                voiceGender = "ROBOT",
                voiceSpeakerCount = 0,
            )

        val domain = entity.toDomain()

        assertEquals(TtsModelType.VITS, domain.type)
        assertEquals(VoiceQuality.UNKNOWN, domain.voice.quality)
        assertEquals(VoiceGender.UNSPECIFIED, domain.voice.gender)
        assertEquals(1, domain.voice.speakerCount)
    }

    @Test
    fun `catalog metadata keeps install state and the selected speaker`() {
        val existing =
            TtsModelEntity(
                id = "kokoro",
                name = "Old name",
                language = "en",
                type = "KOKORO",
                modelUrl = "",
                tokensUrl = "",
                dataDir = "/data/tts_models/kokoro",
                sizeBytes = 0,
                isDownloaded = true,
                progress = 0.75f,
                speakerId = 7,
                lastError = "stale",
            )
        val catalog =
            TtsModelEntity(
                id = "kokoro",
                name = "Kokoro",
                language = "en",
                type = "KOKORO",
                modelUrl = "https://example.com/kokoro.tar.bz2",
                tokensUrl = "",
                dataDir = null,
                sizeBytes = 80_000_000,
                isDownloaded = false,
                progress = 0f,
            )

        val merged = catalog.withPreservedState(existing)

        assertEquals("Kokoro", merged.name)
        assertEquals(80_000_000L, merged.sizeBytes)
        assertTrue(merged.isDownloaded)
        assertEquals(0.75f, merged.progress, 0.0001f)
        assertEquals("/data/tts_models/kokoro", merged.dataDir)
        assertEquals(7, merged.speakerId)
        assertEquals("stale", merged.lastError)
    }
}
