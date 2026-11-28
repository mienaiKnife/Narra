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
package com.mienaiknife.narra.data.models

import com.mienaiknife.narra.data.NetworkConstants
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality

/**
 * The voices Narra ships with. Metadata lives here instead of in free-text descriptions so every
 * entry has the same shape, the list can be tested, and translations never drift from the catalog.
 */
internal object TtsModelCatalog {
    val defaultModels: List<TtsModel> =
        listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Piper Amy",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.LOW,
                    gender = VoiceGender.FEMALE,
                    locale = "en-US",
                ),
                type = TtsModelType.VITS,
                modelUrl = "${NetworkConstants.SHERPA_MODELS_BASE_URL}/vits-piper-en_US-amy-low.tar.bz2",
                sizeBytes = 28_000_000,
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Piper Ryan",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.MEDIUM,
                    gender = VoiceGender.MALE,
                    locale = "en-US",
                ),
                type = TtsModelType.VITS,
                modelUrl = "${NetworkConstants.SHERPA_MODELS_BASE_URL}/vits-piper-en_US-ryan-medium.tar.bz2",
                sizeBytes = 60_000_000,
            ),
            TtsModel(
                id = "kokoro-en-v0_19",
                name = "Kokoro",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.HIGH,
                    gender = VoiceGender.MIXED,
                    locale = "en-US",
                    speakerCount = 11,
                ),
                type = TtsModelType.KOKORO,
                modelUrl = "${NetworkConstants.SHERPA_MODELS_BASE_URL}/kokoro-en-v0_19.tar.bz2",
                sizeBytes = 80_000_000,
            ),
            TtsModel(
                id = "matcha-icefall-en_US-ljspeech",
                name = "Matcha Icefall",
                language = "en",
                voice = VoiceProfile(
                    quality = VoiceQuality.HIGH,
                    gender = VoiceGender.FEMALE,
                    locale = "en-US",
                ),
                type = TtsModelType.MATCHA,
                modelUrl = "${NetworkConstants.SHERPA_MODELS_BASE_URL}/matcha-icefall-en_US-ljspeech.tar.bz2",
                sizeBytes = 150_000_000,
            ),
        )
}
