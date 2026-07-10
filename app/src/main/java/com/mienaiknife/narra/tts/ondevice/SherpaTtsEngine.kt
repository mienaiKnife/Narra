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
import com.k2fsa.sherpa.onnx.*
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

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
    private val synthesizedQueue = Channel<SynthesizedAudioStream>(Channel.UNLIMITED)

    private var synthesisJob: Job? = null
    private var playbackJob: Job? = null
    private var monitorJob: Job? = null
    private var currentModelId: String? = null
    private var lastNoiseScale: Float = -1f
    private var lastLengthScale: Float = -1f
    private var currentModelType: TtsModelType? = null
    private var currentSessionId: Int = 0
    private var currentSampleRate: Int = -1
    private var samplesPerCharAverage: Float = 1800f // Default for ~12 chars/sec at 22050Hz

    private val activeStreams = java.util.Collections.synchronizedList(mutableListOf<StreamPlaybackInfo>())

    data class StreamPlaybackInfo(
        val stream: SynthesizedAudioStream,
        var startFrame: Long = -1,
        var totalFrames: Int,
        var isWriteFinished: Boolean = false,
        var boundaries: List<WordBoundary> = emptyList(),
        var fullSamplesAccumulator: MutableList<FloatArray> = mutableListOf(),
        var isFirstChunkEstimated: Boolean = false,
        var projectedTotalFrames: Int = 0,
    )

    data class UtteranceRequest(val text: String, val utteranceId: String, val sessionId: Int)

    data class WordBoundary(
        val startChar: Int,
        val endChar: Int,
        val startSample: Int,
        val endSample: Int,
    )

    sealed class AudioStreamEvent {
        data class Chunk(val samples: FloatArray) : AudioStreamEvent() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Chunk
                return samples.contentEquals(other.samples)
            }

            override fun hashCode(): Int = samples.contentHashCode()
        }
        object Finished : AudioStreamEvent()
    }

    data class SynthesizedAudioStream(
        val eventChannel: Channel<AudioStreamEvent>,
        val sampleRate: Int,
        val utteranceId: String,
        val text: String,
        val sessionId: Int,
    )

    private var currentSpeakerId: Int? = null

    init {
        scope.launch {
            combine(
                settingsManager.ttsModelId,
                settingsManager.sherpaNoiseScale,
                settingsManager.sherpaLengthScale,
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
                    var result = synthesizedQueue.tryReceive()
                    while (result.isSuccess) {
                        result.getOrNull()?.eventChannel?.close()
                        result = synthesizedQueue.tryReceive()
                    }
                }

                tts?.release()
                tts = null
                currentSampleRate = -1

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
                    silenceScale = 0.2f,
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
        lengthScale: Float,
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
                    lengthScale = lengthScale,
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
                    lengthScale = lengthScale,
                )
            }

            TtsModelType.KOKORO -> {
                kokoro = OfflineTtsKokoroModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    lengthScale = lengthScale,
                )
            }

            TtsModelType.ZIPVOICE -> {
                zipvoice = OfflineTtsZipVoiceModelConfig(
                    encoder = File(modelPath, "encoder.onnx").absolutePath,
                    decoder = File(modelPath, "decoder.onnx").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                )
            }
            TtsModelType.KITTEN -> {
                kitten = OfflineTtsKittenModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    dataDir = modelPath,
                    lengthScale = lengthScale,
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
                    tokenScoresJson = File(modelPath, "token_scores.json").absolutePath,
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
                    voiceStyle = File(modelPath, "voice_style.bin").absolutePath,
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
            numThreads = 4,
            debug = true,
            provider = "cpu",
        )
    }

    private fun startLoops() {
        synthesisJob?.cancel()
        playbackJob?.cancel()
        monitorJob?.cancel()

        synthesisJob = scope.launch(Dispatchers.Default) {
            for (request in utteranceQueue) {
                if (request.sessionId != currentSessionId) continue
                val engine = tts ?: continue
                try {
                    val eventChannel = Channel<AudioStreamEvent>(Channel.UNLIMITED)
                    val sampleRate = engine.sampleRate()

                    synthesizedQueue.send(
                        SynthesizedAudioStream(
                            eventChannel,
                            sampleRate,
                            request.utteranceId,
                            request.text,
                            request.sessionId,
                        ),
                    )

                    val genConfig = GenerationConfig(
                        speed = 1.0f,
                        sid = currentSpeakerId ?: 0,
                    )

                    val callback = object : SherpaTtsCallback() {
                        override fun invoke(samples: FloatArray): Int = if (request.sessionId == currentSessionId) {
                            eventChannel.trySend(AudioStreamEvent.Chunk(samples))
                            1
                        } else {
                            0
                        }
                    }

                    engine.generateWithConfigAndCallback(request.text, genConfig, callback)
                    eventChannel.trySend(AudioStreamEvent.Finished)
                } catch (e: Exception) {
                    Log.e("SherpaTtsEngine", "Synthesis failed", e)
                }
            }
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            for (stream in synthesizedQueue) {
                if (stream.sessionId != currentSessionId) {
                    Log.d("SherpaTtsEngine", "Skipping stale audio: ${stream.utteranceId}")
                    stream.eventChannel.close()
                    continue
                }
                playAudioStream(stream)
            }
        }

        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                updatePlaybackProgress()
                delay(20)
            }
        }
    }

    private fun updatePlaybackProgress() {
        val track = synchronized(this) { audioTrack } ?: return

        val head = try {
            if (track.state != AudioTrack.STATE_INITIALIZED) return
            track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        } catch (e: Exception) {
            // Track might have been released just after our null check
            return
        }

        synchronized(activeStreams) {
            val iterator = activeStreams.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.stream.sessionId != currentSessionId) {
                    iterator.remove()
                    continue
                }

                val relativeHead = if (info.startFrame != -1L) head - info.startFrame else -1L

                if (info.isWriteFinished && info.startFrame != -1L && relativeHead >= info.totalFrames) {
                    // This stream is finished playing
                    _state.value = TtsState.Finished(info.stream.utteranceId)
                    iterator.remove()
                    continue // IMPORTANT: Skip to next stream immediately
                }

                // If audio hasn't started playing yet but it's the current stream in the queue,
                // we report it as Speaking at position 0.
                if (info.startFrame == -1L || (relativeHead < info.totalFrames || !info.isWriteFinished)) {
                    // This is the currently playing (or about to play) stream

                    // If we have no boundaries AND it's not finished, we are in early synthesis.
                    if (info.boundaries.isEmpty() && !info.isWriteFinished) {
                        _state.value = TtsState.Buffering(info.stream.utteranceId)
                        break
                    }

                    // If we have boundaries, find current word
                    val currentWord = if (relativeHead >= 0 && info.boundaries.isNotEmpty()) {
                        info.boundaries.find {
                            relativeHead in it.startSample.toLong() until it.endSample.toLong()
                        } ?: if (!info.isWriteFinished) info.boundaries.firstOrNull() else info.boundaries.lastOrNull { relativeHead >= it.endSample }
                    } else if (info.boundaries.isNotEmpty() && info.startFrame == -1L) {
                        info.boundaries.firstOrNull()
                    } else {
                        null
                    }

                    if (currentWord != null) {
                        val newState = TtsState.Speaking(
                            info.stream.utteranceId,
                            currentWord.startChar,
                            currentWord.endChar,
                            relativeHead.coerceAtLeast(0).toInt(),
                        )

                        // Only update if state actually changed or significantly progressed
                        val oldState = _state.value
                        if (oldState !is TtsState.Speaking ||
                            oldState.utteranceId != newState.utteranceId ||
                            oldState.start != newState.start ||
                            oldState.end != newState.end ||
                            abs(oldState.frame - newState.frame) > 200
                        ) { // Even lower threshold for higher frequency
                            _state.value = newState
                        }
                    } else if (info.startFrame == -1L) {
                        _state.value = TtsState.Buffering(info.stream.utteranceId)
                    }

                    // We only update the state for the oldest active stream that is still playing or pending
                    break
                }
            }
        }
    }

    private suspend fun playAudioStream(stream: SynthesizedAudioStream) {
        try {
            // Check session before starting
            if (stream.sessionId != currentSessionId) return

            val sampleRate = stream.sampleRate

            val track = synchronized(this) {
                if (audioTrack == null || currentSampleRate != sampleRate) {
                    audioTrack?.let {
                        try {
                            it.flush()
                            it.stop()
                            it.release()
                        } catch (e: Exception) {
                            Log.w("SherpaTtsEngine", "Error releasing AudioTrack", e)
                        }
                    }

                    val bufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_FLOAT,
                    )

                    val newTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(audioUsage)
                                .setContentType(audioContentType)
                                .build(),
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        )
                        .setBufferSizeInBytes(maxOf(bufferSize, sampleRate * 4)) // ~250ms buffer
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    newTrack.playbackParams = newTrack.playbackParams.setSpeed(playbackSpeed)
                    newTrack.setVolume(volume)
                    newTrack.play()

                    audioTrack = newTrack
                    currentSampleRate = sampleRate
                    newTrack
                } else {
                    audioTrack!!
                }
            }

            // Ensure track is playing before writing
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }

            val info = StreamPlaybackInfo(
                stream = stream,
                totalFrames = 0,
                projectedTotalFrames = (stream.text.length * samplesPerCharAverage).toInt(),
            )

            activeStreams.add(info)

            for (event in stream.eventChannel) {
                if (stream.sessionId != currentSessionId) break

                when (event) {
                    is AudioStreamEvent.Chunk -> {
                        val samples = event.samples
                        var offset = 0
                        while (offset < samples.size && scope.isActive && stream.sessionId == currentSessionId) {
                            val currentTrack = synchronized(this@SherpaTtsEngine) { audioTrack } ?: break
                            if (currentTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                try {
                                    currentTrack.play()
                                } catch (e: Exception) {
                                    break
                                }
                            }
                            val toWrite = samples.size - offset
                            val written = try {
                                currentTrack.write(samples, offset, toWrite, AudioTrack.WRITE_NON_BLOCKING)
                            } catch (e: Exception) {
                                -1
                            }
                            if (written < 0) {
                                Log.e("SherpaTtsEngine", "AudioTrack write error: $written")
                                break
                            }
                            if (written == 0) {
                                delay(10) // Wait for buffer to clear
                                continue
                            }

                            // Capture the actual start frame when we first successfully write audio
                            if (info.startFrame == -1L) {
                                // For MODE_STREAM, the head position represents the frame currently being played.
                                // If we just wrote audio, the head hasn't reached it yet.
                                info.startFrame = (currentTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL)
                            }

                            offset += written
                        }
                        info.totalFrames += samples.size
                        info.fullSamplesAccumulator.add(samples)

                        // If synthesis is ongoing, we estimate boundaries for the first chunk
                        // to get highlighting moving as soon as possible.
                        if (info.totalFrames > 100) { // Even earlier
                            info.boundaries = estimateWordBoundaries(stream.text, info.projectedTotalFrames, null)
                            info.isFirstChunkEstimated = true
                        }
                    }
                    is AudioStreamEvent.Finished -> {
                        val allSamples = FloatArray(info.totalFrames)
                        var offset = 0
                        for (chunk in info.fullSamplesAccumulator) {
                            System.arraycopy(chunk, 0, allSamples, offset, chunk.size)
                            offset += chunk.size
                        }

                        // Update our global average for future projections
                        if (stream.text.length > 10) {
                            val currentAverage = info.totalFrames.toFloat() / stream.text.length
                            samplesPerCharAverage = (samplesPerCharAverage * 0.8f) + (currentAverage * 0.2f)
                        }

                        info.boundaries = estimateWordBoundaries(stream.text, info.totalFrames, allSamples)
                        info.fullSamplesAccumulator.clear()
                        info.isWriteFinished = true
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SherpaTtsEngine", "Playback failed", e)
            _state.value = TtsState.Error(e.message ?: "Playback failed")
        } finally {
            stream.eventChannel.close()
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
            // Reset sample rate to force AudioTrack recreation if needed next time
            currentSampleRate = -1
            while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
            var result = synthesizedQueue.tryReceive()
            while (result.isSuccess) {
                result.getOrNull()?.eventChannel?.close()
                result = synthesizedQueue.tryReceive()
            }
            activeStreams.clear()

            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        try {
                            track.pause()
                            track.flush()
                            track.stop()
                        } catch (e: Exception) {
                            // Ignore errors during stop/flush as track might already be in an invalid state
                        }
                        track.release()
                    }
                }
                audioTrack = null
                currentSampleRate = -1
            } catch (e: Exception) {
                Log.e("SherpaTtsEngine", "Error stopping AudioTrack", e)
            }
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

    // NOTE: As of Sherpa-ONNX v1.13.4, native word-level timestamps are supported in the C++ core
    // but not yet exposed in the Java/JNI bindings (GeneratedAudio only contains samples/sampleRate).
    // Using this heuristic until the Java API is updated in a future release.
    private fun estimateWordBoundaries(text: String, totalSamples: Int, samples: FloatArray?): List<WordBoundary> {
        val boundaries = mutableListOf<WordBoundary>()
        if (text.isEmpty() || totalSamples == 0) return boundaries

        // 1. Detect actual speech bounds if we have the full audio
        var speechStart = 0
        var speechEnd = totalSamples

        if (samples != null) {
            val bounds = detectSpeechBounds(samples)
            // Only trust bounds if they don't seem like the whole thing anyway
            if (bounds.first > samples.size * 0.01 || bounds.second < samples.size * 0.99) {
                speechStart = bounds.first
                speechEnd = bounds.second
            }
        }

        val speechSamples = (speechEnd - speechStart).coerceAtLeast(1)

        // 2. Calculate weighted length of the text
        val weights = text.map { getCharWeight(it) }
        val totalWeight = weights.sum().coerceAtLeast(1.0f)

        // 3. Find all non-whitespace tokens (words)
        val regex = Regex("\\S+")
        val matches = regex.findAll(text).toList()

        if (matches.isEmpty()) {
            // Fallback for single word or no whitespace
            if (text.trim().isEmpty()) return emptyList()
            boundaries.add(WordBoundary(0, text.length, speechStart, speechEnd))
            return boundaries
        }

        matches.forEach { match ->
            val startChar = match.range.first
            val endChar = match.range.last + 1

            // Weight-based heuristic: sum weights of characters before and within the word
            val weightBefore = weights.take(startChar).sum()
            val weightInWord = weights.subList(startChar, endChar).sum()

            val wordStartSample = speechStart + (weightBefore / totalWeight * speechSamples).toInt()
            val wordEndSample = speechStart + ((weightBefore + weightInWord) / totalWeight * speechSamples).toInt()

            boundaries.add(WordBoundary(startChar, endChar, wordStartSample, wordEndSample))
        }

        return boundaries
    }

    private fun detectSpeechBounds(samples: FloatArray): Pair<Int, Int> {
        val threshold = 0.005f // More sensitive threshold
        var start = 0
        // Search first 40% for start
        val startLimit = (samples.size * 0.4).toInt()
        while (start < startLimit && start < samples.size && abs(samples[start]) < threshold) {
            start++
        }

        var end = samples.size - 1
        // Search last 40% for end
        val endLimit = (samples.size * 0.6).toInt()
        while (end > endLimit && end > 0 && abs(samples[end]) < threshold) {
            end--
        }

        return Pair(start, end)
    }

    private fun getCharWeight(c: Char): Float = when (c) {
        '.', '!', '?' -> 3.0f // Significant pause
        ',', ';', ':', '-' -> 2.0f // Medium pause
        ' ' -> 1.2f // Gap between words
        '(', ')', '[', ']', '{', '}', '"', '\'' -> 0.1f // Usually quick
        else -> 1.0f // Standard character duration
    }

    override fun release() {
        synthesisJob?.cancel()
        playbackJob?.cancel()
        monitorJob?.cancel()
        tts?.release()
        audioTrack?.release()
        tts = null
        audioTrack = null
    }
}
