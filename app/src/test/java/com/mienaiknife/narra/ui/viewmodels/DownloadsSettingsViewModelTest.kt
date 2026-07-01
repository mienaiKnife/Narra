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

import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsSettingsViewModelTest {
    private val downloadSettingsManager: DownloadSettingsManager = mock()
    private val syncSettingsManager: SyncSettingsManager = mock()
    private val contentRepository: ContentRepository = mock()
    private val playbackManager: PlaybackManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DownloadsSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(downloadSettingsManager.downloadOverWifiOnly).thenReturn(flowOf(false))
        whenever(downloadSettingsManager.refreshInterval).thenReturn(flowOf("Never"))
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(syncSettingsManager.autoExportEnabled).thenReturn(flowOf(false))
        whenever(syncSettingsManager.autoImportEnabled).thenReturn(flowOf(false))
        whenever(syncSettingsManager.autoExportUri).thenReturn(flowOf(null))
        whenever(syncSettingsManager.lastExportTimestamp).thenReturn(flowOf(0L))
        whenever(syncSettingsManager.pendingImport).thenReturn(flowOf(false))

        viewModel = DownloadsSettingsViewModel(
            downloadSettingsManager,
            syncSettingsManager,
            contentRepository,
            playbackManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteDatabase calls playbackManager stop`() = runTest {
        viewModel.deleteDatabase()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackManager).stop()
        verify(contentRepository).deleteAllMetadata()
        verify(contentRepository).deleteAllFeeds()
    }

    @Test
    fun `restoreDatabase calls playbackManager stop`() = runTest {
        val inputStream: InputStream = mock()
        whenever(contentRepository.restoreDatabase(inputStream)).thenReturn(Result.success(Unit))

        viewModel.restoreDatabase(inputStream)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackManager).stop()
        verify(contentRepository).restoreDatabase(inputStream)
    }

    @Test
    fun `deleteAllMetadata calls playbackManager stop`() = runTest {
        viewModel.deleteAllMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackManager).stop()
        verify(contentRepository).deleteAllMetadata()
    }

    @Test
    fun `deleteAllFeeds calls playbackManager stop`() = runTest {
        viewModel.deleteAllFeeds()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackManager).stop()
        verify(contentRepository).deleteAllFeeds()
    }
}
