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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ContentRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object ArticleAdded : UiEvent()
        data class FeedSubscribed(val feedName: String) : UiEvent()
    }

    val articles: StateFlow<List<Article>> = repository.getQueueArticles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun downloadArticle(url: String) {
        viewModelScope.launch {
            repository.downloadWebPage(url)
                .onSuccess {
                    _uiEvent.emit(UiEvent.ArticleAdded)
                }
                .onFailure { error ->
                    if (error.message == "Article already in queue") {
                        _uiEvent.emit(UiEvent.ShowSnackbar("Article is already in your queue"))
                    } else {
                        _uiEvent.emit(UiEvent.ShowSnackbar("Failed to download article"))
                    }
                }
        }
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch {
            repository.removeFromQueue(id)
        }
    }

    fun subscribeToFeed(url: String) {
        viewModelScope.launch {
            repository.subscribeToFeed(url)
                .onSuccess { feedName ->
                    _uiEvent.emit(UiEvent.FeedSubscribed(feedName))
                }
                .onFailure {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Failed to subscribe to feed"))
                }
        }
    }
}
