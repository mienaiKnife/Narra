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

import com.mienaiknife.narra.data.local.ImageDataSource
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.remote.RemoteFeedDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.utils.NotificationHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FeedRepositoryImplTest {
    private val feedDao: FeedDao = mock()
    private val articleDao: ArticleDao = mock()
    private val remoteFeedDataSource: RemoteFeedDataSource = mock()
    private val imageDataSource: ImageDataSource = mock()
    private val networkMonitor: NetworkMonitor = mock()
    private val downloadSettingsManager: DownloadSettingsManager = mock()
    private val notificationHelper: NotificationHelper = mock()

    private lateinit var repository: FeedRepositoryImpl

    @Before
    fun setUp() {
        repository =
            FeedRepositoryImpl(
                feedDao,
                articleDao,
                remoteFeedDataSource,
                imageDataSource,
                networkMonitor,
                downloadSettingsManager,
                notificationHelper,
            )
    }

    @Test
    fun `deleteFeed removes inbox articles by feed URL instead of title`() = runTest {
        whenever(articleDao.deleteArticlesByFeedUrlFromInbox(any())).thenReturn(Unit)
        whenever(feedDao.deleteFeedByUrl(any())).thenReturn(Unit)

        repository.deleteFeed("https://example.com/feed.xml")

        verify(articleDao).deleteArticlesByFeedUrlFromInbox("https://example.com/feed.xml")
        verify(feedDao).deleteFeedByUrl("https://example.com/feed.xml")
        verify(feedDao, never()).getFeedByUrl(any())
    }
}
