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
import androidx.room.withTransaction
import com.mienaiknife.narra.data.local.AppDatabase
import com.mienaiknife.narra.data.local.EpubDataSource
import com.mienaiknife.narra.data.local.ImageDataSource
import com.mienaiknife.narra.data.local.OpmlDataSource
import com.mienaiknife.narra.data.local.backup.BackupPayload
import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.domain.repository.ImportExportRepository
import com.mienaiknife.narra.utils.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class ImportExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val epubDataSource: EpubDataSource,
    private val opmlDataSource: OpmlDataSource,
    private val imageDataSource: ImageDataSource,
) : ImportExportRepository {

    override suspend fun importEpub(inputStream: InputStream, title: String): Result<Unit> = withContext(Dispatchers.IO) {
        epubDataSource.parseEpub(context, inputStream, title).map { articles ->
            val nextOrderBase = articleDao.getNextQueueOrder()
            articles.forEachIndexed { index, article ->
                val articleEntity = ArticleEntity(
                    id = article.id,
                    title = article.title,
                    source = article.source,
                    content = article.content,
                    excerpt = article.publishedAt,
                    imageUrl = article.imageUrl,
                    url = article.url,
                    feedUrl = null,
                    duration = DateUtils.estimateReadingTimeMs(article.content),
                    isInQueue = true,
                    queueOrder = nextOrderBase + index,
                    createdAt = System.currentTimeMillis(),
                )
                articleDao.insertArticle(articleEntity)
            }
        }
    }

    override suspend fun importOpml(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        opmlDataSource.parseOpml(inputStream).mapCatching { feeds ->
            var count = 0
            feeds.forEach { feed ->
                if (feedDao.getFeedByUrl(feed.url) == null) {
                    feedDao.insertFeed(feed)
                    count++
                }
            }
            // Note: ContentRepositoryImpl will handle refreshFeeds() if count > 0
            count
        }
    }

    override suspend fun exportOpml(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val feeds = feedDao.getAllFeeds().first()
            opmlDataSource.generateOpml(outputStream, feeds)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun backupDatabase(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload =
                BackupPayload(
                    exportedAt = System.currentTimeMillis(),
                    feeds = feedDao.getAllFeeds().first(),
                    articles = articleDao.getAllArticleEntities(),
                )
            outputStream.bufferedWriter().use { it.write(BackupPayload.encode(payload)) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun restoreDatabase(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = BackupPayload.decode(inputStream.bufferedReader().use { it.readText() })
            appDatabase.withTransaction {
                articleDao.deleteAllArticles()
                feedDao.deleteAllFeeds()
                payload.feeds.forEach { feedDao.insertFeed(it) }
                payload.articles.forEach { articleDao.insertArticle(it) }
            }
            imageDataSource.pruneUnreferenced(payload.articles.mapNotNull { it.localImageUrl }.toSet())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun deleteAllMetadata() {
        articleDao.deleteAllArticles()
        imageDataSource.pruneUnreferenced(emptySet())
    }
}
