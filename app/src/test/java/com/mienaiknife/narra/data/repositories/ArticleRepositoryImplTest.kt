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
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.ArticleWithFeed
import com.mienaiknife.narra.data.remote.WebDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArticleRepositoryImplTest {
    private val articleDao: ArticleDao = mock()
    private val webDataSource: WebDataSource = mock()
    private val imageDataSource: ImageDataSource = mock()
    private val networkMonitor: NetworkMonitor = mock()
    private val downloadSettingsManager: DownloadSettingsManager = mock()

    private lateinit var repository: ArticleRepositoryImpl

    @Before
    fun setUp() {
        repository =
            ArticleRepositoryImpl(
                articleDao,
                webDataSource,
                imageDataSource,
                networkMonitor,
                downloadSettingsManager,
            )
    }

    @Test
    fun `updateArticleProgress performs one targeted update without rewriting the row`() = runTest {
        whenever(articleDao.updateArticleProgress(any(), any(), any(), any(), anyOrNull(), any())).thenReturn(Unit)

        repository.updateArticleProgress(
            id = "a1",
            progress = 0.42f,
            paragraphIndex = 3,
            wordOffset = 7,
            duration = 1234L,
        )

        verify(articleDao).updateArticleProgress(
            id = eq("a1"),
            progress = eq(0.42f),
            paragraphIndex = eq(3),
            wordOffset = eq(7),
            duration = eq(1234L),
            timestamp = any(),
        )
        verify(articleDao, never()).insertArticle(any())
        verify(articleDao, never()).getArticleById(any())
    }

    @Test
    fun `updateArticleProgress marks finished at completion`() = runTest {
        whenever(articleDao.markAsFinished(any(), any())).thenReturn(Unit)

        repository.updateArticleProgress(
            id = "a1",
            progress = 1f,
            paragraphIndex = 0,
            wordOffset = 0,
            duration = null,
        )

        verify(articleDao).markAsFinished(eq("a1"), any())
        verify(articleDao, never()).updateArticleProgress(any(), any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun `markAsPlayed delegates to the single finished DAO query`() = runTest {
        whenever(articleDao.markAsFinished(any(), any())).thenReturn(Unit)

        repository.markAsPlayed("a1")

        verify(articleDao).markAsFinished(eq("a1"), any())
    }

    @Test
    fun `addToQueue stores the downloaded image with a targeted update`() = runTest {
        val entity = article(id = "a1", imageUrl = "https://example.com/a.png")
        whenever(articleDao.getArticleById("a1")).thenReturn(entity)
        whenever(imageDataSource.downloadAndSaveImage(any(), any())).thenReturn("/images/a.png")
        whenever(articleDao.updateLocalImageUrl(any(), any())).thenReturn(Unit)
        whenever(articleDao.addToQueue("a1")).thenReturn(Unit)

        val result = repository.addToQueue("a1")

        assertTrue(result.isSuccess)
        verify(articleDao).updateLocalImageUrl("a1", "/images/a.png")
        verify(articleDao, never()).insertArticle(any())
    }

    @Test
    fun `reorderQueue updates only rows whose order changed`() = runTest {
        whenever(articleDao.getQueueArticles()).thenReturn(flowOf(queueEntities()))
        whenever(articleDao.updateQueueOrders(any())).thenReturn(Unit)

        repository.reorderQueue(fromIndex = 0, toIndex = 1)

        verify(articleDao).updateQueueOrders(listOf("b" to 0, "a" to 1))
        verify(articleDao, never()).updateArticles(any())
    }

    @Test
    fun `updateQueueOrder maps ids to positions in one pass`() = runTest {
        whenever(articleDao.getQueueArticles()).thenReturn(flowOf(queueEntities()))
        whenever(articleDao.updateQueueOrders(any())).thenReturn(Unit)

        repository.updateQueueOrder(listOf("c", "a", "b"))

        verify(articleDao).updateQueueOrders(listOf("a" to 1, "b" to 2, "c" to 0))
        verify(articleDao, never()).updateArticles(any())
    }

    private fun queueEntities(): List<ArticleWithFeed> = listOf(
        ArticleWithFeed(article = article("a", queueOrder = 0), feed = null),
        ArticleWithFeed(article = article("b", queueOrder = 1), feed = null),
        ArticleWithFeed(article = article("c", queueOrder = 2), feed = null),
    )

    private fun article(
        id: String,
        queueOrder: Int = 0,
        imageUrl: String? = null,
    ): ArticleEntity = ArticleEntity(
        id = id,
        title = id,
        source = "test",
        content = "content",
        imageUrl = imageUrl,
        queueOrder = queueOrder,
    )
}
