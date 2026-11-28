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
import com.mienaiknife.narra.domain.models.SherpaVoiceOverrides
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    private val kokoroModel = TtsModel(
        id = "kokoro-en-v0_19",
        name = "Kokoro",
        language = "en",
        voice = VoiceProfile(
            quality = VoiceQuality.HIGH,
            gender = VoiceGender.MIXED,
            locale = "en-US",
            speakerCount = 11,
        ),
        type = TtsModelType.KOKORO,
        modelUrl = "",
        isDownloaded = true,
    )

    private lateinit var viewModel: VoicesSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(modelRepository.getAvailableModels()).thenReturn(flowOf(listOf(kokoroModel)))
        whenever(settingsManager.ttsEngine).thenReturn(flowOf("ondevice"))
        whenever(settingsManager.ttsModelId).thenReturn(flowOf(kokoroModel.id))
        whenever(settingsManager.sherpaVoiceOverrides).thenReturn(flowOf(emptyMap()))
        whenever(ttsEngine.state).thenReturn(MutableStateFlow(TtsState.Idle))
        viewModel = VoicesSettingsViewModel(modelRepository, settingsManager, ttsEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `engine and model setters delegate to the settings manager`() = runTest {
        viewModel.setEngine("ondevice")
        viewModel.selectModel("kokoro-en-v0_19")
        advanceUntilIdle()

        verify(settingsManager).setTtsEngine("ondevice")
        verify(settingsManager).setTtsModelId("kokoro-en-v0_19")
    }

    @Test
    fun `voice parameter setters delegate to the selected model`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setSpeakerId(3)
        viewModel.setVoiceNoiseScale(0.4f)
        viewModel.setVoiceNoiseScaleW(0.9f)
        viewModel.setVoiceLengthScale(1.2f)
        viewModel.setVoicePauseScale(0.5f)
        viewModel.setVoiceExtraPause(250)
        viewModel.setVoiceCpuThreads(2)
        viewModel.resetVoiceParams()
        advanceUntilIdle()

        verify(settingsManager).setVoiceSpeaker(kokoroModel.id, 3)
        verify(settingsManager).setVoiceNoiseScale(kokoroModel.id, 0.4f)
        verify(settingsManager).setVoiceNoiseScaleW(kokoroModel.id, 0.9f)
        verify(settingsManager).setVoiceLengthScale(kokoroModel.id, 1.2f)
        verify(settingsManager).setVoicePauseScale(kokoroModel.id, 0.5f)
        verify(settingsManager).setVoiceExtraPause(kokoroModel.id, 250)
        verify(settingsManager).setVoiceCpuThreads(kokoroModel.id, 2)
        verify(settingsManager).resetVoiceParams(kokoroModel.id)
    }

    @Test
    fun `ui state resolves per-model overrides over the model defaults`() = runTest {
        whenever(settingsManager.sherpaVoiceOverrides).thenReturn(
            flowOf(mapOf(kokoroModel.id to SherpaVoiceOverrides(lengthScale = 1.8f))),
        )
        viewModel = VoicesSettingsViewModel(modelRepository, settingsManager, ttsEngine)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(kokoroModel.type, state.selectedModelType)
        assertEquals(kokoroModel.id, state.selectedModel?.id)
        assertEquals(1.8f, state.voiceParams.lengthScale, 0.001f)
        // Kokoro ignores the noise knob, so it must be hidden.
        assertEquals(false, state.voiceCapabilities.noiseScale)
        assertEquals(false, state.voiceCapabilities.noiseScaleW)
        assertEquals(true, state.voiceCapabilities.lengthScale)
    }

    @Test
    fun `deleting the selected model clears its tuning`() = runTest {
        viewModel.deleteModel(kokoroModel.id)
        advanceUntilIdle()

        verify(settingsManager).setTtsModelId(null)
        verify(modelRepository).deleteModel(kokoroModel.id)
        verify(settingsManager).resetVoiceParams(kokoroModel.id)
    }

    @Test
    fun `download and cancel delegate to the model repository`() {
        viewModel.downloadModel("vits-piper-en_US-amy-low")
        viewModel.cancelDownload("vits-piper-en_US-amy-low")

        verify(modelRepository).enqueueDownload("vits-piper-en_US-amy-low")
        verify(modelRepository).cancelDownload("vits-piper-en_US-amy-low")
    }
}
