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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PronunciationsViewModel @Inject constructor(
    private val repository: CustomPronunciationRepository,
    private val ttsEngine: TtsEngine,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<UiText?>(null)

    private var previewJob: Job? = null

    /**
     * True from the moment an entry is saved or deleted until the engine has rebuilt its
     * lexicon. A preview started in that window would be synthesized against the old lexicon
     * and then discarded when the engine reloads, which sounds like a silent play button.
     */
    private var reloadPending = false
    private var wasInitializing = false

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

    init {
        viewModelScope.launch {
            ttsEngine.state.collect { state ->
                val initializing = state is TtsState.Initializing
                if (wasInitializing && !initializing) reloadPending = false
                wasInitializing = initializing
            }
        }
    }

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
                    reloadPending = true
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
                        reloadPending = false
                        _errorMessage.value = UiText.StringResource(R.string.settings_pronunciations_save_failed)
                    }
                }
            }
        }
    }

    fun delete(entry: CustomPronunciation) {
        viewModelScope.launch {
            reloadPending = true
            repository.delete(entry).onFailure {
                reloadPending = false
                _errorMessage.value = UiText.StringResource(R.string.settings_pronunciations_delete_failed)
            }
        }
    }

    fun preview(entry: CustomPronunciation) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            awaitEngineReady()
            ttsEngine.speak(entry.word, "pronunciation-preview-${entry.word}")
        }
    }

    /**
     * Waits until the engine can play a preview. Saving or deleting an entry makes it rebuild the
     * patched lexicon after a short debounce; anything spoken before that reload finishes is cut
     * off when the native engine is recreated. Previews tapped while the engine is initializing are
     * queued instead of being dropped.
     */
    private suspend fun awaitEngineReady() {
        if (reloadPending) {
            // The debounced reload may not have started yet, so wait for it briefly. If the
            // entry did not actually change the lexicon no reload follows and the timeouts let
            // the preview through.
            withTimeoutOrNull(RELOAD_START_TIMEOUT_MS) {
                ttsEngine.state.first { it is TtsState.Initializing }
            }
            withTimeoutOrNull(RELOAD_FINISH_TIMEOUT_MS) {
                ttsEngine.state.first { it !is TtsState.Initializing }
            }
            reloadPending = false
        } else {
            ttsEngine.state.first { it !is TtsState.Initializing }
        }
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

    private companion object {
        /** Covers the engine's 500 ms reload debounce plus the settings flow's latency. */
        const val RELOAD_START_TIMEOUT_MS = 2_000L

        /** Upper bound for a lexicon rebuild; the preview still runs if it takes longer. */
        const val RELOAD_FINISH_TIMEOUT_MS = 15_000L
    }
}
