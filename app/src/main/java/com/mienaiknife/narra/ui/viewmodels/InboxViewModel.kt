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
import com.mienaiknife.narra.data.models.SortOption
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val currentArticle: Article? = null,
    val isPlaying: Boolean = false
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _isRefreshing = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)

    val uiState: StateFlow<InboxUiState> = combine(
        repository.getInboxArticles(),
        _isRefreshing,
        _sortOption,
        playbackManager.currentArticle,
        playbackManager.isPlaying
    ) { flowArray ->
        val articles = flowArray[0] as List<Article>
        val isRefreshing = flowArray[1] as Boolean
        val sort = flowArray[2] as SortOption
        val currentArticle = flowArray[3] as? Article
        val isPlaying = flowArray[4] as Boolean

        val sortedArticles = when (sort) {
            SortOption.MANUAL -> articles
            SortOption.DATE_DESC -> articles.sortedByDescending { it.publishedTimestamp ?: 0L }
            SortOption.DATE_ASC -> articles.sortedBy { it.publishedTimestamp ?: Long.MAX_VALUE }
            SortOption.TITLE_ASC -> articles.sortedBy { it.title }
            SortOption.TITLE_DESC -> articles.sortedByDescending { it.title }
            SortOption.SOURCE_ASC -> articles.sortedBy { it.source }
            SortOption.SOURCE_DESC -> articles.sortedByDescending { it.source }
        }
        InboxUiState(
            articles = sortedArticles,
            isRefreshing = isRefreshing,
            sortOption = sort,
            currentArticle = currentArticle,
            isPlaying = isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState()
    )

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            val next = when (option) {
                SortOption.DATE_DESC -> SortOption.DATE_ASC
                SortOption.DATE_ASC -> SortOption.DATE_DESC
                SortOption.TITLE_ASC -> SortOption.TITLE_DESC
                SortOption.TITLE_DESC -> SortOption.TITLE_ASC
                SortOption.SOURCE_ASC -> SortOption.SOURCE_DESC
                SortOption.SOURCE_DESC -> SortOption.SOURCE_ASC
                SortOption.MANUAL -> SortOption.MANUAL
            }
            _sortOption.value = next
        } else {
            _sortOption.value = option
        }
    }

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshFeeds().onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to refresh feeds"))
            }
            _isRefreshing.value = false
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
            val newProgress = if (article.progress == 1f) 0f else 1f
            repository.updateArticleProgress(article.id, newProgress, 0, 0)
        }
    }

    fun clearInbox() {
        viewModelScope.launch {
            repository.clearInbox()
        }
    }
}
