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
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val article: StateFlow<Article?> = playbackManager.currentArticle
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPosition: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.duration
    val playbackSpeed: StateFlow<Float> = playbackManager.playbackSpeed
    val currentParagraphIndex: StateFlow<Int> = playbackManager.currentParagraphIndex
    val currentWordRange: StateFlow<IntRange?> = playbackManager.currentWordRange

    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    val blocks: StateFlow<List<ContentBlock>> = _blocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Automatically update blocks when the article changes in the PlaybackManager
        playbackManager.currentArticle
            .onEach { art ->
                if (art != null) {
                    val parsedBlocks = HtmlParser.parse(art.content)
                    _blocks.value = parsedBlocks
                } else {
                    _blocks.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadArticle(id: String) {
        if (playbackManager.currentArticle.value?.id == id) return

        viewModelScope.launch {
            _isLoading.value = true
            var articleData = repository.getArticleById(id)
            if (articleData != null) {
                if (!articleData.isInQueue) {
                    repository.addToQueue(id)
                    // Refresh data after adding to queue (which might have downloaded content)
                    articleData = repository.getArticleById(id)
                }

                if (articleData != null) {
                    // This triggers the Flow in PlaybackManager, which our init block observes
                    val paragraphs = HtmlParser.parse(articleData.content).map { it.text.toString() }
                    playbackManager.setCurrentArticle(articleData, paragraphs, playWhenReady = false)
                }
            }
            _isLoading.value = false
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekTo(position: Long) = playbackManager.seekTo(position)
    fun seekToParagraph(index: Int) = playbackManager.seekToParagraph(index)
    fun seekToWord(paragraphIndex: Int, wordRange: IntRange) = playbackManager.seekToWord(paragraphIndex, wordRange)
    fun skipForward() = playbackManager.skipForward()
    fun skipBackward() = playbackManager.skipBackward()
    fun skipNext() = playbackManager.skipNext()
    fun cycleSpeed() = playbackManager.cycleSpeed()

    fun toggleFavorite() {
        article.value?.let { art ->
            viewModelScope.launch {
                repository.toggleFavorite(art.id)
                // The article Flow in loadArticle will automatically emit the updated article
            }
        }
    }
}
