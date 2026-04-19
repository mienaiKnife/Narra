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
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.ui.utils.HtmlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getHistoryArticles(),
        _isRefreshing,
        playbackManager.currentArticle,
        playbackManager.isPlaying
    ) { articles, isRefreshing, currentArticle, isPlaying ->
        HistoryUiState(
            articles = articles,
            isRefreshing = isRefreshing,
            currentArticle = currentArticle,
            isPlaying = isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onPlayPauseClick(article: Article) {
        if (!article.isInQueue) {
            addToQueue(article)
        } else {
            if (uiState.value.currentArticle?.id == article.id) {
                playbackManager.togglePlayPause()
            } else {
                val blocks = HtmlParser.parse(article.content)
                playbackManager.setCurrentArticle(article, blocks)
            }
        }
    }

    fun addToQueue(article: Article) {
        viewModelScope.launch {
            repository.addToQueue(article.id).onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to add to queue"))
            }
        }
    }

    fun togglePlayedStatus(article: Article) {
        viewModelScope.launch {
            if (article.progress == 1f) {
                repository.markAsUnplayed(article.id)
                repository.addToQueue(article.id).onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to re-add to queue"))
                }
            } else {
                repository.markAsFinished(article.id)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshFeeds().onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to refresh feeds"))
            }
            _isRefreshing.value = false
        }
    }
}
