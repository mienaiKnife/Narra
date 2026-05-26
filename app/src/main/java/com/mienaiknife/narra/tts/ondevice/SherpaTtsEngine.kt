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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SherpaTtsEngine @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager,
) : TtsEngine {

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var playbackSpeed = 1.0f
    private var volume = 1.0f
    private var audioUsage = AudioAttributes.USAGE_MEDIA
    private var audioContentType = AudioAttributes.CONTENT_TYPE_SPEECH

    private val utteranceQueue = Channel<UtteranceRequest>(Channel.UNLIMITED)
    private val synthesizedQueue = Channel<SynthesizedAudio>(2)
    
    private var synthesisJob: Job? = null
    private var playbackJob: Job? = null
    private var currentModelId: String? = null
    private var lastNoiseScale: Float = -1f
    private var lastLengthScale: Float = -1f
    private var currentModelType: TtsModelType? = null
    private var currentSessionId: Int = 0

    data class UtteranceRequest(val text: String, val utteranceId: String, val sessionId: Int)
    
    data class WordBoundary(
        val startChar: Int,
        val endChar: Int,
        val startSample: Int,
        val endSample: Int,
    )

    data class SynthesizedAudio(
        val samples: FloatArray,
        val sampleRate: Int,
        val utteranceId: String,
        val text: String,
        val sessionId: Int,
        val wordBoundaries: List<WordBoundary> = emptyList()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SynthesizedAudio

            if (!samples.contentEquals(other.samples)) return false
            if (sampleRate != other.sampleRate) return false
            if (utteranceId != other.utteranceId) return false
            if (text != other.text) return false
            return sessionId == other.sessionId
        }

        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = (31 * result) + sampleRate
            result = (31 * result) + utteranceId.hashCode()
            result = (31 * result) + text.hashCode()
            result = (31 * result) + sessionId
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
            }
            .distinctUntilChanged()
            .collectLatest { (modelId, noiseScale, lengthScale) ->
                // Delay for scales to avoid rapid re-init while sliding
                if (modelId == currentModelId) {
                    delay(500)
                }
                initializeEngine(modelId, noiseScale, lengthScale)
            }
        }

        scope.launch {
            settingsManager.ttsSpeakerId.collect { speakerId ->
                if (currentSpeakerId != speakerId) {
                    currentSpeakerId = speakerId
                    // For Kokoro, we can change speaker without re-init, 
                    // but we might want to restart current synthesis to apply it immediately
                    // TODO: Optional: restart current paragraph synthesis if currentModelType == TtsModelType.KOKORO && _state.value is TtsState.Speaking
                }
            }
        }

        scope.launch {
            settingsManager.sherpaSpeed.collect { speed ->
                setPlaybackSpeed(speed)
            }
        }
    }

    private suspend fun initializeEngine(modelId: String?, noiseScale: Float, lengthScale: Float) {
        val models = modelRepository.getAvailableModels().first()
        val modelMetadata = models.find { it.id == modelId }
        val modelType = modelMetadata?.type

        if (
            (modelId == currentModelId) &&
            (modelType == TtsModelType.KOKORO || noiseScale == lastNoiseScale) &&
            (lengthScale == lastLengthScale) &&
            (tts != null)
        ) {
            return
        }

        withContext(Dispatchers.IO) {
            _state.value = TtsState.Initializing
            try {
                synchronized(this@SherpaTtsEngine) {
                    currentSessionId++
                    // Stop any current synthesis/playback loops
                    synthesisJob?.cancel()
                    playbackJob?.cancel()

                    // Clear queues
                    while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
                    while (synthesizedQueue.tryReceive().isSuccess) { /* consume */ }
                }

                tts?.release()
                tts = null

                if (modelId == null) {
                    Log.d("SherpaTtsEngine", "Engine skipped initialization: no model selected")
                    _state.value = TtsState.Idle
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

                tts = OfflineTts(null, config)
                currentModelId = modelId
                currentModelType = modelType
                lastNoiseScale = noiseScale
                lastLengthScale = lengthScale
                Log.i("SherpaTtsEngine", "Engine initialized successfully with model: $modelId, type: $modelType")
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
                    dataDir = File(modelPath, "espeak-ng-data").absolutePath,
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
                if (request.sessionId != currentSessionId) continue
                val engine = tts ?: continue
                try {
                    val audio = engine.generate(request.text, currentSpeakerId ?: 0)
                    if (request.sessionId != currentSessionId) continue
                    
                    val boundaries = estimateWordBoundaries(request.text, audio.samples.size, audio.samples)
                    
                    synthesizedQueue.send(
                        SynthesizedAudio(
                            audio.samples,
                            audio.sampleRate,
                            request.utteranceId,
                            request.text,
                            request.sessionId,
                            boundaries
                        )
                    )
                } catch (e: Exception) {
                    Log.e("SherpaTtsEngine", "Synthesis failed", e)
                }
            }
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            for (audio in synthesizedQueue) {
                if (audio.sessionId != currentSessionId) continue
                playAudio(audio)
            }
        }
    }

    private suspend fun playAudio(audio: SynthesizedAudio) {
        try {
            // Initial state update
            _state.value = TtsState.Speaking(audio.utteranceId, 0, 0, 0)
            
            val samples = audio.samples
            val sampleRate = audio.sampleRate
            val wordBoundaries = audio.wordBoundaries

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
                
                playbackParams = playbackParams.setSpeed(playbackSpeed)
                setVolume(volume)
                if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                    play()
                }

                val startHeadPosition = playbackHeadPosition
                var offset = 0
                val totalSamples = samples.size
                
                while (offset < totalSamples && scope.isActive) {
                    if (audio.sessionId != currentSessionId) break
                    
                    val toWrite = totalSamples - offset
                    val written = write(samples, offset, toWrite, AudioTrack.WRITE_NON_BLOCKING)
                    if (written < 0) {
                        Log.e("SherpaTtsEngine", "AudioTrack write error: $written")
                        break
                    }
                    offset += written
                    
                    val currentHead = playbackHeadPosition
                    val playedFrames = (currentHead - startHeadPosition).coerceAtLeast(0)
                    
                    // Find the current word based on played frames
                    val currentWord = wordBoundaries.find { 
                        playedFrames in it.startSample until it.endSample 
                    } ?: wordBoundaries.lastOrNull { playedFrames >= it.endSample }

                    // Throttle state updates to avoid overwhelming the UI
                    if (offset % 4000 == 0 || offset >= totalSamples) {
                        _state.value = TtsState.Speaking(
                            audio.utteranceId,
                            currentWord?.startChar ?: 0,
                            currentWord?.endChar ?: 0,
                            playedFrames
                        )
                    }
                    
                    if (written == 0) {
                        // Buffer full, wait a bit longer to be power efficient
                        delay(100)
                    }
                }
                
                // Wait for the remainder of the audio to finish playing
                val expectedEndHeadPosition = startHeadPosition + totalSamples
                while (playbackHeadPosition < expectedEndHeadPosition && 
                    playState == AudioTrack.PLAYSTATE_PLAYING &&
                    scope.isActive &&
                    audio.sessionId == currentSessionId) {
                    
                    val playedFrames = (playbackHeadPosition - startHeadPosition).coerceAtLeast(0)
                    val currentWord = wordBoundaries.find { 
                        playedFrames in it.startSample until it.endSample 
                    } ?: wordBoundaries.lastOrNull { playedFrames >= it.endSample }

                    _state.value = TtsState.Speaking(
                        audio.utteranceId,
                        currentWord?.startChar ?: 0,
                        currentWord?.endChar ?: 0,
                        playedFrames
                    )
                    delay(30)
                }
            }
            
            _state.value = TtsState.Ready
        } catch (e: Exception) {
            Log.e("SherpaTtsEngine", "Playback failed", e)
            _state.value = TtsState.Error(e.message ?: "Playback failed")
        }
    }

    override fun speak(text: String, utteranceId: String) {
        if (tts == null && _state.value is TtsState.Idle) {
            _state.value = TtsState.Error("No Sherpa-ONNX model selected")
            return
        }
        stop()
        enqueue(text, utteranceId)
    }

    override fun enqueue(text: String, utteranceId: String) {
        if (tts == null && _state.value is TtsState.Idle) {
            _state.value = TtsState.Error("No Sherpa-ONNX model selected")
            return
        }
        utteranceQueue.trySend(UtteranceRequest(text, utteranceId, currentSessionId))
    }

    override fun stop() {
        synchronized(this) {
            currentSessionId++
            while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
            while (synthesizedQueue.tryReceive().isSuccess) { /* consume */ }
        }
        
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.flush()
                }
            }
        } catch (e: Exception) {
            Log.e("SherpaTtsEngine", "Error stopping AudioTrack", e)
        }

        if (_state.value !is TtsState.Initializing) {
            _state.value = TtsState.Ready
        }
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

    private fun estimateWordBoundaries(text: String, totalSamples: Int, samples: FloatArray): List<WordBoundary> {
        val boundaries = mutableListOf<WordBoundary>()
        if (text.isEmpty() || totalSamples == 0) return boundaries

        // 1. Detect actual speech bounds to avoid counting silence in the heuristic
        val (speechStart, speechEnd) = detectSpeechBounds(samples)
        val speechSamples = (speechEnd - speechStart).coerceAtLeast(0)
        
        if (speechSamples == 0) return boundaries

        // 2. Calculate weighted length of the text
        val weights = text.map { getCharWeight(it) }
        val totalWeight = weights.sum()
        
        if (totalWeight == 0f) return boundaries

        // 3. Find all non-whitespace tokens (words)
        val regex = Regex("\\S+")
        val matches = regex.findAll(text).toList()
        
        if (matches.isEmpty()) return boundaries

        matches.forEach { match ->
            val startChar = match.range.first
            val endChar = match.range.last + 1
            
            // Weight-based heuristic: sum weights of characters before and within the word
            val weightBefore = weights.take(startChar).sum()
            val weightInWord = weights.subList(startChar, endChar).sum()
            
            val startSample = speechStart + (weightBefore / totalWeight * speechSamples).toInt()
            val endSample = speechStart + ((weightBefore + weightInWord) / totalWeight * speechSamples).toInt()
            
            boundaries.add(WordBoundary(startChar, endChar, startSample, endSample))
        }
        
        return boundaries
    }

    private fun detectSpeechBounds(samples: FloatArray): Pair<Int, Int> {
        val threshold = 0.01f 
        var start = 0
        // Search first 20% for start
        val startLimit = (samples.size * 0.2).toInt()
        while (start < startLimit && start < samples.size && abs(samples[start]) < threshold) {
            start++
        }
        
        var end = samples.size - 1
        // Search last 20% for end
        val endLimit = (samples.size * 0.8).toInt()
        while (end > endLimit && end > 0 && abs(samples[end]) < threshold) {
            end--
        }
        
        // If we didn't find clear bounds, use a small buffer
        if (start >= startLimit) start = (samples.size * 0.05).toInt()
        if (end <= endLimit) end = (samples.size * 0.95).toInt()
        
        return Pair(start, end)
    }

    private fun getCharWeight(c: Char): Float {
        return when (c) {
            '.', '!', '?' -> 3.0f // Longest pause
            ',', ';', ':', '-' -> 2.0f // Medium pause
            ' ' -> 0.6f // Slight gap between words
            else -> 1.0f // Standard character duration
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
