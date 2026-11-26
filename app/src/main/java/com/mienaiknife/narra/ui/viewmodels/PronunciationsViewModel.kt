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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.domain.pronunciation.PronunciationValidator
import com.mienaiknife.narra.domain.repository.CustomPronunciationRepository
import com.mienaiknife.narra.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PronunciationsViewModel @Inject constructor(
    private val repository: CustomPronunciationRepository,
    private val ttsEngine: TtsEngine,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<PronunciationsUiState> = combine(
        repository.observeAll(),
        ttsEngine.state,
        _errorMessage,
    ) { entries, engineState, errorMessage ->
        PronunciationsUiState(
            entries = entries
                .filter { it.isEnglish() }
                .sortedBy { it.sortKey() },
            engineState = engineState,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PronunciationsUiState(),
    )

    fun save(word: String, arpabet: String, original: CustomPronunciation? = null) {
        when (val validated = PronunciationValidator.validate(word, arpabet)) {
            is PronunciationValidator.Result.Invalid -> {
                _errorMessage.value = validated.error.toUiText(validated.detail)
            }

            is PronunciationValidator.Result.Valid -> {
                val entry = CustomPronunciation(
                    word = validated.word,
                    arpabet = arpabet.trim(),
                    language = CustomPronunciation.DEFAULT_LANGUAGE,
                )
                viewModelScope.launch {
                    val result = if (
                        original != null &&
                        PronunciationValidator.normalizeWord(original.word) != validated.normalizedWord
                    ) {
                        // Editing the spelling replaces the old entry instead of leaving it behind.
                        repository.replace(original, entry)
                    } else {
                        repository.upsert(entry)
                    }
                    result.onFailure {
                        _errorMessage.value = UiText.StringResource(R.string.settings_pronunciations_save_failed)
                    }
                }
            }
        }
    }

    fun delete(entry: CustomPronunciation) {
        viewModelScope.launch {
            repository.delete(entry).onFailure {
                _errorMessage.value = UiText.StringResource(R.string.settings_pronunciations_delete_failed)
            }
        }
    }

    fun preview(entry: CustomPronunciation) {
        if (ttsEngine.state.value is TtsState.Initializing) return
        ttsEngine.speak(entry.word, "pronunciation-preview-${entry.word}")
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /** Only English entries can be mapped to the on-device lexicon's phone inventory. */
    private fun CustomPronunciation.isEnglish(): Boolean {
        val primarySubtag = language.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        return primarySubtag == CustomPronunciation.DEFAULT_LANGUAGE
    }

    private fun CustomPronunciation.sortKey(): String = PronunciationValidator.normalizeWord(word) ?: word.lowercase(Locale.ROOT)
}
