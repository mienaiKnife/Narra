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
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.domain.repository.FeedRepository
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.ui.UiText
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
class FeedArticlesViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val feedRepository: FeedRepository,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val uiText: UiText) : UiEvent()
    }

    private val routeData = savedStateHandle.toRoute<NavDestination.Feed>()
    val feedUrl: String = routeData.feedUrl
    val initialFeedTitle: String = routeData.feedTitle

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    private val _isRefreshing = MutableStateFlow(false)
    private val _showPlayed = MutableStateFlow(true)

    private val _downloadingArticleIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<FeedArticlesUiState> = combine(
        repository.getArticlesByFeedUrl(feedUrl),
        _isRefreshing,
        _sortOption,
        _showPlayed,
        _downloadingArticleIds,
        playbackManager.playbackSpeed
    ) { args: Array<Any?> ->
        val articles = args[0] as List<Article>
        val isRefreshing = args[1] as Boolean
        val sort = args[2] as SortOption
        val showPlayed = args[3] as Boolean
        val downloadingIds = args[4] as Set<String>
        val playbackSpeed = args[5] as Float

        // Use the source of the first article if available, as it might have been updated
        val currentFeedTitle = articles.firstOrNull()?.source ?: initialFeedTitle

        val filteredArticles = if (showPlayed) articles else articles.filter { (it.progress ?: 0f) < 1f }
        val sortedArticles = when (sort) {
            SortOption.MANUAL -> filteredArticles
            SortOption.DATE_DESC -> filteredArticles.sortedByDescending { it.publishedTimestamp ?: 0L }
            SortOption.DATE_ASC -> filteredArticles.sortedBy { it.publishedTimestamp ?: Long.MAX_VALUE }
            SortOption.TITLE_ASC -> filteredArticles.sortedBy { it.title }
            SortOption.TITLE_DESC -> filteredArticles.sortedByDescending { it.title }
            SortOption.SOURCE_ASC -> filteredArticles.sortedBy { it.source }
            SortOption.SOURCE_DESC -> filteredArticles.sortedByDescending { it.source }
        }
        FeedArticlesUiState(
            articles = sortedArticles,
            isRefreshing = isRefreshing,
            sortOption = sort,
            showPlayed = showPlayed,
            playbackSpeed = playbackSpeed,
            feedTitle = currentFeedTitle,
            downloadingArticleIds = downloadingIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedArticlesUiState(feedTitle = initialFeedTitle)
    )

    fun setShowPlayed(show: Boolean) {
        _showPlayed.value = show
    }


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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.refreshFeeds().onFailure { error ->
                _uiEvent.emit(UiEvent.ShowSnackbar(UiText.fromError(error)))
            }
            _isRefreshing.value = false
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

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            repository.deleteArticle(article.id)
        }
    }

    fun markAllAsPlayed() {
        viewModelScope.launch {
            repository.markAllAsPlayedInFeed(feedUrl)
        }
    }

    fun markAllAsUnplayed() {
        viewModelScope.launch {
            repository.markAllAsUnplayedInFeed(feedUrl)
        }
    }
}
