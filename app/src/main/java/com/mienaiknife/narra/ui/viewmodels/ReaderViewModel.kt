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
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.ui.UiText
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.utils.HtmlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: String = savedStateHandle.toRoute<NavDestination.Reader>().articleId

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val uiText: UiText) : UiEvent()
    }

    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    private val _isLoading = MutableStateFlow(value = true)
    private val _error = MutableStateFlow<UiText?>(null)
    private val _searchQuery = MutableStateFlow("")

    // Tracks the article whose content has already been parsed into [_blocks] so the
    // currentArticle observer does not parse it a second time.
    private var parsedArticleId: String? = null

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
                                previewText = text, // Could be truncated
                            ),
                        )
                        startIndex = found + query.length
                    }
                    results
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private data class PlaybackCore(
        val article: Article?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val currentPosition: Long,
        val duration: Long,
    )

    private data class PlaybackNav(
        val playbackSpeed: Float,
        val currentParagraphIndex: Int,
        val currentWordRange: IntRange?,
        val sleepTimerMillisLeft: Long?,
    )

    private data class ReaderContent(
        val blocks: List<ContentBlock>,
        val isLoading: Boolean,
        val error: UiText?,
    )

    private data class ReaderSearch(
        val query: String,
        val results: List<SearchResult>,
    )

    private data class SkipTimes(
        val fastForward: String,
        val rewind: String,
    )

    val uiState: StateFlow<ReaderUiState> = combine(
        combine(
            playbackManager.currentArticle,
            playbackManager.isPlaying,
            playbackManager.isBuffering,
            playbackManager.currentPosition,
            playbackManager.duration,
            ::PlaybackCore,
        ),
        combine(
            playbackManager.playbackSpeed,
            playbackManager.currentParagraphIndex,
            playbackManager.currentWordRange,
            playbackManager.sleepTimerMillisLeft,
            ::PlaybackNav,
        ),
        combine(
            _blocks,
            _isLoading,
            _error,
            ::ReaderContent,
        ),
        combine(
            _searchQuery,
            _searchResults,
            ::ReaderSearch,
        ),
        combine(
            playbackManager.settingsManager.fastForwardSkipTime,
            playbackManager.settingsManager.rewindSkipTime,
            ::SkipTimes,
        ),
    ) { core, nav, content, search, skips ->
        ReaderUiState(
            article = core.article,
            blocks = content.blocks,
            // Keep loading if explicit flag is true OR if we have an article with content but no parsed blocks yet.
            // This prevents the UI from initializing with 0 items, which would reset the scroll state.
            isLoading =
            content.isLoading ||
                (core.article != null && content.blocks.isEmpty() && core.article.content.isNotBlank()),
            error = content.error,
            isPlaying = core.isPlaying,
            isBuffering = core.isBuffering,
            currentPosition = core.currentPosition,
            duration = core.duration,
            playbackSpeed = nav.playbackSpeed,
            currentParagraphIndex = nav.currentParagraphIndex,
            currentWordRange = nav.currentWordRange,
            fastForwardSkipTime = skips.fastForward,
            rewindSkipTime = skips.rewind,
            sleepTimerMillisLeft = nav.sleepTimerMillisLeft,
            searchQuery = search.query,
            searchResults = search.results,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderUiState(isLoading = true),
    )

    init {
        loadArticle(articleId)

        // Automatically update blocks when the article changes in the PlaybackManager
        playbackManager.currentArticle
            .onEach { art ->
                if (art == null) {
                    parsedArticleId = null
                    _blocks.value = emptyList()
                } else if (art.id != parsedArticleId) {
                    val parsedBlocks = withContext(Dispatchers.Default) {
                        HtmlParser.parse(art.content, art.url)
                    }
                    parsedArticleId = art.id
                    _blocks.value = parsedBlocks
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadArticle(id: String) {
        val currentArt = playbackManager.currentArticle.value
        if (currentArt?.id == id && currentArt.content.isNotEmpty()) {
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
                            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
                            _error.value = UiText.fromError(error)
                            return@launch
                        }
                        // Refresh data after adding to queue (which might have downloaded content)
                        articleData = repository.getArticleById(id)
                    }

                    if (articleData != null) {
                        // Parse once here; [_blocks] is updated directly and the
                        // currentArticle observer skips re-parsing the same article.
                        val blocks = withContext(Dispatchers.Default) {
                            HtmlParser.parse(articleData.content, articleData.url)
                        }
                        parsedArticleId = articleData.id
                        _blocks.value = blocks
                        playbackManager.setCurrentArticle(articleData, blocks, playWhenReady = false)
                    } else {
                        _error.value = UiText.fromError(NarraError.Content.NotFound())
                    }
                } else {
                    _error.value = UiText.fromError(NarraError.Content.NotFound())
                }
            } catch (e: Exception) {
                _error.value = UiText.fromError(NarraError.Unknown(e))
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
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite() {
        uiState.value.article?.let { art ->
            viewModelScope.launch {
                repository.toggleFavorite(art.id)
            }
        }
    }
}
