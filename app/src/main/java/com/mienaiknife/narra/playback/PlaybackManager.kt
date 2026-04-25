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

import androidx.core.content.ContextCompat
import com.mienaiknife.narra.R
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.service.PlaybackService
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.ui.utils.toSpeakableText
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
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
@Singleton
class PlaybackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ttsPlayer: TtsPlayer,
    private val repository: ContentRepository,
    val settingsManager: PlaybackSettingsManager
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

    private val _sleepTimerMillisLeft = MutableStateFlow<Long?>(null)
    val sleepTimerMillisLeft: StateFlow<Long?> = _sleepTimerMillisLeft.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var transitionJob: kotlinx.coroutines.Job? = null

    init {
        ttsPlayer.onSkipNext = { skipNext() }
        ttsPlayer.onSkipPrevious = { skipPrevious() }

        scope.launch {
            settingsManager.lastArticleId.first()?.let { id ->
                val article = repository.getArticleById(id)
                if (article != null) {
                    val blocks = HtmlParser.parse(article.content)
                    setCurrentArticle(article, blocks, playWhenReady = false)
                }
            }
        }

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
                    scope.launch {
                        if (settingsManager.autoPlayNext.first()) {
                            playNextArticle(isAutomatic = true)
                        } else {
                            _isPlaying.value = false
                        }
                    }
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
                currentWordOffset,
                duration
            )
        }
    }

    fun setCurrentArticle(
        article: Article,
        blocks: List<ContentBlock>,
        playWhenReady: Boolean = true,
        isAutomatic: Boolean = false
    ) {
        if (_currentArticle.value?.id != article.id) {
            transitionJob?.cancel()
            
            _currentArticle.value = article
            scope.launch {
                settingsManager.setLastArticleId(article.id)
            }
            _duration.value = 0L // TTS doesn't have fixed duration
            _currentPosition.value = 0L
            _currentParagraphIndex.value = 0
            _currentWordRange.value = null

            val ttsTexts = blocks.map { block ->
                when (block) {
                    is ContentBlock.Image -> block.altText?.let { "Image: $it" } ?: ""
                    else -> block.text.toSpeakableText()
                }
            }

            // Ensure PlaybackService is running so MediaSession is active
            try {
                ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "Failed to start PlaybackService", e)
            }
            
            if (isAutomatic && playWhenReady) {
                // Prepare TtsPlayer with the new article but don't start playing yet
                ttsPlayer.speak(article, ttsTexts, playWhenReady = false)

                transitionJob = scope.launch {
                    val playChimeAndTitle = settingsManager.playChimeAndTitle.first()

                    if (playChimeAndTitle) {
                        // Request audio focus before playing chime and announcement
                        ttsPlayer.requestAudioFocus()

                        playChime()
                        // Small delay to let the chime breathe
                        delay(300)

                        // Announcements usually sound better at normal speed even if the article is fast
                        val currentSpeed = _playbackSpeed.value
                        if (currentSpeed != 1.0f) ttsPlayer.setPlaybackSpeed(1.0f)

                        ttsPlayer.speakAnnouncement("Now playing: ${article.title}")

                        // Wait for the announcement to start and then finish
                        withTimeoutOrNull(5000) {
                            // Wait for Speaking state with "announcement" ID
                            ttsPlayer.engineState.first {
                                it is TtsState.Speaking && it.utteranceId == "announcement"
                            }
                            // Then wait for it to be Ready (meaning it finished)
                            ttsPlayer.engineState.first { it is TtsState.Ready }
                        }

                        delay(500) // Brief pause after announcement

                        // Restore speed for the actual content
                        if (currentSpeed != 1.0f) {
                            ttsPlayer.setPlaybackSpeed(currentSpeed)
                        }
                    }
                    
                    // Now start the actual article content
                    ttsPlayer.play()
                }
            } else {
                if (playWhenReady) {
                    scope.launch {
                        // Small delay to ensure the service is started and connected
                        // before the player transitions to READY and triggers foreground update.
                        delay(100)
                        ttsPlayer.speak(article, ttsTexts, playWhenReady = true)
                    }
                } else {
                    ttsPlayer.speak(article, ttsTexts, playWhenReady = false)
                }
            }
        }
    }

    private fun playNextArticle(isAutomatic: Boolean = false) {
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
                val blocks = HtmlParser.parse(nextArticle.content)
                setCurrentArticle(nextArticle, blocks, isAutomatic = isAutomatic)
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
            val resId = when (soundName) {
                "music_box_chime_positive" -> R.raw.music_box_chime_positive
                "vibraphone_chime_positive" -> R.raw.vibraphone_chime_positive
                else -> 0
            }
            if (resId != 0) {
                val mediaPlayer = MediaPlayer.create(context, resId) ?: return
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    mediaPlayer.setOnCompletionListener { 
                        it.release()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    mediaPlayer.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        if (continuation.isActive) continuation.resume(Unit)
                        true
                    }
                    mediaPlayer.start()
                    continuation.invokeOnCancellation {
                        mediaPlayer.release()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Error playing chime", e)
        }
    }

    fun stop() {
        transitionJob?.cancel()
        ttsPlayer.pause()
        _currentArticle.value = null
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (ttsPlayer.isPlaying || transitionJob?.isActive == true) {
            transitionJob?.cancel()
            ttsPlayer.pause()
        } else {
            // Ensure PlaybackService is running when starting playback
            try {
                ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "Failed to start PlaybackService", e)
            }
            
            scope.launch {
                // Small delay to ensure the service is started and connected
                delay(100)
                ttsPlayer.play()
            }
        }
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position
        ttsPlayer.seekTo(position)
    }

    fun seekToParagraph(index: Int) {
        _currentParagraphIndex.value = index
        _currentWordRange.value = null
        ttsPlayer.seekToParagraph(index)
    }

    fun seekToWord(paragraphIndex: Int, wordRange: IntRange) {
        _currentParagraphIndex.value = paragraphIndex
        _currentWordRange.value = wordRange
        ttsPlayer.seekToWord(paragraphIndex, wordRange)
    }

    fun skipForward() {
        ttsPlayer.seekForward()
    }

    fun skipBackward() {
        ttsPlayer.seekBack()
    }

    fun skipNext() {
        playNextArticle()
    }

    fun skipPrevious() {
        val article = _currentArticle.value ?: return
        // If we are more than 3 seconds in, just restart current article
        if (ttsPlayer.currentPosition > 3000) {
            val blocks = HtmlParser.parse(article.content)
            val ttsTexts = blocks.map { block ->
                if (block is ContentBlock.Image) {
                    block.altText?.let { "Image: $it" } ?: ""
                } else {
                    block.text.toSpeakableText()
                }
            }
            scope.launch {
                repository.updateArticleProgress(article.id, 0f, 0, 0)
                ttsPlayer.speak(
                    article.copy(currentParagraphIndex = 0, currentWordOffset = 0, progress = 0f),
                    ttsTexts,
                    ttsPlayer.playWhenReady
                )
            }
        } else {
            playPreviousArticle()
        }
    }

    private fun playPreviousArticle() {
        val current = _currentArticle.value ?: return
        scope.launch {
            val queue = repository.getQueueArticles().first()
            val currentIndex = queue.indexOfFirst { it.id == current.id }
            
            val prevArticle = if (currentIndex > 0) {
                queue[currentIndex - 1]
            } else {
                null
            }

            if (prevArticle != null) {
                val blocks = HtmlParser.parse(prevArticle.content)
                setCurrentArticle(prevArticle, blocks)
            } else {
                // Restart current if no previous
                val blocks = HtmlParser.parse(current.content)
                val ttsTexts = blocks.map { block ->
                    if (block is ContentBlock.Image) {
                        block.altText?.let { "Image: $it" } ?: ""
                    } else {
                        block.text.toSpeakableText()
                    }
                }
                repository.updateArticleProgress(current.id, 0f, 0, 0)
                ttsPlayer.speak(
                    current.copy(currentParagraphIndex = 0, currentWordOffset = 0, progress = 0f),
                    ttsTexts,
                    ttsPlayer.playWhenReady
                )
            }
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

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            _sleepTimerMillisLeft.value = null
            return
        }

        _sleepTimerMillisLeft.value = minutes * 60 * 1000L
        sleepTimerJob = scope.launch {
            while ((_sleepTimerMillisLeft.value ?: 0) > 0) {
                delay(1000)
                _sleepTimerMillisLeft.value = (_sleepTimerMillisLeft.value ?: 0) - 1000
            }
            _sleepTimerMillisLeft.value = 0
            ttsPlayer.pause()
        }
    }
}
