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

import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SherpaTtsEngineErrorTest {
    private lateinit var modelRepository: ModelRepository
    private lateinit var settingsManager: PlaybackSettingsManager
    private lateinit var engine: SherpaTtsEngine

    @Before
    fun setup() {
        modelRepository = mock(ModelRepository::class.java)
        settingsManager = mock(PlaybackSettingsManager::class.java)

        // Mocking settings flows to avoid null pointer exceptions during init
        `when`(settingsManager.ttsModelId).thenReturn(MutableStateFlow(null))
        `when`(settingsManager.sherpaNoiseScale).thenReturn(MutableStateFlow(0.667f))
        `when`(settingsManager.sherpaLengthScale).thenReturn(MutableStateFlow(1.0f))
        `when`(settingsManager.ttsSpeakerId).thenReturn(MutableStateFlow(0))
        `when`(settingsManager.sherpaSpeed).thenReturn(MutableStateFlow(1.0f))

        engine = SherpaTtsEngine(modelRepository, settingsManager)
    }

    @Test
    fun testSpeakTransitionsToErrorStateWhenNoModelSelected() {
        // Force the state to Idle to test our change specifically.
        val stateField = SherpaTtsEngine::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (stateField.get(engine) as MutableStateFlow<TtsState>).value = TtsState.Idle

        engine.speak("Hello", "1")

        val currentState = engine.state.value
        assertTrue(currentState is TtsState.Error)
        assertEquals("No Sherpa-ONNX model selected", (currentState as TtsState.Error).message)
    }

    @Test
    fun testEnqueueTransitionsToErrorStateWhenNoModelSelected() {
        val stateField = SherpaTtsEngine::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (stateField.get(engine) as MutableStateFlow<TtsState>).value = TtsState.Idle

        engine.enqueue("Hello", "1")

        val currentState = engine.state.value
        assertTrue(currentState is TtsState.Error)
        assertEquals("No Sherpa-ONNX model selected", (currentState as TtsState.Error).message)
    }
}
