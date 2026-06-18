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
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.domain.repository.FeedRepository
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.ui.UiText
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
    private val repository: ArticleRepository,
    private val feedRepository: FeedRepository,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val uiText: UiText) : UiEvent()
    }

    private val _isRefreshing = MutableStateFlow(false)
    private val _downloadingArticleIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getHistoryArticles(),
        _isRefreshing,
        _downloadingArticleIds,
        playbackManager.currentArticle,
        playbackManager.isPlaying,
        playbackManager.playbackSpeed,
    ) { args: Array<Any?> ->
        val articles = args[0] as List<Article>
        val isRefreshing = args[1] as Boolean
        val downloadingIds = args[2] as Set<String>
        val currentArticle = args[3] as Article?
        val isPlaying = args[4] as Boolean
        val playbackSpeed = args[5] as Float

        HistoryUiState(
            articles = articles,
            isRefreshing = isRefreshing,
            currentArticle = currentArticle,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            downloadingArticleIds = downloadingIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(),
    )

    fun onPlayPauseClick(article: Article) {
        if (!article.isInQueue) {
            viewModelScope.launch {
                if (article.content.isEmpty()) {
                    _downloadingArticleIds.value += article.id
                }
                repository.addToQueue(article.id).onSuccess {
                    val updatedArticle = repository.getArticleById(article.id)
                    if (updatedArticle != null && updatedArticle.content.isNotEmpty()) {
                        val blocks = HtmlParser.parse(updatedArticle.content)
                        playbackManager.setCurrentArticle(updatedArticle, blocks)
                    }
                }.onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
                }.also {
                    _downloadingArticleIds.value -= article.id
                }
            }
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
            if (article.content.isEmpty()) {
                _downloadingArticleIds.value += article.id
            }
            repository.addToQueue(article.id).onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
            }.also {
                _downloadingArticleIds.value -= article.id
            }
        }
    }

    fun togglePlayedStatus(article: Article) {
        viewModelScope.launch {
            if (article.progress == 1f) {
                repository.markAsUnplayed(article.id)
                repository.addToQueue(article.id).onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
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
            feedRepository.refreshFeeds().onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
            }
            _isRefreshing.value = false
        }
    }
}
