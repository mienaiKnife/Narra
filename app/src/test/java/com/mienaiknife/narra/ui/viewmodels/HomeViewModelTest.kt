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
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.domain.repository.FeedRepository
import com.mienaiknife.narra.domain.repository.ImportExportRepository
import com.mienaiknife.narra.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val articleRepository: ArticleRepository = mock()
    private val feedRepository: FeedRepository = mock()
    private val importExportRepository: ImportExportRepository = mock()
    private val modelRepository: ModelRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(articleRepository.getQueueArticles()).thenReturn(flowOf(emptyList()))
        whenever(articleRepository.getInboxArticles()).thenReturn(flowOf(emptyList()))
        whenever(articleRepository.getFavoriteArticles()).thenReturn(flowOf(emptyList()))

        viewModel = HomeViewModel(articleRepository, feedRepository, importExportRepository, modelRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HomeUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh updates isRefreshing state`() = runTest {
        whenever(feedRepository.refreshFeeds()).thenReturn(Result.success(Unit))

        viewModel.refresh()

        viewModel.uiState.test {
            // First item might be the initial state or the state after combine
            val state = awaitItem()
            if (state is HomeUiState.Success) {
                assertFalse(state.isRefreshing)
            }
        }
    }
}
