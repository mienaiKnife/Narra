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
import com.mienaiknife.narra.data.local.backup.BackupPayload
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream

class ImportExportRepositoryImplTest {
    private val articleDao: ArticleDao = mock()
    private val feedDao: FeedDao = mock()

    private val repository =
        ImportExportRepositoryImpl(
            context = mock<Context>(),
            appDatabase = mock<AppDatabase>(),
            articleDao = articleDao,
            feedDao = feedDao,
            epubDataSource = mock<EpubDataSource>(),
            opmlDataSource = mock<OpmlDataSource>(),
            imageDataSource = mock<ImageDataSource>(),
        )

    @Test
    fun `backupDatabase writes a portable payload that can be decoded`() = runTest {
        whenever(feedDao.getAllFeeds()).thenReturn(flowOf(listOf(FeedEntity(url = "feed", title = "Feed"))))
        whenever(articleDao.getAllArticleEntities()).thenReturn(
            listOf(ArticleEntity(id = "a1", title = "Article", source = "Feed", content = "body", progress = 0.25f)),
        )

        val output = ByteArrayOutputStream()
        val result = repository.backupDatabase(output)

        assertTrue(result.isSuccess)
        val payload = BackupPayload.decode(output.toString(Charsets.UTF_8))
        assertEquals(1, payload.feeds.size)
        assertEquals(1, payload.articles.size)
        assertEquals(0.25f, payload.articles.first().progress)
    }
}
