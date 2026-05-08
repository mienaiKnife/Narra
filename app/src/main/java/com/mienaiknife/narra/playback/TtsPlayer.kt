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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

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

    val engineState = ttsEngine.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var _playWhenReady = false
    private var _playbackState = STATE_IDLE
    private var _currentMediaItem: MediaItem? = null
    private var _playbackParameters = PlaybackParameters.DEFAULT
    private var _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
    private var _playerError: PlaybackException? = null

    private var paragraphs: List<String> = emptyList()
    private val _currentParagraphIndexFlow = MutableStateFlow(-1)
    val currentParagraphIndexFlow: StateFlow<Int> = _currentParagraphIndexFlow.asStateFlow()
    private var currentParagraphIndex: Int
        get() = _currentParagraphIndexFlow.value
        set(value) { _currentParagraphIndexFlow.value = value }

    private val _currentWordRangeFlow = MutableStateFlow<IntRange?>(null)
    val currentWordRangeFlow: StateFlow<IntRange?> = _currentWordRangeFlow.asStateFlow()
    private var currentWordRange: IntRange?
        get() = _currentWordRangeFlow.value
        set(value) { _currentWordRangeFlow.value = value }

    private var resumeWordOffset = 0
    private var isEngineSpeaking = false

    private var _seekForwardIncrement = 30000L
    private var _seekBackIncrement = 10000L

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                _playWhenReady = false
                pauseInternal()
                invalidateState()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                pauseInternal()
                invalidateState()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // For TTS/Speech, it's often better to pause than duck (talk over)
                // but we follow the request if possible. 
                // Here we'll treat it as transient loss to be safe for speech.
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                pauseInternal()
                invalidateState()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                if (_playWhenReady) {
                    resumeInternal()
                }
                invalidateState()
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                setPlayWhenReady(false)
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    init {
        settingsManager.fastForwardSkipTime.onEach { time ->
            _seekForwardIncrement = time.removeSuffix("s").toLongOrNull()?.times(1000) ?: 30000L
            invalidateState()
        }.launchIn(scope)

        settingsManager.rewindSkipTime.onEach { time ->
            _seekBackIncrement = time.removeSuffix("s").toLongOrNull()?.times(1000) ?: 10000L
            invalidateState()
        }.launchIn(scope)

        ttsEngine.state.onEach { state ->
            when (state) {
                is TtsState.Ready -> {
                    val wasSpeaking = isEngineSpeaking
                    isEngineSpeaking = false
                    _playbackState = STATE_READY
                    
                    if (_playWhenReady && (currentParagraphIndex >= 0) && (currentParagraphIndex < paragraphs.size)) {
                        if (currentParagraphIndex == paragraphs.size - 1 && wasSpeaking) {
                            _playWhenReady = false
                            _playbackState = STATE_ENDED
                        } else if (!wasSpeaking) {
                            resumeInternal()
                        }
                    }
                    
                    // Maintain WakeLock if we are still ready to play but not currently speaking
                    if (_playWhenReady || _playbackState == STATE_BUFFERING) {
                        acquireLocks()
                    } else {
                        releaseLocks()
                    }
                    
                    invalidateState()
                }
                is TtsState.Speaking -> {
                    isEngineSpeaking = true
                    _playbackState = STATE_READY
                    val index = state.utteranceId.toIntOrNull()
                    if (index != null) {
                        if (index != currentParagraphIndex) {
                            currentParagraphIndex = index
                            currentWordRange = null
                            resumeWordOffset = 0
                        }

                        if (state.start < state.end) {
                            currentWordRange = (state.start + resumeWordOffset) until (state.end + resumeWordOffset)
                        }
                    }
                    
                    // Ensure WakeLock is held while speaking
                    acquireLocks()
                    
                    invalidateState()
                }
                is TtsState.Initializing -> {
                    _playbackState = STATE_BUFFERING
                    
                    // Hold WakeLock while initializing/buffering
                    acquireLocks()
                    
                    invalidateState()
                }
                is TtsState.Error -> {
                    android.util.Log.e("TtsPlayer", "Engine error: ${state.message}")
                    _playerError = PlaybackException(state.message, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
                    _playbackState = STATE_IDLE
                    _playWhenReady = false
                    
                    releaseLocks()
                    invalidateState()
                }
                is TtsState.Idle -> {
                    if (paragraphs.isEmpty() && currentParagraphIndex == -1) {
                        _playbackState = STATE_IDLE
                        releaseLocks()
                        invalidateState()
                    }
                }
            }
        }.launchIn(scope)
    }

    override fun getState(): State {
        val playlist = if (_currentMediaItem != null || mediaItems.size > 0) {
            (0 until mediaItems.size).map { i ->
                val item = mediaItems[i]
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
        } else {
            emptyList()
        }

        val currentPositionMs = if (currentParagraphIndex >= 0 && paragraphs.isNotEmpty()) {
            val progress = currentWordRange?.let { it.last.toFloat() / paragraphs[currentParagraphIndex].length.toFloat() }
                ?: (resumeWordOffset.toFloat() / paragraphs[currentParagraphIndex].length.toFloat()).coerceIn(0f, 1f)
            (currentParagraphIndex * 1000L + (progress * 1000L).toLong())
        } else 0L

        val currentPlaybackState = if (playlist.isEmpty() && _playbackState != STATE_ENDED) {
            STATE_IDLE
        } else {
            _playbackState
        }

        android.util.Log.d("TtsPlayer", "getState(): state=$_playbackState -> $currentPlaybackState, playWhenReady=$_playWhenReady, hasPlaylist=${playlist.isNotEmpty()}, isSpeaking=$isEngineSpeaking")

        return State.Builder()
            .setAvailableCommands(Player.Commands.Builder()
                .addAll(
                    COMMAND_PLAY_PAUSE,
                    COMMAND_PREPARE,
                    COMMAND_STOP,
                    COMMAND_SET_SPEED_AND_PITCH,
                    COMMAND_GET_CURRENT_MEDIA_ITEM,
                    COMMAND_GET_METADATA,
                    COMMAND_GET_TIMELINE,
                    COMMAND_GET_AUDIO_ATTRIBUTES,
                    COMMAND_GET_TRACKS,
                    COMMAND_GET_DEVICE_VOLUME,
                    COMMAND_GET_VOLUME,
                    COMMAND_SEEK_TO_NEXT,
                    COMMAND_SEEK_TO_PREVIOUS,
                    COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    COMMAND_SEEK_FORWARD,
                    COMMAND_SEEK_BACK,
                    COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    COMMAND_SET_MEDIA_ITEM,
                    COMMAND_CHANGE_MEDIA_ITEMS
                ).build())
            .setPlayWhenReady(_playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(currentPlaybackState)
            .setPlaybackParameters(_playbackParameters)
            .setPlaybackSuppressionReason(_playbackSuppressionReason)
            .setPlayerError(_playerError)
            .setPlaylist(playlist)
            .setPlaylistMetadata(_currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY)
            .setCurrentMediaItemIndex(currentItemIndex)
            .setContentPositionMs(currentPositionMs)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        val playWhenReadyChanged = _playWhenReady != playWhenReady
        if (playWhenReadyChanged || (playWhenReady && _playbackSuppressionReason != PLAYBACK_SUPPRESSION_REASON_NONE)) {
            _playWhenReady = playWhenReady
            if (playWhenReady) {
                val focusResult = requestAudioFocus()
                if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                    resumeInternal()
                } else {
                    _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                    pauseInternal()
                }
            } else {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                pauseInternal()
            }
            invalidateState()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        _playbackState = if (_currentMediaItem != null) {
            if (paragraphs.isNotEmpty()) {
                STATE_READY
            } else {
                STATE_BUFFERING
            }
        } else {
            STATE_IDLE
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        _playWhenReady = false
        ttsEngine.stop()
        paragraphs = emptyList()
        currentParagraphIndex = -1
        currentWordRange = null
        resumeWordOffset = 0
        _playbackState = STATE_IDLE
        abandonAudioFocusInternal()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        _playbackParameters = playbackParameters
        ttsEngine.setPlaybackSpeed(playbackParameters.speed)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    private val mediaItems = mutableListOf<MediaItem>()
    private var currentItemIndex = 0

    override fun handleSetMediaItems(items: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        mediaItems.clear()
        mediaItems.addAll(items)
        currentItemIndex = if (startIndex != C.INDEX_UNSET) startIndex.coerceIn(items.indices) else 0
        
        if (mediaItems.isNotEmpty()) {
            _currentMediaItem = mediaItems[currentItemIndex]
        } else {
            _currentMediaItem = null
        }
        
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        if (mediaItemIndex != currentItemIndex && mediaItemIndex in mediaItems.indices) {
            currentItemIndex = mediaItemIndex
            _currentMediaItem = mediaItems[currentItemIndex]
            // When switching items, we'd normally need to reload content from repository
            // but PlaybackManager will handle the sync
            invalidateState()
            return Futures.immediateVoidFuture()
        }
        when (seekCommand) {
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                onSkipNext?.invoke()
            }
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                onSkipPrevious?.invoke()
            }
            COMMAND_SEEK_FORWARD -> {
                seekToPosition(currentPosition + _seekForwardIncrement)
            }
            COMMAND_SEEK_BACK -> {
                seekToPosition((currentPosition - _seekBackIncrement).coerceAtLeast(0))
            }
            else -> {
                seekToPosition(positionMs)
            }
        }
        return Futures.immediateVoidFuture()
    }

    private fun seekToPosition(positionMs: Long) {
        val index = (positionMs / 1000).toInt()
        val remainder = positionMs % 1000
        if (index in paragraphs.indices) {
            val text = paragraphs[index]
            val charOffset = if (text.isNotEmpty()) {
                (remainder.toFloat() / 1000f * text.length).toInt().coerceIn(0, text.length)
            } else 0
            
            val word = Regex("\\w+").find(text, charOffset)?.range 
                ?: Regex("\\w+").find(text)?.range
                ?: (charOffset until charOffset)
                
            seekToWord(index, word, _playWhenReady)
        }
    }

    internal fun requestAudioFocus(): Int {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                val attr = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attr)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                registerNoisyReceiver()
                acquireLocks()
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            }
            else -> {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            }
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
        unregisterNoisyReceiver()
        releaseLocks()
    }

    private fun pauseInternal() {
        ttsEngine.stop()
        isEngineSpeaking = false
        // Save current word offset for resume
        currentWordRange?.let { resumeWordOffset = it.first }
    }

    private fun resumeInternal() {
        if (currentParagraphIndex in paragraphs.indices) {
            speakCurrentFrom(currentParagraphIndex, resumeWordOffset)
        }
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
        if (!_playWhenReady && !isEngineSpeaking && _playbackState != STATE_BUFFERING) return

        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Narra:PlaybackWakeLock").apply {
                setReferenceCounted(false)
            }
        }
        // Acquire for a long duration if needed, but it's better to manage it explicitly
        if (!wakeLock!!.isHeld) {
            wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max, will be released manually
        }

        if (wifiLock == null) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(lockMode, "Narra:PlaybackWifiLock").apply {
                setReferenceCounted(false)
            }
        }
        if (!wifiLock!!.isHeld) {
            wifiLock?.acquire()
        }
    }

    private fun releaseLocks() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
    }

    // TtsPlayer specific
    fun speak(article: com.mienaiknife.narra.data.models.Article, parsedParagraphs: List<String>, playWhenReady: Boolean = false) {
        android.util.Log.d("TtsPlayer", "speak() called: title=${article.title}, paragraphs=${parsedParagraphs.size}, playWhenReady=$playWhenReady")
        
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
                .setArtist(article.source)
                .setArtworkUri(artworkUri)
                .setIsPlayable(true)
                .build())
            .build()
        
        _currentMediaItem = mediaItem
        
        // Ensure this item is in the mediaItems list if not already
        if (!mediaItems.any { it.mediaId == article.id }) {
            mediaItems.clear()
            mediaItems.add(mediaItem)
            currentItemIndex = 0
        } else {
            currentItemIndex = mediaItems.indexOfFirst { it.mediaId == article.id }
        }

        _playbackState = STATE_READY
        
        currentParagraphIndex = article.currentParagraphIndex.coerceIn(0, paragraphs.size - 1).takeIf { paragraphs.isNotEmpty() } ?: 0
        resumeWordOffset = article.currentWordOffset.coerceAtLeast(0)
        
        _playWhenReady = playWhenReady
        if (playWhenReady) {
            requestAudioFocus()
            resumeInternal()
        }
        
        invalidateState()
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
}
