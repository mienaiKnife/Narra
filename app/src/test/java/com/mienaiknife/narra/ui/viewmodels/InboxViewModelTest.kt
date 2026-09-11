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

import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.domain.repository.FeedRepository
import com.mienaiknife.narra.playback.PlaybackManager
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
class InboxViewModelTest {
    private val repository: ArticleRepository = mock()
    private val feedRepository: FeedRepository = mock()
    private val playbackManager: PlaybackManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: InboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getInboxArticles()).thenReturn(flowOf(emptyList()))
        whenever(playbackManager.currentArticle).thenReturn(MutableStateFlow<com.mienaiknife.narra.domain.models.Article?>(null))
        whenever(playbackManager.isPlaying).thenReturn(MutableStateFlow(false))
        whenever(playbackManager.playbackSpeed).thenReturn(MutableStateFlow(1f))
        viewModel = InboxViewModel(repository, feedRepository, playbackManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearInbox delegates to the repository`() = runTest {
        viewModel.clearInbox()
        advanceUntilIdle()

        verify(repository).clearInbox()
    }

    @Test
    fun `refresh delegates to the feed repository`() = runTest {
        whenever(feedRepository.refreshFeeds()).thenReturn(Result.success(Unit))
        viewModel.refresh()
        advanceUntilIdle()

        verify(feedRepository).refreshFeeds()
    }
}
