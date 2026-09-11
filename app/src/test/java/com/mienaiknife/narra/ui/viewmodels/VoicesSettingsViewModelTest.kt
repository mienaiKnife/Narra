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
package com.mienaiknife.narra.ui.viewmodels

import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VoicesSettingsViewModelTest {
    private val modelRepository: ModelRepository = mock()
    private val settingsManager: PlaybackSettingsManager = mock()
    private val ttsEngine: TtsEngine = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: VoicesSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(modelRepository.getAvailableModels()).thenReturn(flowOf(emptyList()))
        whenever(settingsManager.ttsEngine).thenReturn(flowOf("android"))
        whenever(settingsManager.ttsModelId).thenReturn(flowOf(null))
        whenever(settingsManager.ttsSpeakerId).thenReturn(flowOf(0))
        whenever(settingsManager.sherpaNoiseScale).thenReturn(flowOf(0.667f))
        whenever(settingsManager.sherpaLengthScale).thenReturn(flowOf(1f))
        whenever(ttsEngine.state).thenReturn(MutableStateFlow(TtsState.Idle))
        viewModel = VoicesSettingsViewModel(modelRepository, settingsManager, ttsEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `engine and model setters delegate to the settings manager`() = runTest {
        whenever(modelRepository.ensureDefaultModelsInitialized()).thenReturn(Unit)

        viewModel.setEngine("ondevice")
        viewModel.selectModel("kokoro-en-v0_19")
        viewModel.setSpeakerId(3)
        advanceUntilIdle()

        verify(settingsManager).setTtsEngine("ondevice")
        verify(settingsManager).setTtsModelId("kokoro-en-v0_19")
        verify(settingsManager).setTtsSpeakerId(3)
    }

    @Test
    fun `download and cancel delegate to the model repository`() {
        viewModel.downloadModel("vits-piper-en_US-amy-low")
        viewModel.cancelDownload("vits-piper-en_US-amy-low")

        verify(modelRepository).enqueueDownload("vits-piper-en_US-amy-low")
        verify(modelRepository).cancelDownload("vits-piper-en_US-amy-low")
    }
}
