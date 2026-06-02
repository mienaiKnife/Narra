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

package com.mienaiknife.narra.tts.common

import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import com.mienaiknife.narra.tts.android.AndroidTtsEngine
import com.mienaiknife.narra.tts.ondevice.SherpaTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelegatingTtsEngine @Inject constructor(
    private val androidTtsEngine: AndroidTtsEngine,
    private val sherpaTtsEngine: SherpaTtsEngine,
    private val settingsManager: PlaybackSettingsManager,
) : TtsEngine {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private var currentEngine: TtsEngine = androidTtsEngine
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var engineStateJob: Job? = null

    private var currentSpeed = 1.0f
    private var currentVolume = 1.0f
    private var currentAudioUsage = android.media.AudioAttributes.USAGE_MEDIA
    private var currentAudioContentType = android.media.AudioAttributes.CONTENT_TYPE_SPEECH

    init {
        scope.launch {
            settingsManager.ttsEngine.collectLatest { engineType ->
                when (engineType) {
                    "android" -> switchEngine(androidTtsEngine)
                    "ondevice" -> switchEngine(sherpaTtsEngine)
                }
            }
        }
    }

    private fun switchEngine(newEngine: TtsEngine) {
        if ((currentEngine == newEngine) && (engineStateJob != null)) return
        
        currentEngine.stop()
        
        // Performance/Memory: If switching AWAY from SherpaTtsEngine, we might want to release its models.
        // However, we don't want to release if the user might switch back quickly.
        // For now, let's at least call release if it's the on-device engine and we're switching to android.
        if (currentEngine is SherpaTtsEngine && newEngine is AndroidTtsEngine) {
            currentEngine.release()
        }

        currentEngine = newEngine
        
        // Apply current settings to the new engine
        currentEngine.setPlaybackSpeed(currentSpeed)
        currentEngine.setVolume(currentVolume)
        currentEngine.setAudioAttributes(currentAudioUsage, currentAudioContentType)
        
        engineStateJob?.cancel()
        engineStateJob = scope.launch {
            currentEngine.state.collect {
                _state.value = it
            }
        }
    }

    override fun speak(text: String, utteranceId: String) {
        currentEngine.speak(text, utteranceId)
    }

    override fun enqueue(text: String, utteranceId: String) {
        currentEngine.enqueue(text, utteranceId)
    }

    override fun stop() {
        currentEngine.stop()
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        currentEngine.setPlaybackSpeed(speed)
    }

    override fun setAudioAttributes(usage: Int, contentType: Int) {
        currentAudioUsage = usage
        currentAudioContentType = contentType
        currentEngine.setAudioAttributes(usage, contentType)
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume
        currentEngine.setVolume(volume)
    }

    override fun release() {
        androidTtsEngine.release()
        sherpaTtsEngine.release()
    }
}
