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
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.Article
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val settingsManager: PlaybackSettingsManager,
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
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
    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        ttsPlayer.onSkipNext = { skipNext() }
        ttsPlayer.onSkipPrevious = { skipPrevious() }

        scope.launch {
            ttsPlayer.currentParagraphIndexFlow.collect { index ->
                _currentParagraphIndex.value = index
            }
        }

        scope.launch {
            ttsPlayer.currentWordRangeFlow.collect { range ->
                _currentWordRange.value = range
            }
        }

        scope.launch {
            settingsManager.lastArticleId.first()?.let { id ->
                val article = repository.getArticleById(id)
                if (article != null) {
                    setCurrentArticle(article, playWhenReady = false)
                }
            }
        }
    }

    private var queueArticles: List<Article> = emptyList()

    private fun startQueueSync() {
        // Observe queue changes and sync with TtsPlayer
        repository.getQueueArticles()
            .onEach { articles ->
                queueArticles = articles
                syncQueueToPlayer()
            }
            .launchIn(scope)
    }

    init {
        startQueueSync()

        scope.launch {
            ttsPlayer.engineState.collect { state ->
                if (state is TtsState.Error && state.message == "No Sherpa-ONNX model selected") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        ttsPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updateProgress()

                progressJob?.cancel()
                if (isPlaying) {
                    progressJob = scope.launch {
                        while (true) {
                            delay(1000)
                            _currentPosition.value = ttsPlayer.currentPosition
                            updateProgress()
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                _currentParagraphIndex.value = ttsPlayer.getParagraphIndex()
                _currentWordRange.value = ttsPlayer.getWordRange()
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
                            ttsPlayer.releasePlaybackResources()
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

    private var lastPersistenceTime = 0L
    private val PERSISTENCE_THRESHOLD_MS = 5000L

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

        val currentPara = ttsPlayer.getParagraphIndex()
        val currentRange = ttsPlayer.getWordRange()
        val currentWordOffset = if (playbackState == Player.STATE_ENDED) 0 else currentRange?.first ?: 0

        val currentTime = System.currentTimeMillis()
        if (playbackState == Player.STATE_ENDED || currentTime - lastPersistenceTime >= PERSISTENCE_THRESHOLD_MS) {
            lastPersistenceTime = currentTime
            scope.launch {
                withContext(kotlinx.coroutines.NonCancellable) {
                    repository.updateArticleProgress(
                        article.id,
                        progress,
                        if (playbackState == Player.STATE_ENDED) 0 else currentPara,
                        currentWordOffset,
                    )
                }
            }
        }
    }

    private fun syncQueueToPlayer() {
        if (queueArticles.isEmpty()) return

        val currentItems = (0 until ttsPlayer.mediaItemCount).map { ttsPlayer.getMediaItemAt(it) }
        val newItems = queueArticles.map { article ->
            val artworkUrl = article.imageUrl ?: article.feedImageUrl
            val artworkUri = artworkUrl?.takeIf { it.startsWith("http") }?.toUri()
            MediaItem.Builder()
                .setMediaId(article.id)
                .setUri("tts://${article.id}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(article.title)
                        .setSubtitle(article.source)
                        .setArtist(article.source)
                        .setAlbumTitle(article.source)
                        .setAlbumArtist(article.source)
                        .setDisplayTitle(article.title)
                        .setArtworkUri(artworkUri)
                        .setIsPlayable(true)
                        .build(),
                )
                .build()
        }

        // Simple sync logic: if the IDs don't match, replace the playlist
        // In a more advanced version, we'd use move/add/remove for smoother transitions
        val currentIds = currentItems.map { it.mediaId }
        val newIds = newItems.map { it.mediaId }

        if (currentIds != newIds) {
            val currentIndex = ttsPlayer.currentMediaItemIndex
            val currentId = if (currentIndex >= 0 && currentIndex < currentIds.size) currentIds[currentIndex] else null

            ttsPlayer.setMediaItems(newItems)

            // Try to restore current item index if it's still in the queue
            val newIndex = newIds.indexOf(currentId)
            if (newIndex != -1) {
                ttsPlayer.seekTo(newIndex, ttsPlayer.currentPosition)
            }
        }
    }

    /**
     * Reloads the last played article into the TtsPlayer.
     * This is useful for session resumption after the app has been killed.
     */
    suspend fun reloadLastArticle(): Boolean {
        val lastId = settingsManager.lastArticleId.firstOrNull() ?: return false
        val article = repository.getArticleById(lastId) ?: return false

        setCurrentArticle(article, playWhenReady = false, isAutomatic = false)
        return true
    }

    private fun startPlaybackService(action: String? = null) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            if (action != null) this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setCurrentArticle(
        article: Article,
        blocks: List<ContentBlock>? = null,
        playWhenReady: Boolean = true,
        isAutomatic: Boolean = false,
    ) {
        val isSameArticle = _currentArticle.value?.id == article.id

        if (!isSameArticle) {
            transitionJob?.cancel()

            _currentArticle.value = article
            scope.launch {
                repository.updateArticleProgress(
                    article.id,
                    article.progress ?: 0f,
                    article.currentParagraphIndex,
                    article.currentWordOffset,
                    article.duration,
                )
                settingsManager.setLastArticleId(article.id)
            }
            _duration.value = 0L // TTS doesn't have fixed duration
            _currentPosition.value = 0L
            _currentParagraphIndex.value = article.currentParagraphIndex
            _currentWordRange.value = article.currentWordOffset.let { it until it + 1 } // Placeholder until TtsPlayer refines it

            transitionJob = scope.launch {
                val actualBlocks = withContext(Dispatchers.Default) {
                    blocks ?: HtmlParser.parse(article.content)
                }

                val readAltText = settingsManager.readAltText.first()
                val shortenHyperlinks = settingsManager.shortenHyperlinks.first()

                val ttsTexts = withContext(Dispatchers.Default) {
                    actualBlocks.map { block ->
                        when (block) {
                            is ContentBlock.Image -> if (readAltText) {
                                block.altText?.let { context.getString(R.string.reader_image_prefix, it) } ?: ""
                            } else {
                                ""
                            }
                            else -> block.text.toSpeakableText(context, shortenLinks = shortenHyperlinks)
                        }
                    }
                }

                if (isAutomatic && playWhenReady) {
                    // Set up player state BEFORE starting the service to ensure Media3 has metadata immediately
                    ttsPlayer.speak(article, ttsTexts, playWhenReady = false)
                    startPlaybackService()

                    val playChimeAndTitle = settingsManager.playChimeAndTitle.first()

                    if (playChimeAndTitle) {
                        try {
                            // Request audio focus before playing chime and announcement
                            ttsPlayer.requestAudioFocus()

                            playChime()
                            // Small delay to let the chime breathe
                            delay(300)

                            // Announcements usually sound better at normal speed even if the article is fast
                            val currentSpeed = _playbackSpeed.value
                            if (currentSpeed != 1.0f) ttsPlayer.setPlaybackSpeed(1.0f)

                            ttsPlayer.speakAnnouncement(context.getString(R.string.reader_now_playing, article.title))

                            // Wait for the announcement to start and then finish
                            // Increased timeout to 10s for slow TTS engines
                            withTimeoutOrNull(10000) {
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
                        } catch (e: Exception) {
                            android.util.Log.e("PlaybackManager", "Error during autoplay transition", e)
                        }
                    }

                    // Now start the actual article content
                    ttsPlayer.play()
                } else {
                    if (playWhenReady) {
                        ttsPlayer.speak(article, ttsTexts, playWhenReady = true)
                        startPlaybackService()
                    } else {
                        ttsPlayer.speak(article, ttsTexts, playWhenReady = false)
                    }
                }
            }
        } else {
            // If it's the same article, just update the article metadata (like favorite status)
            // but DON'T reset the paragraph index or position.
            _currentArticle.value = article

            // If playWhenReady is true and we're not already playing, start it
            if (playWhenReady && !ttsPlayer.isPlaying) {
                startPlaybackService()
                ttsPlayer.play()
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
                withContext(kotlinx.coroutines.NonCancellable) {
                    repository.markAsFinished(finishedArticle.id)
                }
                setCurrentArticle(nextArticle, isAutomatic = isAutomatic)
            } else {
                withContext(kotlinx.coroutines.NonCancellable) {
                    repository.markAsFinished(finishedArticle.id)
                }
                _currentArticle.value = null
                _isPlaying.value = false
                // Queue finished, stop player and release locks
                ttsPlayer.stop()
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
        ttsPlayer.stop() // Use stop() instead of pause() to release locks
        _currentArticle.value = null
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (!ttsPlayer.isPlaying) {
            startPlaybackService()
        }
        if (ttsPlayer.isPlaying || transitionJob?.isActive == true) {
            transitionJob?.cancel()
            ttsPlayer.pause()
        } else {
            ttsPlayer.play()
        }
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
            scope.launch {
                val blocks = withContext(Dispatchers.Default) {
                    HtmlParser.parse(article.content)
                }

                val readAltText = settingsManager.readAltText.first()
                val shortenHyperlinks = settingsManager.shortenHyperlinks.first()

                val ttsTexts = withContext(Dispatchers.Default) {
                    blocks.map { block ->
                        if (block is ContentBlock.Image) {
                            if (readAltText) {
                                block.altText?.let { context.getString(R.string.reader_image_prefix, it) } ?: ""
                            } else {
                                ""
                            }
                        } else {
                            block.text.toSpeakableText(context, shortenLinks = shortenHyperlinks)
                        }
                    }
                }
                repository.updateArticleProgress(article.id, 0f, 0, 0)
                ttsPlayer.speak(
                    article.copy(
                        currentParagraphIndex = 0,
                        currentWordOffset = 0,
                        progress = 0f,
                    ),
                    ttsTexts,
                    ttsPlayer.playWhenReady,
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
                setCurrentArticle(prevArticle)
            } else {
                // Restart current if no previous
                val blocks = withContext(Dispatchers.Default) {
                    HtmlParser.parse(current.content)
                }

                val readAltText = settingsManager.readAltText.first()
                val shortenHyperlinks = settingsManager.shortenHyperlinks.first()

                val ttsTexts = withContext(Dispatchers.Default) {
                    blocks.map { block ->
                        if (block is ContentBlock.Image) {
                            if (readAltText) {
                                block.altText?.let { context.getString(R.string.reader_image_prefix, it) } ?: ""
                            } else {
                                ""
                            }
                        } else {
                            block.text.toSpeakableText(context, shortenLinks = shortenHyperlinks)
                        }
                    }
                }
                repository.updateArticleProgress(current.id, 0f, 0, 0)
                ttsPlayer.speak(
                    current.copy(
                        currentParagraphIndex = 0,
                        currentWordOffset = 0,
                        progress = 0f,
                    ),
                    ttsTexts,
                    ttsPlayer.playWhenReady,
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

    /**
     * Handles hardware media button presses for Next/Previous based on settings.
     */
    fun handleHardwareButton(isNext: Boolean) {
        scope.launch {
            if (isNext) {
                val action = settingsManager.fastForwardHardwareButton.first()
                android.util.Log.d("PlaybackManager", "Handling hardware Next: $action")
                when (action) {
                    "skip_article" -> skipNext()
                    "fast_forward" -> skipForward()
                    else -> skipNext()
                }
            } else {
                val action = settingsManager.rewindHardwareButton.first()
                android.util.Log.d("PlaybackManager", "Handling hardware Previous: $action")
                when (action) {
                    "previous_article" -> skipPrevious()
                    "rewind" -> skipBackward()
                    else -> skipPrevious()
                }
            }
        }
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
