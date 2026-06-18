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

import app.cash.turbine.test
import com.mienaiknife.narra.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {
    private val playbackManager: PlaybackManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PlaybackViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(playbackManager.currentArticle).thenReturn(MutableStateFlow(null))
        whenever(playbackManager.isPlaying).thenReturn(MutableStateFlow(false))
        whenever(playbackManager.currentPosition).thenReturn(MutableStateFlow(0L))
        whenever(playbackManager.duration).thenReturn(MutableStateFlow(0L))

        viewModel = PlaybackViewModel(playbackManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.currentArticle)
            assertFalse(state.isPlaying)
            assertEquals(0L, state.currentPosition)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePlayPause calls manager`() {
        viewModel.togglePlayPause()
        verify(playbackManager).togglePlayPause()
    }
}
