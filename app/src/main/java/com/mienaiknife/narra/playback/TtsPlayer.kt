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
import androidx.media3.common.Player.COMMAND_CHANGE_MEDIA_ITEMS
import androidx.media3.common.Player.COMMAND_GET_AUDIO_ATTRIBUTES
import androidx.media3.common.Player.COMMAND_GET_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_GET_DEVICE_VOLUME
import androidx.media3.common.Player.COMMAND_GET_METADATA
import androidx.media3.common.Player.COMMAND_GET_TIMELINE
import androidx.media3.common.Player.COMMAND_GET_TRACKS
import androidx.media3.common.Player.COMMAND_GET_VOLUME
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_PREPARE
import androidx.media3.common.Player.COMMAND_RELEASE
import androidx.media3.common.Player.COMMAND_SEEK_BACK
import androidx.media3.common.Player.COMMAND_SEEK_FORWARD
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_DEFAULT_POSITION
import androidx.media3.common.Player.COMMAND_SEEK_TO_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SET_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SET_PLAYLIST_METADATA
import androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE
import androidx.media3.common.Player.COMMAND_SET_SHUFFLE_MODE
import androidx.media3.common.Player.COMMAND_SET_SPEED_AND_PITCH
import androidx.media3.common.Player.COMMAND_SET_VOLUME
import androidx.media3.common.Player.COMMAND_STOP
import androidx.media3.common.Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.core.net.toUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

    private var _seekForwardIncrement = 15000L
    private var _seekBackIncrement = 15000L

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_playWhenReady) {
                    resumeInternal()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                _playWhenReady = false
                pauseInternal()
                invalidateState()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Not implemented, just keep playing
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                _playWhenReady = false
                pauseInternal()
                invalidateState()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

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
                        // Finished an utterance, wait for next or signal completion
                        // If paragraphs ended, this will be handled by ttsEngine.enqueue chain
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

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Narra:PlaybackWakeLock")
        
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Narra:PlaybackWifiLock")
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

        val currentPlaybackState = if (playlist.isEmpty() && _playbackState != STATE_ENDED) {
            if (isPreparing || _playWhenReady) STATE_BUFFERING else STATE_IDLE
        } else if (isPreparing) {
            STATE_BUFFERING
        } else {
            _playbackState
        }

        android.util.Log.d("TtsPlayer", "getState(): state=$_playbackState -> $currentPlaybackState, playWhenReady=$_playWhenReady, hasPlaylist=${playlist.isNotEmpty()}, isSpeaking=$isEngineSpeaking")

        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        COMMAND_PLAY_PAUSE,
                        COMMAND_PREPARE,
                        COMMAND_STOP,
                        COMMAND_SEEK_TO_DEFAULT_POSITION,
                        COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        COMMAND_SEEK_TO_PREVIOUS,
                        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        COMMAND_SEEK_TO_NEXT,
                        COMMAND_SEEK_TO_MEDIA_ITEM,
                        COMMAND_SEEK_BACK,
                        COMMAND_SEEK_FORWARD,
                        COMMAND_SET_SPEED_AND_PITCH,
                        COMMAND_SET_SHUFFLE_MODE,
                        COMMAND_SET_REPEAT_MODE,
                        COMMAND_GET_CURRENT_MEDIA_ITEM,
                        COMMAND_GET_TIMELINE,
                        COMMAND_GET_METADATA,
                        COMMAND_SET_PLAYLIST_METADATA,
                        COMMAND_SET_MEDIA_ITEM,
                        COMMAND_CHANGE_MEDIA_ITEMS,
                        COMMAND_GET_AUDIO_ATTRIBUTES,
                        COMMAND_GET_VOLUME,
                        COMMAND_GET_DEVICE_VOLUME,
                        COMMAND_SET_VOLUME,
                        COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                        COMMAND_GET_TRACKS,
                        COMMAND_RELEASE,
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
        _playWhenReady = playWhenReady
        if (playWhenReady) {
            if (requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                resumeInternal()
            } else {
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
        currentWordRange = null
        
        if (_playWhenReady) {
            resumeInternal()
        }
    }

    fun requestAudioFocus(): Int {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AndroidAudioAttributes.Builder()
                .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                .setContentType(AndroidAudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
        }
        return result
    }

    private fun abandonAudioFocusInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun pauseInternal() {
        ttsEngine.stop()
        isEngineSpeaking = false
        unregisterNoisyReceiver()
        releaseLocks()
    }

    private fun resumeInternal() {
        registerNoisyReceiver()
        acquireLocks()
        speakCurrentFrom(currentParagraphIndex, resumeWordOffset)
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            context.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            isNoisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            context.unregisterReceiver(noisyReceiver)
            isNoisyReceiverRegistered = false
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            android.util.Log.e("TtsPlayer", "Error acquiring locks", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("TtsPlayer", "Error releasing locks", e)
        }
    }

    // TtsPlayer specific
    fun speak(article: Article, parsedParagraphs: List<String>, playWhenReady: Boolean = false) {
        android.util.Log.d("TtsPlayer", "speak() called: title=${article.title}, paragraphs=${parsedParagraphs.size}, playWhenReady=$playWhenReady")
        isPreparing = true
        invalidateState()
        
        ttsEngine.stop()
        isEngineSpeaking = false

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
                .setArtworkUri(artworkUri)
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
        acquireLocks()
        if (requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            ttsEngine.speak(text, "announcement")
        }
    }

    fun acquireManualWakeLock() {
        acquireLocks()
    }

    fun releaseManualWakeLock() {
        if (!_playWhenReady && !isEngineSpeaking) {
            releaseLocks()
        }
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
    }

    fun seekToWord(paragraphIndex: Int, wordRange: IntRange, playWhenReady: Boolean = true) {
        if (paragraphIndex in paragraphs.indices) {
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
    override fun handleSetVolume(volume: Float): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetDeviceVolume(volume: Int, flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): ListenableFuture<*> {
        _audioAttributes = audioAttributes
        ttsEngine.setAudioAttributes(audioAttributes.usage, audioAttributes.contentType)
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
