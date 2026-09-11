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

import androidx.lifecycle.SavedStateHandle
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.playback.PlaybackManager
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val repository: ArticleRepository = mock()
    private val playbackManager: PlaybackManager = mock()
    private val playbackSettingsManager: PlaybackSettingsManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(playbackManager.currentArticle).thenReturn(MutableStateFlow<com.mienaiknife.narra.domain.models.Article?>(null))
        whenever(playbackManager.isPlaying).thenReturn(MutableStateFlow(false))
        whenever(playbackManager.isBuffering).thenReturn(MutableStateFlow(false))
        whenever(playbackManager.currentPosition).thenReturn(MutableStateFlow(0L))
        whenever(playbackManager.duration).thenReturn(MutableStateFlow(0L))
        whenever(playbackManager.playbackSpeed).thenReturn(MutableStateFlow(1f))
        whenever(playbackManager.currentParagraphIndex).thenReturn(MutableStateFlow(0))
        whenever(playbackManager.currentWordRange).thenReturn(MutableStateFlow<IntRange?>(null))
        whenever(playbackManager.sleepTimerMillisLeft).thenReturn(MutableStateFlow<Long?>(null))
        whenever(playbackManager.settingsManager).thenReturn(playbackSettingsManager)
        whenever(playbackSettingsManager.fastForwardSkipTime).thenReturn(flowOf("15s"))
        whenever(playbackSettingsManager.rewindSkipTime).thenReturn(flowOf("15s"))

        val savedStateHandle = SavedStateHandle(mapOf("articleId" to "article-1"))
        viewModel = ReaderViewModel(repository, playbackManager, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playback controls delegate to the playback manager`() {
        viewModel.togglePlayPause()
        viewModel.skipForward()
        viewModel.skipBackward()
        viewModel.skipNext()
        viewModel.cycleSpeed()

        verify(playbackManager).togglePlayPause()
        verify(playbackManager).skipForward()
        verify(playbackManager).skipBackward()
        verify(playbackManager).skipNext()
        verify(playbackManager).cycleSpeed()
    }
}
