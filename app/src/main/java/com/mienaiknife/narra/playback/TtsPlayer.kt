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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.core.net.toUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import java.io.ByteArrayOutputStream

/**
 * A custom Media3 Player implementation that wraps a TtsEngine using SimpleBasePlayer.
 */
@UnstableApi
@Singleton
class TtsPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ttsEngine: TtsEngine,
    settingsManager: PlaybackSettingsManager,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null

    val engineState: StateFlow<TtsState> = ttsEngine.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _playWhenReady = false
    private var _playbackState = STATE_IDLE
    private var _currentMediaItem: MediaItem? = null
    private var _playbackParameters = PlaybackParameters.DEFAULT
    private var _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
    private var _playerError: PlaybackException? = null

    private var paragraphs: List<String> = emptyList()
    private val _currentParagraphIndexFlow = MutableStateFlow(0)
    val currentParagraphIndexFlow = _currentParagraphIndexFlow.asStateFlow()
    var currentParagraphIndex: Int
        get() = _currentParagraphIndexFlow.value
        set(value) { _currentParagraphIndexFlow.value = value }

    private val _currentWordRangeFlow = MutableStateFlow<IntRange?>(null)
    val currentWordRangeFlow = _currentWordRangeFlow.asStateFlow()
    var currentWordRange: IntRange?
        get() = _currentWordRangeFlow.value
        set(value) { _currentWordRangeFlow.value = value }

    private var resumeWordOffset = 0
    private var baseWordOffset = 0
    private var isEngineSpeaking = false
    private var isPreparing = false
    private var _pauseForInterruptions = true
    private var lastEnqueuedUtteranceId: String? = null

    private var _seekForwardIncrement = 15000L
    private var _seekBackIncrement = 15000L

    private val audioFocusManager = AudioFocusManager(context) { hasFocus, shouldDuck ->
        if (hasFocus) {
            _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
            if (_playWhenReady) {
                resumeInternal()
            }
        } else {
            if (shouldDuck && !_pauseForInterruptions) {
                // Ducking handled by engine/system
            } else {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                pauseInternal()
            }
        }
        invalidateState()
    }

    private val powerLockManager = PowerLockManager(context)
    private val noisyAudioReceiver = NoisyAudioReceiver(context) {
        _playWhenReady = false
        pauseInternal()
        invalidateState()
    }

    init {
        ttsEngine.state.onEach { state ->
            when (state) {
                is TtsState.Speaking -> {
                    isEngineSpeaking = true
                    val index = state.utteranceId.toIntOrNull()
                    if (index != null) {
                        if (index != currentParagraphIndex) {
                            baseWordOffset = 0
                            currentParagraphIndex = index
                        }
                        
                        val absoluteStart = baseWordOffset + state.start
                        val absoluteEnd = baseWordOffset + state.end
                        
                        currentWordRange = absoluteStart until absoluteEnd
                        resumeWordOffset = absoluteStart
                        invalidateState()
                    }
                }
                is TtsState.Ready -> {
                    isEngineSpeaking = false
                    if (_playWhenReady && _playbackState == STATE_READY && !isPreparing) {
                        if (lastEnqueuedUtteranceId != null &&
                            currentParagraphIndex.toString() == lastEnqueuedUtteranceId
                        ) {
                            _playbackState = STATE_ENDED
                            _playWhenReady = false
                            // Don't abandon audio focus or release locks here.
                            // The caller decides when to clean up via stop() or
                            // starting new content, so autoplay transitions don't
                            // lose audio focus or CPU wake locks in the background.
                            unregisterNoisyReceiver()
                            invalidateState()
                        }
                    }
                }
                is TtsState.Error -> {
                    isEngineSpeaking = false
                    _playerError = PlaybackException(state.message, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
                    invalidateState()
                }
                else -> {
                    isEngineSpeaking = false
                }
            }
        }.launchIn(scope)

        settingsManager.fastForwardSkipTime.onEach { _seekForwardIncrement = parseSkipTime(it) }.launchIn(scope)
        settingsManager.rewindSkipTime.onEach { _seekBackIncrement = parseSkipTime(it) }.launchIn(scope)
        settingsManager.pauseForInterruptions.onEach { _pauseForInterruptions = it }.launchIn(scope)
    }

    private fun parseSkipTime(time: String): Long {
        return time.filter { it.isDigit() }.toLongOrNull()?.let { it * 1000L } ?: 15000L
    }

    private var _audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .build()

    override fun getState(): State {
        val playlist = if (_currentMediaItem != null || mediaItems.isNotEmpty() || isPreparing) {
            val itemsToUse = if (mediaItems.isNotEmpty()) mediaItems else listOfNotNull(_currentMediaItem)
            
            if (itemsToUse.isEmpty() && isPreparing) {
                // Return a dummy item while preparing to ensure notification can be shown
                val dummyItem = MediaItem.Builder()
                    .setMediaId("preparing")
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle("Loading...")
                        .build())
                    .build()
                listOf(
                    MediaItemData.Builder(dummyItem)
                        .setUid(dummyItem.mediaId)
                        .build()
                )
            } else {
                (0 until itemsToUse.size).map { i ->
                    val item = itemsToUse[i]
                    val durationUs = if (i == currentItemIndex && paragraphs.isNotEmpty()) {
                        paragraphs.size * 1000000L
                    } else {
                        C.TIME_UNSET
                    }
                    
                    MediaItemData.Builder(item)
                        .setUid(item.mediaId)
                        .setDurationUs(durationUs)
                        .setIsSeekable(true)
                        .setMediaMetadata(item.mediaMetadata)
                        .build()
                }
            }
        } else {
            emptyList()
        }

        val currentPositionMs = if (currentParagraphIndex >= 0 && paragraphs.isNotEmpty()) {
            val progress = currentWordRange?.let { it.last.toFloat() / paragraphs[currentParagraphIndex].length.toFloat() }
                ?: (resumeWordOffset.toFloat() / paragraphs[currentParagraphIndex].length.toFloat()).coerceIn(0f, 1f)
            (currentParagraphIndex * 1000L + (progress * 1000L).toLong())
        } else 0L

        val currentPlaybackState = if (playlist.isEmpty()) {
            if (isPreparing) STATE_BUFFERING else STATE_IDLE
        } else if (isPreparing) {
            STATE_BUFFERING
        } else if (_playbackState == STATE_IDLE && playlist.isNotEmpty()) {
            // Force READY if we have a media item loaded, even if not playing.
            // This ensures the session is seen as "active" by the system.
            STATE_READY
        } else {
            _playbackState
        }

        android.util.Log.d("TtsPlayer", "getState(): state=$_playbackState -> $currentPlaybackState, playWhenReady=$_playWhenReady, hasPlaylist=${playlist.isNotEmpty()}, isSpeaking=$isEngineSpeaking")

        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                        Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD,
                        Player.COMMAND_SET_SPEED_AND_PITCH,
                        Player.COMMAND_SET_SHUFFLE_MODE,
                        Player.COMMAND_SET_REPEAT_MODE,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_SET_PLAYLIST_METADATA,
                        Player.COMMAND_SET_MEDIA_ITEM,
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                        Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                        Player.COMMAND_GET_VOLUME,
                        Player.COMMAND_GET_DEVICE_VOLUME,
                        Player.COMMAND_SET_VOLUME,
                        Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                        Player.COMMAND_GET_TRACKS,
                        Player.COMMAND_RELEASE,
                    ).build(),
            )
            .setPlayWhenReady(_playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(currentPlaybackState)
            .setPlaybackParameters(_playbackParameters)
            .setPlaybackSuppressionReason(_playbackSuppressionReason)
            .setPlayerError(_playerError)
            .setAudioAttributes(_audioAttributes)
            .setPlaylist(playlist)
            .setPlaylistMetadata(playlist.getOrNull(currentItemIndex)?.mediaMetadata ?: MediaMetadata.EMPTY)
            .setCurrentMediaItemIndex(currentItemIndex)
            .setContentPositionMs(currentPositionMs)
            .setSeekBackIncrementMs(_seekBackIncrement)
            .setSeekForwardIncrementMs(_seekForwardIncrement)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        android.util.Log.d("TtsPlayer", "handleSetPlayWhenReady: current=$_playWhenReady, new=$playWhenReady")
        if (_playWhenReady == playWhenReady) {
            return Futures.immediateVoidFuture()
        }
        
        _playWhenReady = playWhenReady
        if (playWhenReady) {
            val focusResult = audioFocusManager.requestAudioFocus()
            android.util.Log.d("TtsPlayer", "handleSetPlayWhenReady: requestAudioFocus result=$focusResult")
            if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                resumeInternal()
            } else if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            } else {
                _playWhenReady = false
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            }
        } else {
            pauseInternal()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        if (_playbackState == STATE_IDLE) {
            _playbackState = STATE_BUFFERING
            invalidateState()
            
            // In a real TTS player, prepare might fetch content.
            // Here we assume speak() handles it.
            _playbackState = STATE_READY
            invalidateState()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        _playWhenReady = false
        _playbackState = STATE_IDLE
        pauseInternal()
        ttsEngine.stop()
        paragraphs = emptyList()
        currentParagraphIndex = 0
        resumeWordOffset = 0
        baseWordOffset = 0
        currentWordRange = null
        releaseLocks()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        _playbackParameters = playbackParameters
        ttsEngine.setPlaybackSpeed(playbackParameters.speed)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    private val mediaItems: MutableList<MediaItem> = mutableListOf()
    private var currentItemIndex = 0

    override fun handleSetMediaItems(items: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        mediaItems.clear()
        mediaItems.addAll(items)
        currentItemIndex = startIndex.coerceIn(0, items.size - 1).takeIf { items.isNotEmpty() } ?: 0
        
        // This is a stub for the full playlist support. 
        // Real implementation would trigger speak() for the selected item.
        
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        if (mediaItemIndex != currentItemIndex) {
            // Skip to next/previous article
            if (mediaItemIndex > currentItemIndex) {
                onSkipNext?.invoke()
            } else {
                onSkipPrevious?.invoke()
            }
            return Futures.immediateVoidFuture()
        }

        seekToPosition(positionMs)
        
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    private fun seekToPosition(positionMs: Long) {
        if (paragraphs.isEmpty()) return
        
        val totalMs = paragraphs.size * 1000L
        val clampedPos = positionMs.coerceIn(0, totalMs - 1)
        val pIndex = (clampedPos / 1000).toInt().coerceIn(0, paragraphs.size - 1)
        val progress = (clampedPos % 1000) / 1000f
        val wordOffset = (progress * paragraphs[pIndex].length).toInt()
        
        currentParagraphIndex = pIndex
        resumeWordOffset = wordOffset
        
        // Find a word boundary for the highlight
        val text = paragraphs[pIndex]
        if (text.isNotEmpty()) {
            val start = wordOffset.coerceIn(0, text.length - 1)
            var end = start
            while (end < text.length && !text[end].isWhitespace()) {
                end++
            }
            currentWordRange = start until end
        } else {
            currentWordRange = null
        }
        
        if (_playWhenReady) {
            resumeInternal()
        }
    }

    fun requestAudioFocus(): Int {
        val result = audioFocusManager.requestAudioFocus()
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
        }
        return result
    }

    private fun abandonAudioFocusInternal() {
        audioFocusManager.abandonAudioFocus()
    }

    private fun pauseInternal() {
        ttsEngine.stop()
        isEngineSpeaking = false
        noisyAudioReceiver.unregister()
        powerLockManager.releaseLocks()
        lastEnqueuedUtteranceId = null
    }

    private fun resumeInternal() {
        noisyAudioReceiver.register()
        powerLockManager.acquireLocks()
        speakCurrentFrom(currentParagraphIndex, resumeWordOffset)
    }

    private fun registerNoisyReceiver() {
        noisyAudioReceiver.register()
    }

    private fun unregisterNoisyReceiver() {
        noisyAudioReceiver.unregister()
    }

    private fun acquireLocks() {
        powerLockManager.acquireLocks()
    }

    private fun releaseLocks() {
        powerLockManager.releaseLocks()
    }

    // TtsPlayer specific
    fun speak(article: Article, parsedParagraphs: List<String>, playWhenReady: Boolean = false) {
        android.util.Log.d("TtsPlayer", "speak() called: title=${article.title}, paragraphs=${parsedParagraphs.size}, playWhenReady=$playWhenReady")
        isPreparing = true
        invalidateState()
        
        ttsEngine.stop()
        isEngineSpeaking = false
        lastEnqueuedUtteranceId = null

        paragraphs = parsedParagraphs
        if (paragraphs.isEmpty()) {
            _playbackState = STATE_ENDED
            invalidateState()
            return
        }
        
        val artworkUrl = article.imageUrl ?: article.feedImageUrl
        val artworkUri = artworkUrl?.takeIf { it.startsWith("http") }?.toUri()
        val mediaItem = MediaItem.Builder()
            .setMediaId(article.id)
            .setUri("tts://${article.id}")
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(article.title)
                .setSubtitle(article.source)
                .setArtist(article.source)
                .setAlbumTitle(article.source)
                .setAlbumArtist(article.source)
                .setDisplayTitle(article.title) // Essential for Samsung "Now Bar"
                .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                .setArtworkUri(artworkUri)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build())
            .build()
        
        _currentMediaItem = mediaItem
        
        currentItemIndex = if (!mediaItems.any { it.mediaId == article.id }) {
            mediaItems.clear()
            mediaItems.add(mediaItem)
            0
        } else {
            mediaItems.indexOfFirst { it.mediaId == article.id }
        }

        _playbackState = STATE_READY
        
        currentParagraphIndex = article.currentParagraphIndex.coerceIn(0, paragraphs.size - 1).takeIf { paragraphs.isNotEmpty() } ?: 0
        resumeWordOffset = article.currentWordOffset.coerceAtLeast(0)
        
        // Initialize highlight from saved offset
        if (currentParagraphIndex in paragraphs.indices) {
            val text = paragraphs[currentParagraphIndex]
            val start = resumeWordOffset.coerceIn(0, text.length.let { if (it > 0) it - 1 else 0 })
            if (text.isNotEmpty()) {
                var end = start
                while (end < text.length && !text[end].isWhitespace()) {
                    end++
                }
                currentWordRange = start until end
            } else {
                currentWordRange = null
            }
        } else {
            currentWordRange = null
        }
        
        _playWhenReady = playWhenReady
        isPreparing = false

        if (artworkUrl != null) {
            loadArtwork(artworkUrl, article.id)
        }

        if (playWhenReady) {
            requestAudioFocus()
            resumeInternal()
        }
        
        invalidateState()
    }

    private fun loadArtwork(url: String, mediaId: String) {
        scope.launch {
            try {
                val imageLoader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(800) // Optimal for notification background
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    val bytes = stream.toByteArray()

                    val currentItem = _currentMediaItem
                    if (currentItem != null && currentItem.mediaId == mediaId) {
                        _currentMediaItem = currentItem.buildUpon()
                            .setMediaMetadata(currentItem.mediaMetadata.buildUpon()
                                .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build())
                            .build()

                        // Also update the item in the playlist if present
                        val index = mediaItems.indexOfFirst { it.mediaId == mediaId }
                        if (index != -1) {
                            mediaItems[index] = _currentMediaItem!!
                        }

                        invalidateState()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TtsPlayer", "Error loading artwork", e)
            }
        }
    }

    fun speakAnnouncement(text: String) {
        powerLockManager.acquireLocks()
        lastEnqueuedUtteranceId = null
        if (audioFocusManager.requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            ttsEngine.speak(text, "announcement")
        }
    }

    fun acquireManualWakeLock() {
        powerLockManager.acquireManualWakeLock()
    }

    fun releaseManualWakeLock() {
        powerLockManager.releaseManualWakeLock()
    }

    /**
     * Releases audio focus, locks, and noisy receiver without clearing
     * playback state. Used when an article finishes and autoplay is off.
     */
    fun releasePlaybackResources() {
        audioFocusManager.abandonAudioFocus()
        noisyAudioReceiver.unregister()
        powerLockManager.releaseLocks()
        lastEnqueuedUtteranceId = null
    }

    private fun speakCurrentFrom(startIndex: Int, wordOffset: Int = 0) {
        baseWordOffset = wordOffset
        currentParagraphIndex = startIndex
        for (i in startIndex until paragraphs.size) {
            val text = if (i == startIndex && wordOffset > 0 && wordOffset < paragraphs[i].length) {
                paragraphs[i].substring(wordOffset)
            } else {
                paragraphs[i]
            }
            if (i == startIndex) {
                ttsEngine.speak(text, i.toString())
            } else {
                ttsEngine.enqueue(text, i.toString())
            }
        }
        lastEnqueuedUtteranceId = if (paragraphs.isNotEmpty()) (paragraphs.size - 1).toString() else null
    }

    fun seekToWord(paragraphIndex: Int, wordRange: IntRange, playWhenReady: Boolean = true) {
        if (paragraphIndex in paragraphs.indices) {
            lastEnqueuedUtteranceId = null
            ttsEngine.stop()
            currentParagraphIndex = paragraphIndex
            currentWordRange = wordRange
            resumeWordOffset = wordRange.first
            _playWhenReady = playWhenReady
            if (playWhenReady) {
                requestAudioFocus()
                resumeInternal()
            }
            invalidateState()
        }
    }

    fun getParagraphIndex(): Int = currentParagraphIndex
    fun getWordRange(): IntRange? = currentWordRange
    
    fun triggerStateInvalidation() {
        invalidateState()
    }
    
    // Stub implementations for required methods
    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> = Futures.immediateVoidFuture()
    
    @Deprecated("Deprecated in Player", ReplaceWith("handleSetVolume(volume)"))
    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        ttsEngine.setVolume(volume)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetDeviceVolume(volume: Int, flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): ListenableFuture<*> {
        _audioAttributes = audioAttributes
        ttsEngine.setAudioAttributes(audioAttributes.usage, audioAttributes.contentType)
        if (handleAudioFocus && _playWhenReady) {
            requestAudioFocus()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
