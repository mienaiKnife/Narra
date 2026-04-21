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
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.repository.ContentRepository
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
    private val repository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    val feedTitle: String = savedStateHandle.toRoute<NavDestination.Feed>().feedTitle

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    private val _showPlayed = MutableStateFlow(false)

    private val _downloadingArticleIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<FeedArticlesUiState> = combine(
        repository.getArticlesBySource(feedTitle),
        _sortOption,
        _showPlayed,
        _downloadingArticleIds
    ) { articles, sort, showPlayed, downloadingIds ->
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
            sortOption = sort,
            showPlayed = showPlayed,
            feedTitle = feedTitle,
            downloadingArticleIds = downloadingIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedArticlesUiState(feedTitle = feedTitle)
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

    fun addToQueue(article: Article) {
        viewModelScope.launch {
            if (article.content.isEmpty()) {
                _downloadingArticleIds.value += article.id
            }
            repository.addToQueue(article.id).onFailure { error ->
                if (error.message == "No internet connection") {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Cannot download article without internet connection"))
                }
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
}
