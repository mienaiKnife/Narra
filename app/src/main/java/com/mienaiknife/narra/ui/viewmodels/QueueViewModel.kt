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
class QueueViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val currentArticle = playbackManager.currentArticle
    val isPlaying = playbackManager.isPlaying

    // For now, the queue is just all articles. 
    // In the future, this might filter for a specific "queue" status.
    val articles: StateFlow<List<Article>> = repository.getQueueArticles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onPlayPauseClick(article: Article) {
        if (currentArticle.value?.id == article.id) {
            playbackManager.togglePlayPause()
        } else {
            val paragraphs = HtmlParser.parse(article.content).map { it.text.toString() }
            playbackManager.setCurrentArticle(article, paragraphs)
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
                repository.addToQueue(article.id)
            } else {
                val isCurrentlyPlaying = currentArticle.value?.id == article.id
                val nextArticle = if (isCurrentlyPlaying) {
                    val currentList = articles.value
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
            repository.addToQueue(articleId)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderQueue(fromIndex, toIndex)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshFeeds()
        }
    }
}
