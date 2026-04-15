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

package com.mienaiknife.narra.tts.ondevice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SherpaTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager
) : TtsEngine {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var playbackSpeed = 1.0f
    private var volume = 1.0f
    private var audioUsage = AudioAttributes.USAGE_MEDIA
    private var audioContentType = AudioAttributes.CONTENT_TYPE_SPEECH

    private val queue = LinkedBlockingQueue<UtteranceRequest>()
    private var synthesisJob: Job? = null
    private var currentModelId: String? = null

    data class UtteranceRequest(val text: String, val utteranceId: String)

    init {
        scope.launch {
            settingsManager.ttsModelId.collectLatest { modelId ->
                if (modelId != currentModelId) {
                    currentModelId = modelId
                    initializeEngine(modelId)
                }
            }
        }
    }

    private suspend fun initializeEngine(modelId: String?) {
        withContext(Dispatchers.IO) {
            _state.value = TtsState.Initializing
            try {
                tts?.release()
                tts = null

                if (modelId == null) {
                    _state.value = TtsState.Error("No model selected")
                    return@withContext
                }

                val modelPath = modelRepository.getModelPath(modelId)
                if (modelPath == null) {
                    _state.value = TtsState.Error("Model files not found")
                    return@withContext
                }

                val vitsConfig = OfflineTtsVitsModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath
                )

                val modelConfig = OfflineTtsModelConfig(
                    vits = vitsConfig,
                    numThreads = 1,
                    debug = true
                )

                val config = OfflineTtsConfig(
                    model = modelConfig
                )
                
                tts = OfflineTts(context.assets, config)
                _state.value = TtsState.Ready
                startSynthesisLoop()
            } catch (e: Exception) {
                Log.e("SherpaTtsEngine", "Failed to initialize engine", e)
                _state.value = TtsState.Error(e.message ?: "Unknown initialization error")
            }
        }
    }

    private fun startSynthesisLoop() {
        synthesisJob?.cancel()
        synthesisJob = scope.launch(Dispatchers.IO) {
            while (true) {
                val request = queue.take()
                synthesizeAndPlay(request)
            }
        }
    }

    private fun synthesizeAndPlay(request: UtteranceRequest) {
        val engine = tts ?: return
        
        try {
            _state.value = TtsState.Speaking(request.utteranceId, 0, request.text.length, 0)
            
            val audio = engine.generate(request.text)
            val samples = audio.samples
            val sampleRate = audio.sampleRate

            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(audioUsage)
                        .setContentType(audioContentType)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 4))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.apply {
                write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                setPlaybackParams(playbackParams.setSpeed(playbackSpeed))
                setVolume(volume)
                play()
                
                // Wait for playback to finish
                val playTimeMs = (samples.size.toFloat() / sampleRate / playbackSpeed * 1000).toLong()
                Thread.sleep(playTimeMs)
            }
            
            _state.value = TtsState.Ready
        } catch (e: Exception) {
            Log.e("SherpaTtsEngine", "Synthesis failed", e)
            _state.value = TtsState.Error(e.message ?: "Synthesis failed")
        }
    }

    override fun speak(text: String, utteranceId: String) {
        stop()
        enqueue(text, utteranceId)
    }

    override fun enqueue(text: String, utteranceId: String) {
        queue.offer(UtteranceRequest(text, utteranceId))
    }

    override fun stop() {
        queue.clear()
        audioTrack?.stop()
        audioTrack?.flush()
        _state.value = TtsState.Ready
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        try {
            audioTrack?.playbackParams = audioTrack?.playbackParams?.setSpeed(speed) ?: return
        } catch (e: Exception) {
            // Might fail if not playing
        }
    }

    override fun setAudioAttributes(usage: Int, contentType: Int) {
        audioUsage = usage
        audioContentType = contentType
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        audioTrack?.setVolume(volume)
    }

    override fun release() {
        synthesisJob?.cancel()
        tts?.release()
        audioTrack?.release()
        tts = null
        audioTrack = null
    }
}
