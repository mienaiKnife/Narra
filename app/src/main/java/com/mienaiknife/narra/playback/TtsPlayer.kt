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
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.BasePlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A custom Media3 Player implementation that wraps a TtsEngine.
 */
@UnstableApi
@Singleton
class TtsPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ttsEngine: TtsEngine,
    settingsManager: PlaybackSettingsManager
) : BasePlayer() {

    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null

    val engineState: kotlinx.coroutines.flow.StateFlow<TtsState> = ttsEngine.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = mutableListOf<Player.Listener>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var _playWhenReady = false
    private var _playbackState = STATE_IDLE
    private var _currentMediaItem: MediaItem? = null
    private var _playbackParameters = PlaybackParameters.DEFAULT
    private var _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
    private var _playerError: PlaybackException? = null
    private var _handleAudioFocus = true

    private var paragraphs: List<String> = emptyList()
    private var currentParagraphIndex = -1
    private var currentWordRange: IntRange? = null
    private var resumeWordOffset = 0
    private var isEngineSpeaking = false

    private var _seekForwardIncrement = 30000L
    private var _seekBackIncrement = 10000L

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_playbackSuppressionReason != PLAYBACK_SUPPRESSION_REASON_NONE) {
                    _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                    if (_playWhenReady) {
                        resumeInternal()
                    }
                    notifySuppressionChanged()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                setPlayWhenReady(false)
                abandonAudioFocusInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_playWhenReady && _playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE) {
                    _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                    pauseInternal()
                    notifySuppressionChanged()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // For speech, we usually want to pause instead of ducking, 
                // but if we were to duck, we'd lower volume here.
                // For Narra, we treat ducking as a transient loss.
                if (_playWhenReady && _playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE) {
                    _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                    pauseInternal()
                    notifySuppressionChanged()
                }
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    /**
     * Requests audio focus. Returns the result code from AudioManager.
     */
    fun requestAudioFocus(): Int {
        if (!_handleAudioFocus) return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        
        registerNoisyReceiver()
        acquireLocks()

        val usage = when (_audioAttributes.usage) {
            androidx.media3.common.C.USAGE_MEDIA -> android.media.AudioAttributes.USAGE_MEDIA
            androidx.media3.common.C.USAGE_ASSISTANCE_ACCESSIBILITY -> android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
            else -> android.media.AudioAttributes.USAGE_MEDIA
        }
        val contentType = when (_audioAttributes.contentType) {
            androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH -> android.media.AudioAttributes.CONTENT_TYPE_SPEECH
            androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC -> android.media.AudioAttributes.CONTENT_TYPE_MUSIC
            else -> android.media.AudioAttributes.CONTENT_TYPE_SPEECH
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = android.media.AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
            
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocusInternal() {
        unregisterNoisyReceiver()
        releaseLocks()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun notifySuppressionChanged() {
        listeners.forEach {
            it.onPlaybackSuppressionReasonChanged(_playbackSuppressionReason)
            it.onIsPlayingChanged(isPlaying)
        }
    }

    private fun pauseInternal() {
        ttsEngine.stop()
        isEngineSpeaking = false
    }

    private fun resumeInternal() {
        if (currentParagraphIndex >= 0 && paragraphs.isNotEmpty()) {
            val index = currentParagraphIndex.coerceAtLeast(0)
            resumeWordOffset = currentWordRange?.first ?: 0
            speakCurrentFrom(index, resumeWordOffset)
        }
    }

    init {
        settingsManager.fastForwardSkipTime.onEach { time ->
            _seekForwardIncrement = time.removeSuffix("s").toLongOrNull()?.times(1000) ?: 30000L
        }.launchIn(scope)

        settingsManager.rewindSkipTime.onEach { time ->
            _seekBackIncrement = time.removeSuffix("s").toLongOrNull()?.times(1000) ?: 10000L
        }.launchIn(scope)

        settingsManager.ttsSpeakerId.onEach {
            if (isEngineSpeaking && _playWhenReady) {
                resumeInternal()
            }
        }.launchIn(scope)

        ttsEngine.state.onEach { state ->
            when (state) {
                is TtsState.Ready -> {
                    val wasSpeaking = isEngineSpeaking
                    isEngineSpeaking = false
                    updatePlaybackState(STATE_READY)
                    
                    // If we are supposed to be playing but the engine stopped (e.g. finished an article or initialization)
                    if (_playWhenReady && currentParagraphIndex >= 0 && currentParagraphIndex < paragraphs.size) {
                        // If we were speaking and reached the end of the last paragraph, it's a natural end.
                        // But if we weren't speaking (e.g. engine just initialized), we should start/resume.
                        if (currentParagraphIndex == paragraphs.size - 1 && wasSpeaking) {
                            // Finished article
                            _playWhenReady = false
                            abandonAudioFocusInternal()
                            updatePlaybackState(STATE_ENDED)
                        } else if (!wasSpeaking) {
                            // Engine just became ready (e.g. after model switch or init), so resume synthesis
                            resumeInternal()
                        }
                    }
                }
                is TtsState.Speaking -> {
                    isEngineSpeaking = true
                    updatePlaybackState(STATE_READY)
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
                        
                        listeners.forEach { 
                            // Using position discontinuity to signal UI that paragraph/word changed
                            it.onPositionDiscontinuity(
                                Player.PositionInfo(
                                    null, 0, null, 0, 0, 0, 0, 0, 0
                                ),
                                Player.PositionInfo(
                                    null, 0, null, 0, 0, 0, 0, 0, 0
                                ),
                                DISCONTINUITY_REASON_AUTO_TRANSITION
                            ) 
                        }
                    }
                }
                is TtsState.Initializing -> {
                    updatePlaybackState(STATE_BUFFERING)
                }
                is TtsState.Error -> {
                    _playerError = PlaybackException(
                        state.message,
                        null,
                        PlaybackException.ERROR_CODE_UNSPECIFIED
                    )
                    updatePlaybackState(STATE_IDLE)
                }
                is TtsState.Idle -> {
                    updatePlaybackState(STATE_IDLE)
                }
            }
        }.launchIn(scope)
    }

    private fun updatePlaybackState(state: Int) {
        if (_playbackState != state) {
            android.util.Log.d("TtsPlayer", "Playback state changed: ${getStateString(_playbackState)} -> ${getStateString(state)}")
            _playbackState = state
            listeners.forEach { 
                it.onPlaybackStateChanged(state)
                it.onIsPlayingChanged(isPlaying)
            }
        }
    }

    private fun getStateString(state: Int): String = when(state) {
        STATE_IDLE -> "IDLE"
        STATE_BUFFERING -> "BUFFERING"
        STATE_READY -> "READY"
        STATE_ENDED -> "ENDED"
        else -> "UNKNOWN"
    }

    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
    }

    override fun getApplicationLooper(): Looper = Looper.getMainLooper()

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        val playWhenReadyChanged = _playWhenReady != playWhenReady
        if (playWhenReadyChanged || (playWhenReady && _playbackSuppressionReason != PLAYBACK_SUPPRESSION_REASON_NONE)) {
            android.util.Log.d("TtsPlayer", "setPlayWhenReady: $playWhenReady (changed: $playWhenReadyChanged, suppression: $_playbackSuppressionReason)")
            _playWhenReady = playWhenReady
            
            if (playWhenReady) {
                val focusResult = requestAudioFocus()
                when (focusResult) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                        resumeInternal()
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                        pauseInternal()
                    }
                    else -> {
                        _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                        pauseInternal()
                    }
                }
            } else {
                _playbackSuppressionReason = PLAYBACK_SUPPRESSION_REASON_NONE
                // We don't abandon focus here immediately to ensure we remain the 
                // preferred media app for headset buttons when paused.
                // abandonAudioFocusInternal() 
                pauseInternal()
            }
            
            listeners.forEach { 
                if (playWhenReadyChanged) {
                    it.onPlayWhenReadyChanged(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                }
                it.onPlaybackSuppressionReasonChanged(_playbackSuppressionReason)
                it.onIsPlayingChanged(isPlaying)
            }
        }
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        if (mediaItems.isNotEmpty()) {
            _currentMediaItem = mediaItems[0]
            // In a real implementation we'd handle the full list
        }
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        if (startIndex in mediaItems.indices) {
            _currentMediaItem = mediaItems[startIndex]
        }
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {}

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) {}

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}

    override fun prepare() {
        if (ttsEngine.state.value is TtsState.Initializing) {
            updatePlaybackState(STATE_BUFFERING)
        } else {
            updatePlaybackState(STATE_READY)
        }
    }

    override fun getPlaybackSuppressionReason(): Int = _playbackSuppressionReason

    override fun getPlayerError(): PlaybackException? = _playerError

    override fun setRepeatMode(repeatMode: Int) {}

    override fun getRepeatMode(): Int = REPEAT_MODE_OFF

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}

    override fun getShuffleModeEnabled(): Boolean = false

    override fun isLoading(): Boolean = false

    override fun getSeekBackIncrement(): Long = _seekBackIncrement

    override fun getSeekForwardIncrement(): Long = _seekForwardIncrement

    override fun getMaxSeekToPreviousPosition(): Long = 0

    override fun clearVideoSurface() {}

    override fun clearVideoSurface(surface: Surface?) {}

    override fun setVideoSurface(surface: Surface?) {}

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}

    override fun setVideoTextureView(textureView: TextureView?) {}

    override fun clearVideoTextureView(textureView: TextureView?) {}

    override fun getSurfaceSize(): Size = Size.UNKNOWN

    private var _audioAttributes: AudioAttributes = AudioAttributes.DEFAULT

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        _audioAttributes = audioAttributes
        _handleAudioFocus = handleAudioFocus
        // Map Media3 AudioAttributes to Android AudioAttributes for TTS
        val usage = when (audioAttributes.usage) {
            androidx.media3.common.C.USAGE_MEDIA -> android.media.AudioAttributes.USAGE_MEDIA
            androidx.media3.common.C.USAGE_ASSISTANCE_ACCESSIBILITY -> android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
            else -> android.media.AudioAttributes.USAGE_MEDIA
        }
        val contentType = when (audioAttributes.contentType) {
            androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH -> android.media.AudioAttributes.CONTENT_TYPE_SPEECH
            androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC -> android.media.AudioAttributes.CONTENT_TYPE_MUSIC
            else -> android.media.AudioAttributes.CONTENT_TYPE_SPEECH
        }
        ttsEngine.setAudioAttributes(usage, contentType)
    }

    override fun getAudioAttributes(): AudioAttributes = _audioAttributes

    override fun getPlayWhenReady(): Boolean = _playWhenReady

    override fun getPlaybackState(): Int = _playbackState

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        _playbackParameters = playbackParameters
        ttsEngine.setPlaybackSpeed(playbackParameters.speed)
        listeners.forEach { 
            it.onPlaybackParametersChanged(playbackParameters)
        }
    }

    override fun getPlaybackParameters(): PlaybackParameters = _playbackParameters

    override fun stop() {
        val previousPlayWhenReady = _playWhenReady
        _playWhenReady = false
        ttsEngine.stop()
        abandonAudioFocusInternal()
        updatePlaybackState(STATE_IDLE)
        if (previousPlayWhenReady) {
            listeners.forEach {
                it.onPlayWhenReadyChanged(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                it.onIsPlayingChanged(isPlaying)
            }
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
        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Narra:PlaybackWakeLock").apply {
                acquire(10 * 60 * 1000L /* 10 minutes */)
            }
        }
        if (wifiLock == null) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(lockMode, "Narra:PlaybackWifiLock").apply {
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        wifiLock?.let {
            if (it.isHeld) it.release()
        }
        wifiLock = null
    }

    override fun release() {
        stop()
        unregisterNoisyReceiver()
        releaseLocks()
        ttsEngine.release()
        scope.cancel()
    }

    override fun getDuration(): Long = paragraphs.size.toLong() * 1000L

    override fun getCurrentPosition(): Long {
        if (paragraphs.isEmpty() || currentParagraphIndex < 0) return 0L
        val basePosition = currentParagraphIndex.toLong() * 1000L
        val paragraphText = paragraphs.getOrNull(currentParagraphIndex) ?: return basePosition
        if (paragraphText.isEmpty()) return basePosition

        val wordOffset = currentWordRange?.first ?: 0
        val intraParagraphProgress = wordOffset.toFloat() / paragraphText.length.toFloat()
        return basePosition + (intraParagraphProgress * 1000L).toLong()
    }

    override fun getBufferedPosition(): Long = 0

    override fun getTotalBufferedDuration(): Long = 0

    override fun isPlayingAd(): Boolean = false

    override fun getCurrentAdGroupIndex(): Int = -1

    override fun getCurrentAdIndexInAdGroup(): Int = -1

    override fun getContentPosition(): Long = 0

    override fun getContentBufferedPosition(): Long = 0

    // Timeline and Tracks
    override fun getCurrentTracks(): Tracks = Tracks.EMPTY
    @Suppress("DEPRECATION")
    override fun getTrackSelectionParameters(): TrackSelectionParameters = TrackSelectionParameters.DEFAULT
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}
    override fun getMediaMetadata(): MediaMetadata = _currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
    override fun getPlaylistMetadata(): MediaMetadata = getMediaMetadata()
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {}
    override fun getCurrentTimeline(): Timeline {
        val item = _currentMediaItem ?: return Timeline.EMPTY
        return object : Timeline() {
            override fun getWindowCount(): Int = 1
            override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
                window.set(
                    "window",
                    item,
                    null,
                    0,
                    0,
                    0,
                    true,
                    false,
                    null,
                    0,
                    getDuration() * 1000L,
                    0,
                    0,
                    0
                )
                return window
            }
            override fun getPeriodCount(): Int = 1
            override fun getPeriod(periodIndex: Int, period: Period, setIdentifiers: Boolean): Period {
                period.set("period", "period", 0, getDuration() * 1000L, 0)
                return period
            }
            override fun getIndexOfPeriod(uid: Any): Int = if (uid == "period") 0 else -1
            override fun getUidOfPeriod(periodIndex: Int): Any = "period"
        }
    }
    override fun getCurrentPeriodIndex(): Int = 0
    override fun getCurrentMediaItemIndex(): Int = 0

    // Commands
    override fun getAvailableCommands(): Player.Commands = Player.Commands.Builder()
        .addAll(
            COMMAND_PLAY_PAUSE,
            COMMAND_PREPARE,
            COMMAND_STOP,
            COMMAND_SET_SPEED_AND_PITCH,
            COMMAND_GET_CURRENT_MEDIA_ITEM,
            COMMAND_GET_METADATA,
            COMMAND_GET_TIMELINE,
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            COMMAND_SEEK_FORWARD,
            COMMAND_SEEK_BACK,
            COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
        ).build()

    // Required by BasePlayer
    override fun getVolume(): Float = 1f
    override fun setVolume(volume: Float) {}
    override fun getDeviceInfo(): DeviceInfo = DeviceInfo.UNKNOWN
    override fun getDeviceVolume(): Int = 0
    override fun isDeviceMuted(): Boolean = false
    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {}
    override fun setDeviceVolume(volume: Int, flags: Int) {}
    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() {}
    override fun increaseDeviceVolume(flags: Int) {}
    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() {}
    override fun decreaseDeviceVolume(flags: Int) {}
    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(muted: Boolean) {}
    override fun setDeviceMuted(muted: Boolean, flags: Int) {}
    override fun mute() {}
    override fun unmute() {}
    override fun getVideoSize(): VideoSize = VideoSize.UNKNOWN
    override fun getCurrentCues(): CueGroup = CueGroup.EMPTY_TIME_ZERO

    // Navigation
    override fun seekTo(mediaItemIndex: Int, positionMs: Long, seekCommand: Int, isAutoSeek: Boolean) {
        val index = (positionMs / 1000).toInt()
        val remainder = positionMs % 1000
        if (index in paragraphs.indices) {
            val text = paragraphs[index]
            val charOffset = if (text.isNotEmpty()) {
                (remainder.toFloat() / 1000f * text.length).toInt().coerceIn(0, text.length)
            } else 0
            
            val word = Regex("\\w+").find(text, charOffset)?.range 
                ?: Regex("\\w+").find(text)?.range // fallback to first word if none found after offset
                ?: (charOffset until charOffset)
                
            seekToWord(index, word, _playWhenReady)
        }
    }
    
    // Internal handlers for navigation commands
    fun seekToNextInternal() {
        onSkipNext?.invoke()
    }

    fun seekToPreviousInternal() {
        onSkipPrevious?.invoke()
    }

    fun seekToParagraph(index: Int) {
        if (index in paragraphs.indices) {
            val text = paragraphs[index]
            val word = Regex("\\w+").find(text)?.range ?: (0..0)
            seekToWord(index, word, _playWhenReady)
        }
    }

    fun seekToWord(paragraphIndex: Int, wordRange: IntRange, startPlaying: Boolean = true) {
        if (paragraphIndex in paragraphs.indices) {
            currentParagraphIndex = paragraphIndex
            resumeWordOffset = wordRange.first
            currentWordRange = wordRange

            ttsEngine.stop()
            val previousPlayWhenReady = _playWhenReady
            if (startPlaying) {
                _playWhenReady = true
            }

            if (_playWhenReady) {
                speakCurrentFrom(paragraphIndex, wordRange.first)
            }

            listeners.forEach {
                if (previousPlayWhenReady != _playWhenReady) {
                    it.onPlayWhenReadyChanged(_playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    it.onIsPlayingChanged(isPlaying)
                }
                // Signal position change to update UI highlighting
                it.onPositionDiscontinuity(
                    Player.PositionInfo(null, 0, null, 0, 0, 0, 0, 0, 0),
                    Player.PositionInfo(null, 0, null, 0, 0, paragraphIndex.toLong(), 0, 0, 0),
                    DISCONTINUITY_REASON_SEEK
                )
            }
        }
    }
    
    // TtsPlayer specific
    fun speak(article: com.mienaiknife.narra.data.models.Article, parsedParagraphs: List<String>, playWhenReady: Boolean = false) {
        // Stop any current playback
        ttsEngine.stop()
        isEngineSpeaking = false

        _currentMediaItem = MediaItem.Builder()
            .setMediaId(article.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(article.title)
                    .setArtist(article.source)
                    .setArtworkUri(article.imageUrl?.toUri())
                    .build()
            )
            .build()
        
        paragraphs = parsedParagraphs
        if (paragraphs.isEmpty()) {
            updatePlaybackState(STATE_ENDED)
            return
        }
        currentParagraphIndex = article.currentParagraphIndex.coerceIn(0, paragraphs.size - 1).takeIf { paragraphs.isNotEmpty() } ?: 0
        
        // Initialize currentWordRange to the saved word offset or the first word of the resumed paragraph
        val currentParaText = paragraphs.getOrNull(currentParagraphIndex)
        currentWordRange = currentParaText?.let { text ->
            if (article.currentWordOffset > 0 && article.currentWordOffset < text.length) {
                Regex("\\w+").find(text, article.currentWordOffset)?.range
            } else {
                Regex("\\w+").find(text)?.range
            }
        }
        
        resumeWordOffset = article.currentWordOffset.coerceAtLeast(0)
        
        listeners.forEach { 
            it.onMediaItemTransition(_currentMediaItem, MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            it.onMediaMetadataChanged(mediaMetadata)
            it.onTimelineChanged(currentTimeline, TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }

        setPlayWhenReady(playWhenReady)

        listeners.forEach {
            // Signal position change to update UI highlighting and scroll to current position
            it.onPositionDiscontinuity(
                Player.PositionInfo(null, 0, null, 0, 0, 0, 0, 0, 0),
                Player.PositionInfo(null, 0, null, 0, 0, currentParagraphIndex.toLong(), 0, 0, 0),
                DISCONTINUITY_REASON_AUTO_TRANSITION
            )
        }

        if (ttsEngine.state.value is TtsState.Initializing) {
            updatePlaybackState(STATE_BUFFERING)
        } else {
            updatePlaybackState(STATE_READY)
        }
    }

    fun speakAnnouncement(text: String) {
        if (requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            ttsEngine.speak(text, "announcement")
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

    fun getCurrentParagraphIndex(): Int = currentParagraphIndex

    fun getCurrentWordRange(): IntRange? = currentWordRange
}
