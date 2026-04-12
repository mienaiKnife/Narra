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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val currentArticle = playbackManager.currentArticle
    val isPlaying = playbackManager.isPlaying

    val articles: StateFlow<List<Article>> = repository.getInboxArticles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onPlayPauseClick(article: Article) {
        if (!article.isInQueue) {
            addToQueue(article)
        } else {
            if (currentArticle.value?.id == article.id) {
                playbackManager.togglePlayPause()
            } else {
                val paragraphs = HtmlParser.parse(article.content).map { it.text.toString() }
                playbackManager.setCurrentArticle(article, paragraphs)
            }
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshFeeds()
        }
    }

    fun addToQueue(article: Article) {
        viewModelScope.launch {
            repository.addToQueue(article.id)
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
