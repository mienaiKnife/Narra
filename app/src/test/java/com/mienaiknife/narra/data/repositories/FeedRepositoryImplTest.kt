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
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.remote.RemoteFeedDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.utils.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(downloadSettingsManager.downloadOverWifiOnly).thenReturn(flowOf(false))
    }

    @Test
    fun `refreshFeeds continues after a failing feed and reports failure`() = runTest {
        val okFeed = FeedEntity(url = "ok", title = "OK")
        val badFeed = FeedEntity(url = "bad", title = "Bad")
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(okFeed, badFeed)))
        whenever(remoteFeedDataSource.fetchArticles(okFeed)).thenReturn(
            Result.success(
                RemoteFeedDataSource.FetchArticlesResult(articles = listOf(article("a1", "u1")), feedTitle = "OK"),
            ),
        )
        whenever(remoteFeedDataSource.fetchArticles(badFeed)).thenReturn(Result.failure(NarraError.Content.InvalidFeed()))
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("ok")).thenReturn(1)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        val result = repository.refreshFeeds()

        assertTrue("One failing feed should surface as failure", result.isFailure)
        verify(articleDao).insertArticle(any())
    }

    @Test
    fun `refreshFeeds looks up existing articles in one batched query`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed")
        val articles = listOf(article("a1", "u1"), article("a2", "u2"), article("a3", "u3"))
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = articles, feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(0)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        repository.refreshFeeds()

        verify(articleDao).getArticlesByUrls(listOf("u1", "u2", "u3"))
        verify(articleDao, never()).getArticleByUrl(any())
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

    @Test
    fun `refreshFeeds notifies for a new article when notifications are enabled`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed", notificationsEnabled = true)
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = listOf(article("a1", "u1")), feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(1)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        repository.refreshFeeds()

        verify(notificationHelper, times(1)).showNewArticleNotification(eq(feed), any())
    }

    @Test
    fun `refreshFeeds does not notify when notifications are disabled`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed", notificationsEnabled = false)
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = listOf(article("a1", "u1")), feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(1)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        repository.refreshFeeds()

        verify(notificationHelper, never()).showNewArticleNotification(any(), any())
    }

    @Test
    fun `refreshFeeds does not notify for articles that already exist`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed", notificationsEnabled = true)
        val existing =
            ArticleEntity(
                id = "old",
                title = "Old",
                source = "test",
                content = null,
                url = "u1",
                feedUrl = "feed",
                isFromFeed = true,
            )
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = listOf(article("a1", "u1")), feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(1)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(listOf(existing))

        repository.refreshFeeds()

        verify(articleDao, never()).insertArticle(any())
        verify(notificationHelper, never()).showNewArticleNotification(any(), any())
    }

    @Test
    fun `first import does not notify for backfilled articles even when enabled`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed", notificationsEnabled = true)
        val articles = listOf(article("a1", "u1"), article("a2", "u2"), article("a3", "u3"))
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = articles, feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(0)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        repository.refreshFeeds()

        verify(articleDao, times(3)).insertArticle(any())
        verify(notificationHelper, never()).showNewArticleNotification(any(), any())
    }

    @Test
    fun `duplicate URLs in one fetch are inserted and notified only once`() = runTest {
        val feed = FeedEntity(url = "feed", title = "Feed", notificationsEnabled = true)
        val articles = listOf(article("a1", "u1"), article("a2", "u1"))
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(feed)))
        whenever(remoteFeedDataSource.fetchArticles(feed)).thenReturn(
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles = articles, feedTitle = "Feed")),
        )
        whenever(downloadSettingsManager.inboxInitialLimit).thenReturn(flowOf("5"))
        whenever(articleDao.getArticleCountByFeedUrl("feed")).thenReturn(1)
        whenever(articleDao.getArticlesByUrls(any())).thenReturn(emptyList())
        whenever(articleDao.insertArticle(any())).thenReturn(Unit)

        repository.refreshFeeds()

        verify(articleDao, times(1)).insertArticle(any())
        verify(notificationHelper, times(1)).showNewArticleNotification(eq(feed), any())
    }

    private fun article(
        id: String,
        url: String,
    ): Article = Article(
        id = id,
        title = id,
        source = "test",
        url = url,
    )
}
