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

package com.mienaiknife.narra.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val ttsPlayer: TtsPlayer,
    private val repository: ContentRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _currentArticle = MutableStateFlow<Article?>(null)
    val currentArticle: StateFlow<Article?> = _currentArticle.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()

    private val _currentWordRange = MutableStateFlow<IntRange?>(null)
    val currentWordRange: StateFlow<IntRange?> = _currentWordRange.asStateFlow()

    init {
        ttsPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updateProgress()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentParagraphIndex.value = ttsPlayer.getCurrentParagraphIndex()
                _currentWordRange.value = ttsPlayer.getCurrentWordRange()
                _currentPosition.value = ttsPlayer.currentPosition
                updateProgress()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _duration.value = ttsPlayer.duration
                updateProgress()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                _duration.value = ttsPlayer.duration
                updateProgress()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _duration.value = ttsPlayer.duration
                _currentPosition.value = ttsPlayer.currentPosition
                updateProgress()
            }
        })
    }

    private fun updateProgress() {
        val article = _currentArticle.value ?: return
        val duration = ttsPlayer.duration
        if (duration <= 0) return

        val playbackState = ttsPlayer.playbackState
        val currentPosition = ttsPlayer.currentPosition
        
        val progress = if (playbackState == Player.STATE_ENDED) {
            1f
        } else {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 0.999f)
        }

        val currentPara = ttsPlayer.getCurrentParagraphIndex()
        val currentRange = ttsPlayer.getCurrentWordRange()
        val currentWordOffset = if (playbackState == Player.STATE_ENDED) 0 else currentRange?.first ?: 0

        scope.launch {
            repository.updateArticleProgress(
                article.id,
                progress,
                if (playbackState == Player.STATE_ENDED) 0 else currentPara,
                currentWordOffset
            )
        }
    }

    fun setCurrentArticle(article: Article, paragraphs: List<String>) {
        if (_currentArticle.value?.id != article.id) {
            _currentArticle.value = article
            _duration.value = 0L // TTS doesn't have fixed duration
            _currentPosition.value = 0L
            _currentParagraphIndex.value = 0
            _currentWordRange.value = null
            
            ttsPlayer.speak(article, paragraphs)
            ttsPlayer.play()
        }
    }

    fun stop() {
        ttsPlayer.pause()
        _currentArticle.value = null
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (ttsPlayer.isPlaying) {
            ttsPlayer.pause()
        } else {
            ttsPlayer.play()
        }
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position
        ttsPlayer.seekTo(position)
    }

    fun seekToParagraph(index: Int) {
        _currentParagraphIndex.value = index
        _currentWordRange.value = null
        // TODO: Logic to seek TTS to specific paragraph
    }

    fun seekToWord(paragraphIndex: Int, wordRange: IntRange) {
        _currentParagraphIndex.value = paragraphIndex
        _currentWordRange.value = wordRange
        ttsPlayer.seekToWord(paragraphIndex, wordRange)
    }

    fun skipForward() {
        // TODO: Skip logic for TTS
    }

    fun skipBackward() {
        // TODO: Skip logic for TTS
    }

    fun cycleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.75f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        ttsPlayer.setPlaybackSpeed(nextSpeed)
    }
}
