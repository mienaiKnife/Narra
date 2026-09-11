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

import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PlaybackSettingsViewModelTest {
    private val settingsManager: PlaybackSettingsManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PlaybackSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(settingsManager.fastForwardSkipTime).thenReturn(flowOf("15s"))
        whenever(settingsManager.rewindSkipTime).thenReturn(flowOf("15s"))
        whenever(settingsManager.fastForwardHardwareButton).thenReturn(flowOf("fast_forward"))
        whenever(settingsManager.rewindHardwareButton).thenReturn(flowOf("rewind"))
        whenever(settingsManager.pauseOnDisconnect).thenReturn(flowOf(true))
        whenever(settingsManager.pauseForInterruptions).thenReturn(flowOf(true))
        whenever(settingsManager.autoPlayNext).thenReturn(flowOf(true))
        whenever(settingsManager.playChimeAndTitle).thenReturn(flowOf(true))
        whenever(settingsManager.chimeSound).thenReturn(flowOf("music_box_chime_positive"))
        whenever(settingsManager.readAltText).thenReturn(flowOf(false))
        whenever(settingsManager.shortenHyperlinks).thenReturn(flowOf(false))
        viewModel = PlaybackSettingsViewModel(settingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setters delegate to the settings manager`() = runTest {
        viewModel.setFastForwardSkipTime("30s")
        viewModel.setRewindSkipTime("10s")
        viewModel.setPauseOnDisconnect(false)
        viewModel.setPauseForInterruptions(false)
        viewModel.setReadAltText(true)
        advanceUntilIdle()

        verify(settingsManager).setFastForwardSkipTime("30s")
        verify(settingsManager).setRewindSkipTime("10s")
        verify(settingsManager).setPauseOnDisconnect(false)
        verify(settingsManager).setPauseForInterruptions(false)
        verify(settingsManager).setReadAltText(true)
    }
}
