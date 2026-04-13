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

import android.content.Context
import android.media.MediaPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.ui.utils.HtmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsPlayer: TtsPlayer,
    private val repository: ContentRepository,
    private val settingsManager: PlaybackSettingsManager
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
        ttsPlayer.onSkipNext = { skipNext() }
        ttsPlayer.onSkipPrevious = { skipBackward() }

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

                if (playbackState == Player.STATE_ENDED) {
                    playNextArticle()
                }
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

    fun setCurrentArticle(article: Article, paragraphs: List<String>, playWhenReady: Boolean = true) {
        if (_currentArticle.value?.id != article.id) {
            val isTransition = _currentArticle.value != null
            
            _currentArticle.value = article
            _duration.value = 0L // TTS doesn't have fixed duration
            _currentPosition.value = 0L
            _currentParagraphIndex.value = 0
            _currentWordRange.value = null
            
            if (isTransition) {
                scope.launch {
                    // Request audio focus before playing chime and announcement
                    ttsPlayer.requestAudioFocus()

                    playChime()
                    // Small delay to let the chime breathe
                    delay(500)
                    
                    // Announcements usually sound better at normal speed even if the article is fast
                    val currentSpeed = _playbackSpeed.value
                    if (currentSpeed != 1.0f) ttsPlayer.setPlaybackSpeed(1.0f)
                    
                    ttsPlayer.speakAnnouncement("Now playing: ${article.title}")
                    
                    // Restore speed for the actual content
                    if (currentSpeed != 1.0f) {
                        // We need to wait for the announcement to finish or just set it back?
                        // TtsPlayer.speak clears the queue, so we should probably wait or use a callback.
                        // For now, let's just speak the article which will flush the announcement if it's too long.
                        // But we want to pause after the announcement.
                        delay(2000) // Rough estimate for title length + 1s pause
                        ttsPlayer.setPlaybackSpeed(currentSpeed)
                    } else {
                        delay(2000)
                    }
                    
                    ttsPlayer.speak(article, paragraphs, playWhenReady)
                    if (playWhenReady) ttsPlayer.play()
                }
            } else {
                ttsPlayer.speak(article, paragraphs, playWhenReady)
                if (playWhenReady) ttsPlayer.play()
            }
        }
    }

    private fun playNextArticle() {
        val finishedArticle = _currentArticle.value ?: return
        scope.launch {
            // Find the next article in the queue
            val queue = repository.getQueueArticles().first()
            val currentIndex = queue.indexOfFirst { it.id == finishedArticle.id }
            
            val nextArticle = if (currentIndex != -1 && currentIndex < queue.size - 1) {
                queue[currentIndex + 1]
            } else {
                // If it's already removed or was the last one, try to find the next one by queue order
                queue.firstOrNull { it.queueOrder > finishedArticle.queueOrder }
            }

            if (nextArticle != null) {
                repository.markAsFinished(finishedArticle.id)
                val paragraphs = HtmlParser.parse(nextArticle.content).map { it.text.toString() }
                setCurrentArticle(nextArticle, paragraphs)
            } else {
                repository.markAsFinished(finishedArticle.id)
                _currentArticle.value = null
                _isPlaying.value = false
            }
        }
    }

    private suspend fun playChime() {
        try {
            val soundName = settingsManager.chimeSound.first()
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            if (resId != 0) {
                MediaPlayer.create(context, resId)?.apply {
                    setOnCompletionListener { release() }
                    start()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Error playing chime", e)
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
        ttsPlayer.seekForward()
    }

    fun skipNext() {
        playNextArticle()
    }

    fun skipBackward() {
        // For now, just restart current article or go to previous if we track it
        val article = _currentArticle.value ?: return
        val paragraphs = HtmlParser.parse(article.content).map { it.text.toString() }
        // Reset progress
        scope.launch {
            repository.updateArticleProgress(article.id, 0f, 0, 0)
            setCurrentArticle(article.copy(currentParagraphIndex = 0, currentWordOffset = 0, progress = 0f), paragraphs)
        }
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
