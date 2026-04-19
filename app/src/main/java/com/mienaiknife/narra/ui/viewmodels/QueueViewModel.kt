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
import com.mienaiknife.narra.data.models.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _isRefreshing = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.MANUAL)
    private val _keepSorted = MutableStateFlow(false)

    val uiState: StateFlow<QueueUiState> = combine(
        repository.getQueueArticles(),
        combine(_isRefreshing, _sortOption, _keepSorted) { refreshing, sort, keep ->
            Triple(refreshing, sort, keep)
        },
        playbackManager.currentArticle,
        playbackManager.isPlaying
    ) { articles, settings, currentArticle, isPlaying ->
        val (isRefreshing, sort, keep) = settings

        val sortedArticles = if (keep) {
            when (sort) {
                SortOption.MANUAL -> articles
                SortOption.DATE_DESC -> articles.sortedByDescending { it.publishedTimestamp ?: 0L }
                SortOption.DATE_ASC -> articles.sortedBy { it.publishedTimestamp ?: Long.MAX_VALUE }
                SortOption.TITLE_ASC -> articles.sortedBy { it.title }
                SortOption.TITLE_DESC -> articles.sortedByDescending { it.title }
                SortOption.SOURCE_ASC -> articles.sortedBy { it.source }
                SortOption.SOURCE_DESC -> articles.sortedByDescending { it.source }
            }
        } else {
            articles
        }
        QueueUiState(
            articles = sortedArticles,
            isRefreshing = isRefreshing,
            sortOption = sort,
            keepSorted = keep,
            currentArticle = currentArticle,
            isPlaying = isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QueueUiState()
    )

    fun setSortOption(option: SortOption) {
        val finalOption = if (_sortOption.value == option) {
            when (option) {
                SortOption.DATE_DESC -> SortOption.DATE_ASC
                SortOption.DATE_ASC -> SortOption.DATE_DESC
                SortOption.TITLE_ASC -> SortOption.TITLE_DESC
                SortOption.TITLE_DESC -> SortOption.TITLE_ASC
                SortOption.SOURCE_ASC -> SortOption.SOURCE_DESC
                SortOption.SOURCE_DESC -> SortOption.SOURCE_ASC
                SortOption.MANUAL -> SortOption.MANUAL
            }
        } else {
            option
        }
        _sortOption.value = finalOption
        if (!_keepSorted.value) {
            viewModelScope.launch {
                applyOneTimeSort(finalOption)
            }
        }
    }

    fun setKeepSorted(keep: Boolean) {
        _keepSorted.value = keep
        if (!keep) {
            _sortOption.value = SortOption.MANUAL
        }
    }

    private suspend fun applyOneTimeSort(option: SortOption) {
        val currentArticles = repository.getQueueArticles().first()
        val sortedArticles = when (option) {
            SortOption.DATE_DESC -> currentArticles.sortedByDescending { it.publishedTimestamp ?: 0L }
            SortOption.DATE_ASC -> currentArticles.sortedBy { it.publishedTimestamp ?: Long.MAX_VALUE }
            SortOption.TITLE_ASC -> currentArticles.sortedBy { it.title }
            SortOption.TITLE_DESC -> currentArticles.sortedByDescending { it.title }
            SortOption.SOURCE_ASC -> currentArticles.sortedBy { it.source }
            SortOption.SOURCE_DESC -> currentArticles.sortedByDescending { it.source }
            SortOption.MANUAL -> currentArticles
        }

        // Apply this order to the database
        repository.updateQueueOrder(sortedArticles.map { it.id })
    }

    fun onPlayPauseClick(article: Article) {
        if (uiState.value.currentArticle?.id == article.id) {
            playbackManager.togglePlayPause()
        } else {
            val blocks = HtmlParser.parse(article.content)
            playbackManager.setCurrentArticle(article, blocks)
        }
    }

    fun removeFromQueue(article: Article) {
        viewModelScope.launch {
            repository.removeFromQueue(article.id)
        }
    }

    fun togglePlayedStatus(article: Article) {
        viewModelScope.launch {
            if (article.progress == 1f) {
                repository.markAsUnplayed(article.id)
                repository.addToQueue(article.id).onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to add to queue"))
                }
            } else {
                val isCurrentlyPlaying = uiState.value.currentArticle?.id == article.id
                val nextArticle = if (isCurrentlyPlaying) {
                    val currentList = uiState.value.articles
                    val currentIndex = currentList.indexOfFirst { it.id == article.id }
                    if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                        currentList[currentIndex + 1]
                    } else null
                } else null

                repository.markAsPlayed(article.id)

                if (isCurrentlyPlaying) {
                    if (nextArticle != null) {
                        onPlayPauseClick(nextArticle)
                    } else {
                        playbackManager.stop()
                    }
                }
            }
        }
    }

    fun addToQueue(articleId: String) {
        viewModelScope.launch {
            repository.addToQueue(articleId).onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to add to queue"))
            }
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            _keepSorted.value = false
            _sortOption.value = SortOption.MANUAL
            repository.reorderQueue(fromIndex, toIndex)
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
