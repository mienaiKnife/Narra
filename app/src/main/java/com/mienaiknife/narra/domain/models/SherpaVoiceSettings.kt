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
 * Defaults and slider bounds for the on-device voice tuning controls.
 *
 * The values mirror Sherpa-ONNX's own defaults for each model type, so a model starts with the
 * sound its author intended instead of sharing one global default.
 */
object SherpaVoiceDefaults {
    const val NOISE_SCALE = 0.667f
    const val NOISE_SCALE_W = 0.8f
    const val LENGTH_SCALE = 1.0f
    const val PAUSE_SCALE = 0.2f
    const val EXTRA_PAUSE_MS = 0
    const val CPU_THREADS = 4
    const val SPEAKER_ID = 0
}

object SherpaVoiceRanges {
    val NOISE_SCALE = 0f..1.5f
    val NOISE_SCALE_W = 0f..1.5f
    val LENGTH_SCALE = 0.5f..2.0f
    val PAUSE_SCALE = 0.05f..2.0f
    val EXTRA_PAUSE_MS = 0..1000
    val CPU_THREADS = 1..8
}

/** The run-time knobs of the on-device engine that can be tuned per voice model. */
data class SherpaVoiceParams(
    val noiseScale: Float = SherpaVoiceDefaults.NOISE_SCALE,
    val noiseScaleW: Float = SherpaVoiceDefaults.NOISE_SCALE_W,
    val lengthScale: Float = SherpaVoiceDefaults.LENGTH_SCALE,
    val pauseScale: Float = SherpaVoiceDefaults.PAUSE_SCALE,
    val extraPauseMs: Int = SherpaVoiceDefaults.EXTRA_PAUSE_MS,
    val cpuThreads: Int = SherpaVoiceDefaults.CPU_THREADS,
    val speakerId: Int = SherpaVoiceDefaults.SPEAKER_ID,
)

/** Which controls a model type actually consumes. Everything else is hidden in the UI. */
data class VoiceCapabilities(
    val noiseScale: Boolean = false,
    val noiseScaleW: Boolean = false,
    val lengthScale: Boolean = false,
    val pauseScale: Boolean = false,
)

val TtsModelType.voiceCapabilities: VoiceCapabilities
    get() = when (this) {
        TtsModelType.VITS -> VoiceCapabilities(
            noiseScale = true,
            noiseScaleW = true,
            lengthScale = true,
            pauseScale = true,
        )

        TtsModelType.MATCHA -> VoiceCapabilities(
            noiseScale = true,
            lengthScale = true,
            pauseScale = true,
        )

        TtsModelType.KOKORO,
        TtsModelType.KITTEN,
        -> VoiceCapabilities(
            lengthScale = true,
            pauseScale = true,
        )

        TtsModelType.ZIPVOICE,
        TtsModelType.POCKET,
        -> VoiceCapabilities(
            pauseScale = true,
        )

        // Supertonic has no silence or length knob; it chunks text internally with its own
        // `silence_duration` extra, which Narra never reaches because it synthesizes per sentence.
        TtsModelType.SUPERTONIC -> VoiceCapabilities()
    }

fun TtsModelType.defaultVoiceParams(): SherpaVoiceParams = SherpaVoiceParams(
    noiseScale = when (this) {
        TtsModelType.MATCHA -> 1.0f
        else -> SherpaVoiceDefaults.NOISE_SCALE
    },
)

/**
 * Per-model values stored in settings. `null` means "not overridden", so the model-type default
 * applies.
 */
data class SherpaVoiceOverrides(
    val noiseScale: Float? = null,
    val noiseScaleW: Float? = null,
    val lengthScale: Float? = null,
    val pauseScale: Float? = null,
    val extraPauseMs: Int? = null,
    val cpuThreads: Int? = null,
    val speakerId: Int? = null,
) {
    fun applyTo(defaults: SherpaVoiceParams): SherpaVoiceParams = SherpaVoiceParams(
        noiseScale = (noiseScale ?: defaults.noiseScale).coerceIn(SherpaVoiceRanges.NOISE_SCALE),
        noiseScaleW = (noiseScaleW ?: defaults.noiseScaleW).coerceIn(SherpaVoiceRanges.NOISE_SCALE_W),
        lengthScale = (lengthScale ?: defaults.lengthScale).coerceIn(SherpaVoiceRanges.LENGTH_SCALE),
        pauseScale = (pauseScale ?: defaults.pauseScale).coerceIn(SherpaVoiceRanges.PAUSE_SCALE),
        extraPauseMs = (extraPauseMs ?: defaults.extraPauseMs).coerceIn(SherpaVoiceRanges.EXTRA_PAUSE_MS),
        cpuThreads = (cpuThreads ?: defaults.cpuThreads).coerceIn(SherpaVoiceRanges.CPU_THREADS),
        speakerId = (speakerId ?: defaults.speakerId).coerceAtLeast(0),
    )
}

/**
 * The subset of [SherpaVoiceParams] that changes the native engine configuration, with values the
 * model type ignores replaced by their defaults and the non-native settings cleared. The engine
 * compares this before reloading, so a leftover hidden value can never rebuild the model.
 */
fun SherpaVoiceParams.renderKeyFor(type: TtsModelType?): SherpaVoiceParams {
    val capabilities = type?.voiceCapabilities ?: VoiceCapabilities()
    return copy(
        noiseScale = if (capabilities.noiseScale) noiseScale else SherpaVoiceDefaults.NOISE_SCALE,
        noiseScaleW = if (capabilities.noiseScaleW) noiseScaleW else SherpaVoiceDefaults.NOISE_SCALE_W,
        lengthScale = if (capabilities.lengthScale) lengthScale else SherpaVoiceDefaults.LENGTH_SCALE,
        pauseScale = if (capabilities.pauseScale) pauseScale else SherpaVoiceDefaults.PAUSE_SCALE,
        extraPauseMs = SherpaVoiceDefaults.EXTRA_PAUSE_MS,
        speakerId = SherpaVoiceDefaults.SPEAKER_ID,
    )
}
