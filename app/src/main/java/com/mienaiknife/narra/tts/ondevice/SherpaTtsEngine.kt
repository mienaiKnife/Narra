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
import com.mienaiknife.narra.domain.TtsErrorReason
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
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SherpaTtsEngine @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager,
) : TtsEngine {

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var tts: OfflineTts? = null

    /**
     * Words the engine's lexicon can pronounce. Built on the IO dispatcher during initialization
     * and read by the synthesis loop, so it is volatile; `null` means "do not rewrite unknown
     * words" (no model, a frontend without a lexicon, or a failed index build).
     */
    @Volatile
    private var lexiconIndex: LexiconIndex? = null

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

    @Volatile
    private var currentSessionId: Int = 0
    private var currentSampleRate: Int = -1

    /**
     * Frames queued to the current [AudioTrack] since it was created. Writes are non-blocking and
     * run ahead of playback, so a stream starts at this position, not at the playback head observed
     * when its first sample is written.
     */
    private var framesWrittenToTrack: Long = 0

    private val activeStreams = java.util.Collections.synchronizedList(mutableListOf<StreamPlaybackInfo>())

    internal data class StreamPlaybackInfo(
        val stream: SynthesizedAudioStream,
        var startFrame: Long = -1,
        var totalFrames: Int,
        var isWriteFinished: Boolean = false,
        var boundaries: List<WordBoundary> = emptyList(),
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

        /**
         * Word boundaries for one synthesized sentence, in stream frames and character offsets.
         * Sent before that sentence's audio so highlighting is ready as soon as it starts playing.
         */
        data class Boundaries(val boundaries: List<WordBoundary>) : AudioStreamEvent()
        object Finished : AudioStreamEvent()
    }

    internal data class SynthesizedAudioStream(
        val eventChannel: Channel<AudioStreamEvent>,
        val sampleRate: Int,
        val utteranceId: String,
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
                        delay(500.milliseconds)
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

                    // Release the previous engine under the same lock that guards `tts`, so no
                    // synthesis/playback thread can be using it while it is freed.
                    tts?.release()
                    tts = null
                    lexiconIndex = null
                    currentSampleRate = -1
                }

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

                val lexiconPath = if (requiresLexicon(modelMetadata.type)) {
                    val path = modelRepository.ensureLexiconAvailable(modelMetadata.language) ?: run {
                        Log.e(
                            "SherpaTtsEngine",
                            "No lexicon available for language '${modelMetadata.language}'",
                        )
                        _state.value = TtsState.Error(
                            "No pronunciation lexicon available for language " +
                                "'${modelMetadata.language}'. Sherpa-ONNX 2.x cannot synthesize " +
                                "this model without one.",
                        )
                        return@withContext
                    }
                    // Index the words before publishing `tts`: the synthesis loop uses this to
                    // rewrite out-of-vocabulary words the native frontend would silently drop.
                    lexiconIndex = loadLexiconIndex(path)
                    path
                } else {
                    lexiconIndex = null
                    ""
                }

                val modelConfig = createModelConfig(modelMetadata, modelPath, lexiconPath, noiseScale, lengthScale)
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

    /**
     * Builds the lexicon index used to rewrite out-of-vocabulary words. It runs on the IO
     * dispatcher and a failure is not fatal: without an index, synthesis still works and unknown
     * words are skipped exactly as before.
     */
    private fun loadLexiconIndex(lexiconPath: String): LexiconIndex? = try {
        LexiconIndex.load(File(lexiconPath))
    } catch (e: Exception) {
        Log.e("SherpaTtsEngine", "Could not index the pronunciation lexicon: $lexiconPath", e)
        null
    }

    private fun createModelConfig(
        model: TtsModel,
        modelPath: String,
        lexiconPath: String,
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
                    lexicon = lexiconPath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    noiseScale = noiseScale,
                    noiseScaleW = 0.8f,
                    lengthScale = lengthScale,
                )
            }

            TtsModelType.MATCHA -> {
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = File(modelPath, "model.onnx").absolutePath,
                    vocoder = File(modelPath, "vocoder.onnx").absolutePath,
                    lexicon = lexiconPath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    noiseScale = noiseScale,
                    lengthScale = lengthScale,
                )
            }

            TtsModelType.KOKORO -> {
                kokoro = OfflineTtsKokoroModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    lexicon = lexiconPath,
                    lengthScale = lengthScale,
                )
            }

            TtsModelType.ZIPVOICE -> {
                zipvoice = OfflineTtsZipVoiceModelConfig(
                    encoder = File(modelPath, "encoder.onnx").absolutePath,
                    decoder = File(modelPath, "decoder.onnx").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    lexicon = lexiconPath,
                )
            }
            TtsModelType.KITTEN -> {
                kitten = OfflineTtsKittenModelConfig(
                    model = File(modelPath, "model.onnx").absolutePath,
                    voices = File(modelPath, "voices.bin").absolutePath,
                    tokens = File(modelPath, "tokens.txt").absolutePath,
                    lengthScale = lengthScale,
                    lexicon = lexiconPath,
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
                val eventChannel = Channel<AudioStreamEvent>(Channel.UNLIMITED)
                try {
                    // Digits and OOV words are not pronounceable by the 2.x lexicon, so rewrite
                    // them before synthesis and keep the mapping for highlighting.
                    val speech = SpeechTextPreparer.prepare(request.text, lexiconIndex)
                    if (speech.text.isBlank()) {
                        eventChannel.trySend(AudioStreamEvent.Finished)
                        continue
                    }

                    val sampleRate = engine.sampleRate()

                    synthesizedQueue.send(
                        SynthesizedAudioStream(
                            eventChannel,
                            sampleRate,
                            request.utteranceId,
                            request.sessionId,
                        ),
                    )

                    val genConfig = GenerationConfig(
                        speed = 1.0f,
                        sid = currentSpeakerId ?: 0,
                    )

                    var framesGenerated = 0
                    for (sentence in splitIntoSentences(speech.text)) {
                        if (request.sessionId != currentSessionId) break
                        val sentenceText = speech.text.substring(sentence.first, sentence.last + 1)
                        if (sentenceText.none { it.isLetterOrDigit() }) continue

                        // One `generate` call per sentence segment. Sherpa's frontend already runs
                        // sentence by sentence (`maxNumSentences = 1`), so this does not change the
                        // audio, but it makes each segment's exact sample span known. Word
                        // highlighting then only has to be estimated within a segment instead of
                        // accumulating error across a whole paragraph.
                        val chunks = mutableListOf<FloatArray>()
                        var sentenceFrames = 0
                        val callback = object : SherpaTtsCallback() {
                            override fun invoke(samples: FloatArray): Int {
                                if (request.sessionId != currentSessionId) return 0
                                chunks.add(samples)
                                sentenceFrames += samples.size
                                return 1
                            }
                        }

                        engine.generateWithConfigAndCallback(sentenceText, genConfig, callback)
                        if (request.sessionId != currentSessionId) break

                        val sentenceSamples = concatenate(chunks, sentenceFrames)
                        val boundaries = estimateWordBoundaries(
                            sentenceText,
                            sentenceFrames,
                            sentenceSamples,
                        ).map { boundary ->
                            boundary.copy(
                                startChar = boundary.startChar + sentence.first,
                                endChar = boundary.endChar + sentence.first,
                                startSample = boundary.startSample + framesGenerated,
                                endSample = boundary.endSample + framesGenerated,
                            )
                        }

                        // Boundaries go first so playback has them before the audio is queued.
                        val mapped = mapToSource(speech, boundaries)
                        if (mapped.isNotEmpty()) {
                            eventChannel.trySend(AudioStreamEvent.Boundaries(mapped))
                        }
                        if (sentenceFrames > 0) {
                            eventChannel.trySend(AudioStreamEvent.Chunk(sentenceSamples))
                        }
                        framesGenerated += sentenceFrames
                    }
                    eventChannel.trySend(AudioStreamEvent.Finished)
                } catch (e: Exception) {
                    Log.e("SherpaTtsEngine", "Synthesis failed", e)
                    _state.value = TtsState.Error(e.message ?: "Synthesis failed")
                } finally {
                    // Always close the channel; otherwise playAudioStream blocks forever on the
                    // channel loop and playback is wedged after a synthesis error.
                    eventChannel.close()
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
                delay(20.milliseconds)
            }
        }
    }

    private fun updatePlaybackProgress() {
        val track = synchronized(this) { audioTrack } ?: return

        val head = try {
            if (track.state != AudioTrack.STATE_INITIALIZED) return
            track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
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

                val relativeHead = if (info.startFrame != -1L) {
                    framesPlayedSince(head, info.startFrame)
                } else {
                    -1L
                }

                if (
                    info.isWriteFinished &&
                    (info.totalFrames == 0 || (info.startFrame != -1L && relativeHead >= info.totalFrames))
                ) {
                    // This stream is finished playing (or produced no audio at all)
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

                    // The spoken word is the last one that has started. Anchoring to the last
                    // start (instead of a containment test) also keeps the most recent word
                    // highlighted while the next sentence is still being synthesized.
                    val currentWord = if (relativeHead >= 0 && info.boundaries.isNotEmpty()) {
                        info.boundaries.lastOrNull { relativeHead >= it.startSample.toLong() }
                            ?: info.boundaries.firstOrNull()
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
                        .setBufferSizeInBytes(maxOf(bufferSize, sampleRate * 4)) // at least 1s (4 bytes per mono float frame)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    newTrack.playbackParams = newTrack.playbackParams.setSpeed(playbackSpeed)
                    newTrack.setVolume(volume)
                    newTrack.play()

                    audioTrack = newTrack
                    currentSampleRate = sampleRate
                    framesWrittenToTrack = 0
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
            )

            synchronized(activeStreams) { activeStreams.add(info) }

            for (event in stream.eventChannel) {
                if (stream.sessionId != currentSessionId) break

                when (event) {
                    is AudioStreamEvent.Boundaries -> {
                        synchronized(activeStreams) {
                            info.boundaries = info.boundaries + event.boundaries
                        }
                    }
                    is AudioStreamEvent.Chunk -> {
                        val samples = event.samples
                        var offset = 0
                        while (offset < samples.size && scope.isActive && stream.sessionId == currentSessionId) {
                            val currentTrack = synchronized(this@SherpaTtsEngine) { audioTrack } ?: break
                            if (currentTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                try {
                                    currentTrack.play()
                                } catch (_: Exception) {
                                    break
                                }
                            }

                            val toWrite = samples.size - offset
                            val written = try {
                                currentTrack.write(samples, offset, toWrite, AudioTrack.WRITE_NON_BLOCKING)
                            } catch (_: Exception) {
                                -1
                            }
                            if (written < 0) {
                                Log.e("SherpaTtsEngine", "AudioTrack write error: $written")
                                break
                            }
                            if (written == 0) {
                                delay(10.milliseconds) // Wait for buffer to clear
                                continue
                            }

                            if (info.startFrame == -1L) {
                                // Record where this stream starts in the track's frame sequence,
                                // not where playback is right now: non-blocking writes queue up
                                // to a buffer ahead of the playback head.
                                synchronized(activeStreams) {
                                    info.startFrame = framesWrittenToTrack and FRAME_COUNTER_MASK
                                }
                            }

                            framesWrittenToTrack += written
                            offset += written
                        }
                        synchronized(activeStreams) { info.totalFrames += samples.size }
                    }
                    is AudioStreamEvent.Finished -> {
                        synchronized(activeStreams) { info.isWriteFinished = true }
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
        if (tts == null) {
            _state.value = TtsState.Error("No Sherpa-ONNX model selected", TtsErrorReason.NO_MODEL_SELECTED)
            return
        }
        stop()
        enqueue(text, utteranceId)
    }

    override fun enqueue(text: String, utteranceId: String) {
        if (tts == null) {
            _state.value = TtsState.Error("No Sherpa-ONNX model selected", TtsErrorReason.NO_MODEL_SELECTED)
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
            synchronized(activeStreams) { activeStreams.clear() }

            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        try {
                            track.pause()
                            track.flush()
                            track.stop()
                        } catch (_: Exception) {
                            // Ignore errors during stop/flush as track might already be in an invalid state
                        }
                        track.release()
                    }
                }
                audioTrack = null
                currentSampleRate = -1
                framesWrittenToTrack = 0
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

    // NOTE: As of Sherpa-ONNX v2.0.2, native word-level timestamps are supported in the C++ core
    // but not exposed in the Kotlin/JVM bindings (GeneratedAudio only contains samples/sampleRate).
    // Using this heuristic until the bindings expose timestamps.
    override fun release() {
        synchronized(this) {
            synthesisJob?.cancel()
            playbackJob?.cancel()
            monitorJob?.cancel()
            tts?.release()
            audioTrack?.release()
            tts = null
            lexiconIndex = null
            audioTrack = null

            // Reset initialization guards and queues so the engine can be re-initialized if reused.
            currentModelId = null
            currentModelType = null
            lastNoiseScale = -1f
            lastLengthScale = -1f
            currentSampleRate = -1
            currentSessionId++

            while (utteranceQueue.tryReceive().isSuccess) { /* consume */ }
            var result = synthesizedQueue.tryReceive()
            while (result.isSuccess) {
                result.getOrNull()?.eventChannel?.close()
                result = synthesizedQueue.tryReceive()
            }
            synchronized(activeStreams) { activeStreams.clear() }

            _state.value = TtsState.Idle
        }
    }

    companion object {
        private const val FRAME_COUNTER_MASK = 0xFFFFFFFFL
        private const val FRAME_COUNTER_WRAP = 0x1_0000_0000L

        /** Punctuation that ends a spoken sentence, matching Sherpa's own sentence frontends. */
        private const val SENTENCE_TERMINATORS = ".!?;:。！？；：…\n"

        /** Characters that belong to the sentence before them, e.g. a closing quote. */
        private const val SENTENCE_CLOSERS = "\"')]}»”’"

        /**
         * Sentences shorter than this are grouped with their neighbours. Models with a character
         * frontend (Kokoro, Matcha, Kitten, ZipVoice) do the same internally; grouping them here
         * avoids handing an abbreviation such as "Dr." to the engine as a standalone sentence.
         */
        private const val MIN_WORDS_PER_SEGMENT = 5

        /**
         * Distance from [startFrame] to [head] in the AudioTrack's 32-bit frame counter, handling
         * the counter wrapping back to zero after ~27 hours of audio. Exposed for tests.
         */
        internal fun framesPlayedSince(head: Long, startFrame: Long): Long = (head - (startFrame and FRAME_COUNTER_MASK) + FRAME_COUNTER_WRAP) and FRAME_COUNTER_MASK

        /**
         * Splits speech-ready text into sentence-sized segments that can be synthesized one at a
         * time. Whitespace around each sentence is trimmed; the returned ranges index [text].
         * Short sentences are grouped so that segments are at least [MIN_WORDS_PER_SEGMENT] words
         * where the text allows it.
         */
        internal fun splitIntoSentences(
            text: String,
            minWords: Int = MIN_WORDS_PER_SEGMENT,
        ): List<IntRange> {
            if (text.isEmpty()) return emptyList()

            val sentences = mutableListOf<IntRange>()
            var start = 0
            var index = 0
            while (index < text.length) {
                val c = text[index]
                // A decimal point is not a sentence end ("3.14" survives normalization edge cases).
                val isDecimalPoint =
                    c == '.' &&
                        index > 0 &&
                        index + 1 < text.length &&
                        text[index - 1].isDigit() &&
                        text[index + 1].isDigit()
                if (c in SENTENCE_TERMINATORS && !isDecimalPoint) {
                    var end = index + 1
                    while (end < text.length && text[end] in SENTENCE_TERMINATORS) end++
                    while (end < text.length && text[end] in SENTENCE_CLOSERS) end++
                    addSentence(text, start, end, sentences)
                    start = end
                    index = end
                } else {
                    index++
                }
            }
            if (start < text.length) addSentence(text, start, text.length, sentences)

            return mergeShortSentences(text, sentences, minWords)
        }

        private fun addSentence(text: String, from: Int, to: Int, sentences: MutableList<IntRange>) {
            var start = from
            var end = to
            while (start < end && text[start].isWhitespace()) start++
            while (end > start && text[end - 1].isWhitespace()) end--
            if (start < end) sentences.add(start until end)
        }

        private fun mergeShortSentences(
            text: String,
            sentences: List<IntRange>,
            minWords: Int,
        ): List<IntRange> {
            if (sentences.size < 2) return sentences

            val merged = mutableListOf<IntRange>()
            var groupStart = -1
            var groupEnd = -1
            var groupWords = 0
            for (sentence in sentences) {
                if (groupStart == -1) groupStart = sentence.first
                groupEnd = sentence.last
                groupWords += wordCount(text, sentence)
                if (groupWords >= minWords) {
                    merged.add(groupStart..groupEnd)
                    groupStart = -1
                    groupWords = 0
                }
            }
            if (groupStart != -1) {
                if (merged.isEmpty()) {
                    merged.add(groupStart..groupEnd)
                } else {
                    merged[merged.lastIndex] = merged.last().first..groupEnd
                }
            }
            return merged
        }

        private fun wordCount(text: String, range: IntRange): Int {
            var count = 0
            var inWord = false
            for (index in range) {
                val isWhitespace = text[index].isWhitespace()
                if (!isWhitespace && !inWord) count++
                inWord = !isWhitespace
            }
            return count
        }

        private fun concatenate(chunks: List<FloatArray>, totalFrames: Int): FloatArray {
            if (chunks.isEmpty()) return FloatArray(0)
            if (chunks.size == 1) return chunks[0]
            val samples = FloatArray(totalFrames)
            var offset = 0
            for (chunk in chunks) {
                System.arraycopy(chunk, 0, samples, offset, chunk.size)
                offset += chunk.size
            }
            return samples
        }

        /**
         * Rewrites boundary offsets from the synthesized text back to the displayed text.
         * Boundaries whose range cannot be mapped are dropped rather than reported out of bounds.
         */
        internal fun mapToSource(
            speech: NormalizedSpeech,
            boundaries: List<WordBoundary>,
        ): List<WordBoundary> = boundaries.mapNotNull { boundary ->
            val range = speech.sourceRange(boundary.startChar, boundary.endChar) ?: return@mapNotNull null
            WordBoundary(range.first, range.last + 1, boundary.startSample, boundary.endSample)
        }

        /**
         * Model types whose text frontend is a lexicon in Sherpa-ONNX 2.x. Pocket and Supertonic
         * ship their own character frontends and expose no lexicon config. Exposed for tests.
         */
        internal fun requiresLexicon(type: TtsModelType): Boolean = when (type) {
            TtsModelType.VITS,
            TtsModelType.MATCHA,
            TtsModelType.KOKORO,
            TtsModelType.ZIPVOICE,
            TtsModelType.KITTEN,
            -> true
            TtsModelType.POCKET,
            TtsModelType.SUPERTONIC,
            -> false
        }

        /**
         * Estimates per-word sample boundaries using a character-weight heuristic. Exposed for tests.
         */
        internal fun estimateWordBoundaries(
            text: String,
            totalSamples: Int,
            samples: FloatArray?,
        ): List<WordBoundary> {
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

            // 2. Calculate weighted length of the text. Prefix sums keep this linear in the text
            // length even for long sentences.
            val weights = FloatArray(text.length + 1)
            for (index in text.indices) {
                weights[index + 1] = weights[index] + getCharWeight(text[index])
            }
            val totalWeight = weights[text.length].coerceAtLeast(1.0f)

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

                // Weight-based heuristic: use the weight of characters before and within the word
                val weightBefore = weights[startChar]
                val weightInWord = weights[endChar] - weightBefore

                val wordStartSample = speechStart + (weightBefore / totalWeight * speechSamples).toInt()
                val wordEndSample = speechStart + ((weightBefore + weightInWord) / totalWeight * speechSamples).toInt()

                boundaries.add(WordBoundary(startChar, endChar, wordStartSample, wordEndSample))
            }

            return boundaries
        }

        internal fun detectSpeechBounds(samples: FloatArray): Pair<Int, Int> {
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

        internal fun getCharWeight(c: Char): Float = when (c) {
            '.', '!', '?' -> 3.0f // Significant pause
            ',', ';', ':', '-' -> 2.0f // Medium pause
            ' ' -> 1.2f // Gap between words
            '(', ')', '[', ']', '{', '}', '"', '\'' -> 0.1f // Usually quick
            else -> 1.0f // Standard character duration
        }
    }
}
