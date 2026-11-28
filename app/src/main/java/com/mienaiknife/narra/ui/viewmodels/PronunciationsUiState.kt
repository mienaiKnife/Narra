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
package com.mienaiknife.narra.ui.viewmodels

import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.domain.pronunciation.PronunciationValidator
import com.mienaiknife.narra.ui.UiText

data class PronunciationsUiState(
    val entries: List<CustomPronunciation> = emptyList(),
    val engineState: TtsState = TtsState.Idle,
    val errorMessage: UiText? = null,
) {
    /** Preview playback is only meaningful once a model is loaded and the engine is usable. */
    val engineReady: Boolean
        get() = engineState is TtsState.Ready ||
            engineState is TtsState.Speaking ||
            engineState is TtsState.Buffering ||
            engineState is TtsState.Finished

    /**
     * A preview requested while the engine is reloading the lexicon is queued and plays when the
     * reload finishes, so the button stays enabled during initialization.
     */
    val previewEnabled: Boolean
        get() = engineReady || engineState is TtsState.Initializing
}

/**
 * Maps a validation failure to the message shown in a toast after saving and inline in the editor.
 *
 * [detail] is the offending ARPAbet token for [PronunciationValidator.Error.UNKNOWN_SYMBOL] and
 * [PronunciationValidator.Error.STRESS_ON_CONSONANT].
 */
internal fun PronunciationValidator.Error.toUiText(detail: String? = null): UiText = when (this) {
    PronunciationValidator.Error.EMPTY_WORD ->
        UiText.StringResource(R.string.settings_pronunciations_error_empty_word)

    PronunciationValidator.Error.WORD_TOO_LONG ->
        UiText.StringResource(R.string.settings_pronunciations_error_word_too_long)

    PronunciationValidator.Error.INVALID_WORD_CHARACTER ->
        UiText.StringResource(R.string.settings_pronunciations_error_invalid_word)

    PronunciationValidator.Error.EMPTY_PRONUNCIATION ->
        UiText.StringResource(R.string.settings_pronunciations_error_empty_pronunciation)

    PronunciationValidator.Error.UNKNOWN_SYMBOL ->
        UiText.StringResource(R.string.settings_pronunciations_error_unknown_symbol, detail.orEmpty())

    PronunciationValidator.Error.STRESS_ON_CONSONANT ->
        UiText.StringResource(R.string.settings_pronunciations_error_stress_on_consonant, detail.orEmpty())

    PronunciationValidator.Error.TOO_MANY_SYMBOLS ->
        UiText.StringResource(R.string.settings_pronunciations_error_too_many_symbols)
}
