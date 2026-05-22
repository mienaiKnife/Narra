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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mienaiknife.narra.NavDestination
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import kotlinx.coroutines.flow.combine

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: String = savedStateHandle.toRoute<NavDestination.Reader>().articleId

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    private val _isLoading = MutableStateFlow(value = true)
    private val _error = MutableStateFlow<Throwable?>(null)
    private val _searchQuery = MutableStateFlow("")

    private val _searchResults = combine(_blocks, _searchQuery) { blocks, query ->
        if (query.length >= 2) {
            withContext(Dispatchers.Default) {
                blocks.flatMapIndexed { index, block ->
                    val text = block.text.text
                    val results = mutableListOf<SearchResult>()
                    var startIndex = 0
                    while (startIndex < text.length) {
                        val found = text.indexOf(query, startIndex, ignoreCase = true)
                        if (found == -1) break
                        results.add(
                            SearchResult(
                                paragraphIndex = index,
                                wordRange = found until (found + query.length),
                                previewText = text // Could be truncated
                            )
                        )
                        startIndex = found + query.length
                    }
                    results
                }
            }
        } else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ReaderUiState> = combine(
        playbackManager.currentArticle,
        _blocks,
        _isLoading,
        _error,
        playbackManager.isPlaying,
        playbackManager.currentPosition,
        playbackManager.duration,
        playbackManager.playbackSpeed,
        playbackManager.currentParagraphIndex,
        playbackManager.currentWordRange,
        playbackManager.settingsManager.fastForwardSkipTime,
        playbackManager.settingsManager.rewindSkipTime,
        playbackManager.sleepTimerMillisLeft,
        _searchQuery,
        _searchResults
    ) { flows ->
        ReaderUiState(
            article = flows[0] as Article?,
            blocks = flows[1] as List<ContentBlock>,
            isLoading = flows[2] as Boolean,
            error = flows[3] as Throwable?,
            isPlaying = flows[4] as Boolean,
            currentPosition = flows[5] as Long,
            duration = flows[6] as Long,
            playbackSpeed = flows[7] as Float,
            currentParagraphIndex = flows[8] as Int,
            currentWordRange = flows[9] as IntRange?,
            fastForwardSkipTime = flows[10] as String,
            rewindSkipTime = flows[11] as String,
            sleepTimerMillisLeft = flows[12] as Long?,
            searchQuery = flows[13] as String,
            searchResults = flows[14] as List<SearchResult>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderUiState(isLoading = true)
    )

    init {
        loadArticle(articleId)

        // Automatically update blocks when the article changes in the PlaybackManager
        playbackManager.currentArticle
            .onEach { art ->
                if (art != null) {
                    val parsedBlocks = withContext(Dispatchers.Default) {
                        HtmlParser.parse(art.content)
                    }
                    _blocks.value = parsedBlocks
                } else {
                    _blocks.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadArticle(id: String) {
        if (playbackManager.currentArticle.value?.id == id) {
            _isLoading.value = false
            _error.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                var articleData = repository.getArticleById(id)
                if (articleData != null) {
                    if (!articleData.isInQueue) {
                        repository.addToQueue(id).onFailure { error ->
                            _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to download article"))
                            _error.value = error
                            return@launch
                        }
                        // Refresh data after adding to queue (which might have downloaded content)
                        articleData = repository.getArticleById(id)
                    }

                    if (articleData != null) {
                        // This triggers the Flow in PlaybackManager, which our init block observes
                        val blocks = withContext(Dispatchers.Default) {
                            HtmlParser.parse(articleData.content)
                        }
                        playbackManager.setCurrentArticle(articleData, blocks, playWhenReady = false)
                    } else {
                        _error.value = Exception("Article not found after adding to queue")
                    }
                } else {
                    _error.value = Exception("Article not found")
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        loadArticle(articleId)
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekToWord(paragraphIndex: Int, wordRange: IntRange) = playbackManager.seekToWord(paragraphIndex, wordRange)
    fun skipForward() = playbackManager.skipForward()
    fun skipBackward() = playbackManager.skipBackward()
    fun skipNext() = playbackManager.skipNext()
    fun cycleSpeed() = playbackManager.cycleSpeed()
    fun setSleepTimer(minutes: Int?) = playbackManager.setSleepTimer(minutes)
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun toggleFavorite() {
        uiState.value.article?.let { art ->
            viewModelScope.launch {
                repository.toggleFavorite(art.id)
            }
        }
    }
}
