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

import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import com.mienaiknife.narra.tts.android.AndroidTtsEngine
import com.mienaiknife.narra.tts.ondevice.SherpaTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingTtsEngineTest {
    private val testDispatcher = StandardTestDispatcher()
    private val engineType = MutableStateFlow("android")

    private lateinit var androidTtsEngine: AndroidTtsEngine
    private lateinit var sherpaTtsEngine: SherpaTtsEngine
    private lateinit var settingsManager: PlaybackSettingsManager
    private lateinit var modelRepository: ModelRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        androidTtsEngine = mock()
        sherpaTtsEngine = mock()
        settingsManager = mock()
        modelRepository = mock()
        whenever(settingsManager.ttsEngine).thenReturn(engineType)
        whenever(settingsManager.ttsModelId).thenReturn(MutableStateFlow(null))
        whenever(modelRepository.getAvailableModels()).thenReturn(flowOf(emptyList()))
        whenever(androidTtsEngine.state).thenReturn(MutableStateFlow(TtsState.Idle))
        whenever(sherpaTtsEngine.state).thenReturn(MutableStateFlow(TtsState.Idle))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `switching away from sherpa stops but never releases it`() = runTest(testDispatcher) {
        DelegatingTtsEngine(androidTtsEngine, sherpaTtsEngine, settingsManager, modelRepository)
        advanceUntilIdle()

        engineType.value = "ondevice"
        advanceUntilIdle()
        engineType.value = "android"
        advanceUntilIdle()

        verify(sherpaTtsEngine).stop()
        verify(sherpaTtsEngine, never()).release()
    }
}
