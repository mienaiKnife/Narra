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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.combine

data class ReaderUiState(
    val article: Article? = null,
    val blocks: List<ContentBlock> = emptyList(),
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentParagraphIndex: Int = 0,
    val currentWordRange: IntRange? = null,
    val fastForwardSkipTime: String = "30s",
    val rewindSkipTime: String = "10s"
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: String = savedStateHandle.toRoute<NavDestination.Reader>().articleId

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<ReaderUiState> = combine(
        playbackManager.currentArticle,
        _blocks,
        _isLoading,
        playbackManager.isPlaying,
        playbackManager.currentPosition,
        playbackManager.duration,
        playbackManager.playbackSpeed,
        playbackManager.currentParagraphIndex,
        playbackManager.currentWordRange,
        playbackManager.settingsManager.fastForwardSkipTime,
        playbackManager.settingsManager.rewindSkipTime
    ) { flows ->
        ReaderUiState(
            article = flows[0] as Article?,
            blocks = flows[1] as List<ContentBlock>,
            isLoading = flows[2] as Boolean,
            isPlaying = flows[3] as Boolean,
            currentPosition = flows[4] as Long,
            duration = flows[5] as Long,
            playbackSpeed = flows[6] as Float,
            currentParagraphIndex = flows[7] as Int,
            currentWordRange = flows[8] as IntRange?,
            fastForwardSkipTime = flows[9] as String,
            rewindSkipTime = flows[10] as String
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
                    val parsedBlocks = HtmlParser.parse(art.content)
                    _blocks.value = parsedBlocks
                } else {
                    _blocks.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadArticle(id: String) {
        if (playbackManager.currentArticle.value?.id == id) return

        viewModelScope.launch {
            _isLoading.value = true
            var articleData = repository.getArticleById(id)
            if (articleData != null) {
                if (!articleData.isInQueue) {
                    repository.addToQueue(id).onFailure { error ->
                        _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to download article"))
                    }
                    // Refresh data after adding to queue (which might have downloaded content)
                    articleData = repository.getArticleById(id)
                }

                if (articleData != null) {
                    // This triggers the Flow in PlaybackManager, which our init block observes
                    val paragraphs = HtmlParser.parse(articleData.content).map { it.text.toString() }
                    playbackManager.setCurrentArticle(articleData, paragraphs, playWhenReady = false)
                }
            }
            _isLoading.value = false
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekTo(position: Long) = playbackManager.seekTo(position)
    fun seekToParagraph(index: Int) = playbackManager.seekToParagraph(index)
    fun seekToWord(paragraphIndex: Int, wordRange: IntRange) = playbackManager.seekToWord(paragraphIndex, wordRange)
    fun skipForward() = playbackManager.skipForward()
    fun skipBackward() = playbackManager.skipBackward()
    fun skipNext() = playbackManager.skipNext()
    fun cycleSpeed() = playbackManager.cycleSpeed()

    fun toggleFavorite() {
        uiState.value.article?.let { art ->
            viewModelScope.launch {
                repository.toggleFavorite(art.id)
            }
        }
    }
}
