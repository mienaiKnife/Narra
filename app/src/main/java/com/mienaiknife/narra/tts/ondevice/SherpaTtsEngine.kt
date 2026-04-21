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
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import com.k2fsa.sherpa.onnx.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SherpaTtsEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
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

    private val utteranceQueue = Channel<UtteranceRequest>(Channel.UNLIMITED)
    private val synthesizedQueue = Channel<SynthesizedAudio>(Channel.BUFFERED)
    
    private var synthesisJob: Job? = null
    private var playbackJob: Job? = null
    private var currentModelId: String? = null

    data class UtteranceRequest(val text: String, val utteranceId: String)
    data class SynthesizedAudio(val samples: FloatArray, val sampleRate: Int, val utteranceId: String, val text: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SynthesizedAudio

            if (!samples.contentEquals(other.samples)) return false
            if (sampleRate != other.sampleRate) return false
            if (utteranceId != other.utteranceId) return false
            if (text != other.text) return false

            return true
        }

        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + utteranceId.hashCode()
            result = 31 * result + text.hashCode()
            return result
        }
    }

    private var currentSpeakerId: Int? = null

    init {
        scope.launch {
            combine(
                settingsManager.ttsModelId,
                settingsManager.sherpaNoiseScale,
                settingsManager.sherpaLengthScale
            ) { modelId, noiseScale, lengthScale ->
                Triple(modelId, noiseScale, lengthScale)
            }.collectLatest { (modelId, noiseScale, lengthScale) ->
                initializeEngine(modelId, noiseScale, lengthScale)
            }
        }

        scope.launch {
            settingsManager.ttsSpeakerId.collect { speakerId ->
                currentSpeakerId = speakerId
            }
        }

        scope.launch {
            settingsManager.sherpaSpeed.collect { speed ->
                setPlaybackSpeed(speed)
            }
        }
    }

    private suspend fun initializeEngine(modelId: String?, noiseScale: Float, lengthScale: Float) {
        withContext(Dispatchers.IO) {
            _state.value = TtsState.Initializing
            try {
                // Stop any current synthesis/playback loops
                synthesisJob?.cancel()
                playbackJob?.cancel()

                // Clear queues
                while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
                while (synthesizedQueue.tryReceive().isSuccess) { /* consume */ }

                tts?.release()
                tts = null

                if (modelId == null) {
                    _state.value = TtsState.Error("No model selected")
                    return@withContext
                }

                val models = modelRepository.getAvailableModels().first()
                val modelMetadata = models.find { it.id == modelId }
                val modelPath = modelRepository.getModelPath(modelId)

                if (modelPath == null || modelMetadata == null) {
                    _state.value = TtsState.Error("Model files not found")
                    return@withContext
                }

                val modelConfig = createModelConfig(modelMetadata, modelPath, noiseScale, lengthScale)
                val config = OfflineTtsConfig(
                    model = modelConfig,
                    ruleFsts = "",
                    ruleFars = "",
                    maxNumSentences = 1,
                    silenceScale = 0.2f
                )

                tts = OfflineTts(context.assets, config)
                _state.value = TtsState.Ready
                startLoops()
            } catch (e: Exception) {
                Log.e("SherpaTtsEngine", "Failed to initialize engine", e)
                _state.value = TtsState.Error(e.message ?: "Unknown initialization error")
            }
        }
    }

    private fun createModelConfig(
        model: TtsModel,
        modelPath: String,
        noiseScale: Float,
        lengthScale: Float
    ): OfflineTtsModelConfig {
        var vits = OfflineTtsVitsModelConfig()
        var matcha = OfflineTtsMatchaModelConfig()
        var kokoro = OfflineTtsKokoroModelConfig()
        var zipvoice = OfflineTtsZipVoiceModelConfig()
        var kitten = OfflineTtsKittenModelConfig()
        var pocket = OfflineTtsPocketModelConfig()
        var supertonic = OfflineTtsSupertonicModelConfig()

        when (model.type) {
            TtsModelType.VITS -> {
                vits = OfflineTtsVitsModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    lexicon = "",
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    noiseScale = noiseScale,
                    noiseScaleW = 0.8f,
                    lengthScale = lengthScale
                )
            }

            TtsModelType.MATCHA -> {
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = File(modelPath, "model.onnx").absolutePath,
                    vocoder = File(modelPath, "vocoder.onnx").absolutePath,
                    lexicon = "",
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    noiseScale = noiseScale,
                    lengthScale = lengthScale
                )
            }

            TtsModelType.KOKORO -> {
                kokoro = OfflineTtsKokoroModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    lengthScale = lengthScale
                )
            }

            TtsModelType.ZIPVOICE -> {
                zipvoice = OfflineTtsZipVoiceModelConfig(
                    encoder = File(modelPath, "encoder.onnx").absolutePath,
                    decoder = File(modelPath, "decoder.onnx").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath
                )
            }
            TtsModelType.KITTEN -> {
                kitten = OfflineTtsKittenModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    lengthScale = lengthScale
                )
            }
            TtsModelType.POCKET -> {
                pocket = OfflineTtsPocketModelConfig(
                    lmFlow = File(modelPath, "lm_flow.onnx").absolutePath,
                    lmMain = File(modelPath, "lm_main.onnx").absolutePath,
                    encoder = File(modelPath, "encoder.onnx").absolutePath,
                    decoder = File(modelPath, "decoder.onnx").absolutePath,
                    textConditioner = File(modelPath, "text_conditioner.onnx").absolutePath,
                    vocabJson = File(modelPath, "vocab.json").absolutePath,
                    tokenScoresJson = File(modelPath, "token_scores.json").absolutePath
                )
            }
            TtsModelType.SUPERTONIC -> {
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = File(modelPath, "duration_predictor.onnx").absolutePath,
                    textEncoder = File(modelPath, "text_encoder.onnx").absolutePath,
                    vectorEstimator = File(modelPath, "vector_estimator.onnx").absolutePath,
                    vocoder = File(modelPath, "vocoder.onnx").absolutePath,
                    ttsJson = File(modelPath, "tts.json").absolutePath,
                    unicodeIndexer = File(modelPath, "unicode_indexer.onnx").absolutePath,
                    voiceStyle = File(modelPath, "voice_style.bin").absolutePath
                )
            }
        }

        return OfflineTtsModelConfig(
            vits = vits,
            matcha = matcha,
            kokoro = kokoro,
            zipvoice = zipvoice,
            kitten = kitten,
            pocket = pocket,
            supertonic = supertonic,
            numThreads = 1,
            debug = true,
            provider = "cpu"
        )
    }

    private fun startLoops() {
        synthesisJob?.cancel()
        playbackJob?.cancel()

        synthesisJob = scope.launch(Dispatchers.Default) {
            for (request in utteranceQueue) {
                val engine = tts ?: continue
                try {
                    val audio = engine.generate(request.text, currentSpeakerId ?: 0)
                    synthesizedQueue.send(SynthesizedAudio(audio.samples, audio.sampleRate, request.utteranceId, request.text))
                } catch (e: Exception) {
                    Log.e("SherpaTtsEngine", "Synthesis failed", e)
                }
            }
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            for (audio in synthesizedQueue) {
                playAudio(audio)
            }
        }
    }

    private suspend fun playAudio(audio: SynthesizedAudio) {
        try {
            _state.value = TtsState.Speaking(audio.utteranceId, 0, audio.text.length, 0)
            
            val samples = audio.samples
            val sampleRate = audio.sampleRate

            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            if (audioTrack == null || audioTrack?.sampleRate != sampleRate) {
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }

            audioTrack?.apply {
                if (state == AudioTrack.STATE_UNINITIALIZED) return@apply
                
                setPlaybackParams(playbackParams.setSpeed(playbackSpeed))
                setVolume(volume)
                if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                    play()
                }

                val startHeadPosition = playbackHeadPosition
                var offset = 0
                val totalSamples = samples.size
                
                while (offset < totalSamples) {
                    val toWrite = totalSamples - offset
                    val written = write(samples, offset, toWrite, AudioTrack.WRITE_NON_BLOCKING)
                    if (written < 0) {
                        Log.e("SherpaTtsEngine", "AudioTrack write error: $written")
                        break
                    }
                    offset += written
                    
                    val currentHead = playbackHeadPosition
                    val playedFrames = (currentHead - startHeadPosition).coerceAtLeast(0)
                    
                    // Throttle state updates to avoid overwhelming the UI
                    if (offset % 4000 == 0 || offset >= totalSamples) {
                        _state.value = TtsState.Speaking(
                            audio.utteranceId,
                            0,
                            audio.text.length,
                            playedFrames
                        )
                    }
                    
                    if (written == 0) {
                        // Buffer full, wait a bit longer to be power efficient
                        kotlinx.coroutines.delay(50)
                    }
                }
                
                // Wait for the remainder of the audio to finish playing
                val expectedEndHeadPosition = startHeadPosition + totalSamples
                while (playbackHeadPosition < expectedEndHeadPosition && playState == AudioTrack.PLAYSTATE_PLAYING) {
                    _state.value = TtsState.Speaking(
                        audio.utteranceId,
                        0,
                        audio.text.length,
                        playbackHeadPosition - startHeadPosition
                    )
                    kotlinx.coroutines.delay(30)
                }
            }
            
            _state.value = TtsState.Ready
        } catch (e: Exception) {
            Log.e("SherpaTtsEngine", "Playback failed", e)
            _state.value = TtsState.Error(e.message ?: "Playback failed")
        }
    }

    override fun speak(text: String, utteranceId: String) {
        stop()
        enqueue(text, utteranceId)
    }

    override fun enqueue(text: String, utteranceId: String) {
        utteranceQueue.trySend(UtteranceRequest(text, utteranceId))
    }

    override fun stop() {
        while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
        while (synthesizedQueue.tryReceive().isSuccess) { /* consume */ }
        
        audioTrack?.stop()
        audioTrack?.flush()
        _state.value = TtsState.Ready
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        scope.launch(Dispatchers.Main) {
            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.playbackParams = track.playbackParams.setSpeed(speed)
                    }
                }
            } catch (e: Exception) {
                Log.e("SherpaTtsEngine", "Failed to set playback speed", e)
            }
        }
    }

    override fun setAudioAttributes(usage: Int, contentType: Int) {
        audioUsage = usage
        audioContentType = contentType
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        scope.launch(Dispatchers.Main) {
            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.setVolume(volume)
                    }
                }
            } catch (e: Exception) {
                Log.e("SherpaTtsEngine", "Failed to set volume", e)
            }
        }
    }

    override fun release() {
        synthesisJob?.cancel()
        playbackJob?.cancel()
        tts?.release()
        audioTrack?.release()
        tts = null
        audioTrack = null
    }
}
