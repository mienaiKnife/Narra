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

package com.mienaiknife.narra.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for all TTS engine implementations.
 */
interface TtsEngine {
    /**
     * Current playback state of the engine.
     */
    val state: StateFlow<TtsState>

    /**
     * Synthesizes and plays the given text, clearing the queue.
     * @param text The text to speak.
     * @param utteranceId A unique identifier for this request.
     */
    fun speak(text: String, utteranceId: String)

    /**
     * Synthesizes and adds the given text to the playback queue.
     * @param text The text to speak.
     * @param utteranceId A unique identifier for this request.
     */
    fun enqueue(text: String, utteranceId: String)

    /**
     * Stops the current playback and clears the queue.
     */
    fun stop()

    /**
     * Sets the playback speed.
     * @param speed The playback speed (e.g., 1.0f for normal speed).
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Sets the audio attributes for playback.
     */
    fun setAudioAttributes(usage: Int, contentType: Int)

    /**
     * Sets the volume for playback.
     * @param volume The volume level from 0.0f to 1.0f.
     */
    fun setVolume(volume: Float)

    /**
     * Releases resources used by the engine.
     */
    fun release()
}

sealed class TtsState {
    object Idle : TtsState()
    object Initializing : TtsState()
    object Ready : TtsState()
    data class Speaking(
        val utteranceId: String,
        val start: Int = 0,
        val end: Int = 0,
        val frame: Int = 0
    ) : TtsState()
    data class Error(val message: String) : TtsState()
}
