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
package com.mienaiknife.narra.tts.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTtsEngine
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) : TtsEngine {
    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentVolume = 1.0f
    private val pendingRequests = mutableListOf<PendingRequest>()

    private sealed class PendingRequest {
        data class Speak(
            val text: String,
            val utteranceId: String,
        ) : PendingRequest()

        data class Enqueue(
            val text: String,
            val utteranceId: String,
        ) : PendingRequest()
    }

    init {
        tts =
            TextToSpeech(context) { status ->
                synchronized(this) {
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.let { engine ->
                            val defaultLocale = Locale.getDefault()
                            val langResult = engine.setLanguage(defaultLocale)
                            android.util.Log.i("AndroidTtsEngine", "Engine initialized. Default locale: $defaultLocale, setLanguage result: $langResult")
                            
                            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                                android.util.Log.w("AndroidTtsEngine", "Default locale not supported, falling back to US English")
                                engine.language = Locale.US
                            }

                            // Log available voices for debugging
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                val voices = engine.voices
                                android.util.Log.i("AndroidTtsEngine", "Available voices count: ${voices?.size ?: 0}")
                                voices?.take(3)?.forEach { 
                                    android.util.Log.d("AndroidTtsEngine", "Voice: ${it.name}, Locale: ${it.locale}")
                                }
                            }

                            engine.setOnUtteranceProgressListener(
                                object : UtteranceProgressListener() {
                                    override fun onStart(utteranceId: String?) {
                                        utteranceId?.let { id ->
                                            _state.value = TtsState.Speaking(id)
                                        }
                                    }

                                    override fun onRangeStart(
                                        utteranceId: String?,
                                        start: Int,
                                        end: Int,
                                        frame: Int,
                                    ) {
                                        utteranceId?.let { id ->
                                            _state.value = TtsState.Speaking(id, start, end, frame)
                                        }
                                    }

                                    override fun onDone(utteranceId: String?) {
                                        utteranceId?.let { id ->
                                            _state.value = TtsState.Finished(id)
                                        }
                                        _state.value = TtsState.Ready
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun onError(utteranceId: String?) {
                                        _state.value = TtsState.Error("Error speaking utterance: $utteranceId")
                                    }

                                    override fun onError(
                                        utteranceId: String?,
                                        errorCode: Int,
                                    ) {
                                        _state.value = TtsState.Error("Error speaking utterance: $utteranceId, code: $errorCode")
                                    }
                                },
                            )
                            isInitialized = true
                            _state.value = TtsState.Ready
                            processPendingRequests()
                        }
                    } else {
                        _state.value = TtsState.Error("Failed to initialize Android TTS")
                        pendingRequests.clear()
                    }
                }
            }
    }

    private fun processPendingRequests() {
        val requests =
            synchronized(this) {
                val list = ArrayList(pendingRequests)
                pendingRequests.clear()
                list
            }
        requests.forEach { request ->
            when (request) {
                is PendingRequest.Speak -> speak(request.text, request.utteranceId)
                is PendingRequest.Enqueue -> enqueue(request.text, request.utteranceId)
            }
        }
    }

    @Synchronized
    override fun speak(
        text: String,
        utteranceId: String,
    ) {
        if (!isInitialized) {
            pendingRequests.clear()
            pendingRequests.add(PendingRequest.Speak(text, utteranceId))
            return
        }

        val params =
            android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
            }
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        
        if (result == TextToSpeech.ERROR) {
            _state.value = TtsState.Error("Immediate error calling speak() for $utteranceId")
        }
    }

    @Synchronized
    override fun enqueue(
        text: String,
        utteranceId: String,
    ) {
        if (!isInitialized) {
            pendingRequests.add(PendingRequest.Enqueue(text, utteranceId))
            return
        }
        val params =
            android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
            }
        val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _state.value = TtsState.Error("Immediate error calling enqueue() for $utteranceId")
        }
    }

    @Synchronized
    override fun stop() {
        pendingRequests.clear()
        tts?.stop()
        _state.value = TtsState.Ready
    }

    @Synchronized
    override fun setPlaybackSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    @Synchronized
    override fun setAudioAttributes(
        usage: Int,
        contentType: Int,
    ) {
        val attr =
            android.media.AudioAttributes
                .Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
        tts?.setAudioAttributes(attr)
    }

    @Synchronized
    override fun setVolume(volume: Float) {
        currentVolume = volume
        // Volume will be applied to the next speak/enqueue call.
        // Android TTS doesn't support changing volume of currently playing utterance easily.
    }

    @Synchronized
    override fun release() {
        pendingRequests.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _state.value = TtsState.Idle
    }
}
