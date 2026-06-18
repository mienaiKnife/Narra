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
package com.mienaiknife.narra.data.repositories

import android.content.Context
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.local.EpubDataSource
import com.mienaiknife.narra.data.local.ImageDataSource
import com.mienaiknife.narra.data.local.OpmlDataSource
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.remote.RemoteFeedDataSource
import com.mienaiknife.narra.data.remote.WebDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.utils.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ContentRepositoryImplTest {
    private val context: Context = mock()
    private val appDatabase: AppDatabase = mock()
    private val articleDao: ArticleDao = mock()
    private val feedDao: FeedDao = mock()
    private val webDataSource: WebDataSource = mock()
    private val remoteFeedDataSource: RemoteFeedDataSource = mock()
    private val epubDataSource: EpubDataSource = mock()
    private val imageDataSource: ImageDataSource = mock()
    private val opmlDataSource: OpmlDataSource = mock()
    private val networkMonitor: NetworkMonitor = mock()
    private val downloadSettingsManager: DownloadSettingsManager = mock()
    private val notificationHelper: NotificationHelper = mock()

    private lateinit var contentRepository: ContentRepositoryImpl

    @Before
    fun setUp() {
        whenever(downloadSettingsManager.downloadOverWifiOnly).thenReturn(flowOf(false))
        whenever(networkMonitor.isOnline()).thenReturn(true)

        contentRepository =
            ContentRepositoryImpl(
                context,
                appDatabase,
                articleDao,
                feedDao,
                webDataSource,
                remoteFeedDataSource,
                epubDataSource,
                imageDataSource,
                opmlDataSource,
                networkMonitor,
                downloadSettingsManager,
                notificationHelper,
            )
    }

    @Test
    fun `downloadWebPage returns failure when offline`() = runBlocking {
        whenever(networkMonitor.isOnline()).thenReturn(false)

        val result = contentRepository.downloadWebPage("https://example.com")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is com.mienaiknife.narra.domain.NarraError.Network.NoConnection)
    }

    @Test
    fun `importOpml returns success with count`() = runBlocking {
        whenever(opmlDataSource.parseOpml(any())).thenReturn(Result.success(emptyList()))

        val result = contentRepository.importOpml("".byteInputStream())

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == 0)
    }
}
