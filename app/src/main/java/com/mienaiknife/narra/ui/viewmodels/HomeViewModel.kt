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
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.domain.repository.ModelRepository
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
class HomeViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            modelRepository.ensureDefaultModelsInitialized()
        }
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val uiText: UiText) : UiEvent()
        object ArticleAdded : UiEvent()
        data class FeedSubscribed(val feedName: String) : UiEvent()
        object EpubImported : UiEvent()
    }

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getQueueArticles(),
        repository.getInboxArticles(),
        repository.getFavoriteArticles(),
        _isRefreshing
    ) { queue, inbox, favorites, isRefreshing ->
        HomeUiState(
            continueListening = queue
                .filter { (it.progress ?: 0f) > 0f && (it.progress ?: 0f) < 1f }
                .sortedByDescending { it.publishedTimestamp ?: 0L }
                .take(10),
            newFromFeeds = inbox
                .filter { (it.progress ?: 0f) < 1f }
                .sortedByDescending { it.publishedTimestamp ?: 0L }
                .take(5),
            favoriteArticles = favorites.take(10),
            isLoading = false,
            isRefreshing = isRefreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshFeeds().onFailure { error ->
                val uiText = error.message?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.error_refresh_failed)
                _uiEvent.emit(UiEvent.ShowSnackbar(uiText))
            }
            _isRefreshing.value = false
        }
    }

    fun importEpub(inputStream: java.io.InputStream, title: String) {
        viewModelScope.launch {
            repository.importEpub(inputStream, title)
                .onSuccess {
                    _uiEvent.emit(UiEvent.EpubImported)
                }
                .onFailure {
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.error_import_epub_failed)))
                }
        }
    }

    fun downloadArticle(url: String) {
        viewModelScope.launch {
            repository.downloadWebPage(url)
                .onSuccess {
                    _uiEvent.emit(UiEvent.ArticleAdded)
                }
                .onFailure { error ->
                    val uiText = when (error.message) {
                        "Article already in queue" -> {
                            UiText.StringResource(R.string.error_article_already_in_queue)
                        }
                        "No internet connection" -> {
                            UiText.StringResource(R.string.error_no_internet)
                        }
                        else -> {
                            UiText.StringResource(R.string.error_download_failed)
                        }
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar(uiText))
                }
        }
    }

    fun subscribeToFeed(url: String) {
        viewModelScope.launch {
            repository.subscribeToFeed(url)
                .onSuccess { feedName ->
                    _uiEvent.emit(UiEvent.FeedSubscribed(feedName))
                }
                .onFailure {
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.error_subscribe_failed)))
                }
        }
    }
}
