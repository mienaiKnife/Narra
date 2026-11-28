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

/**
 * The quality tier of a voice, using the same low/medium/high vocabulary as Piper's voice
 * metadata. [UNKNOWN] is a safe fallback for rows written by older app versions.
 */
enum class VoiceQuality {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN,
}

/** Who the voice belongs to. Multi-speaker models report [MIXED] or [UNSPECIFIED]. */
enum class VoiceGender {
    FEMALE,
    MALE,
    MIXED,
    UNSPECIFIED,
}

/**
 * Structured presentation metadata for a voice model.
 *
 * [locale] is a BCP-47 language tag (for example `en-US`) and is the standard source for the
 * displayed accent. It is separate from [TtsModel.language], which is the bare language code used
 * to select a pronunciation lexicon; [TtsModelCatalog] asserts that the two agree.
 */
data class VoiceProfile(
    val quality: VoiceQuality = VoiceQuality.UNKNOWN,
    val gender: VoiceGender = VoiceGender.UNSPECIFIED,
    val locale: String = "",
    val speakerCount: Int = 1,
)
