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
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.toDomainModel
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.remote.RemoteFeedDataSource
import com.mienaiknife.narra.data.remote.WebDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ContentRepositoryImpl(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val webDataSource: WebDataSource,
    private val remoteFeedDataSource: RemoteFeedDataSource,
    private val epubDataSource: EpubDataSource,
    private val imageDataSource: ImageDataSource,
    private val opmlDataSource: OpmlDataSource,
    private val networkMonitor: NetworkMonitor,
    private val downloadSettingsManager: DownloadSettingsManager,
    private val notificationHelper: com.mienaiknife.narra.utils.NotificationHelper
) : ContentRepository {

    override fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getArticlesBySource(source: String): Flow<List<Article>> {
        return articleDao.getArticlesBySource(source).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getArticlesByFeedUrl(feedUrl: String): Flow<List<Article>> {
        return articleDao.getArticlesByFeedUrl(feedUrl).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchArticles(query: String): Flow<List<Article>> {
        return articleDao.searchArticles(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getQueueArticles(): Flow<List<Article>> {
        return articleDao.getQueueArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getHistoryArticles(): Flow<List<Article>> {
        return articleDao.getHistoryArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getInboxArticles(): Flow<List<Article>> {
        return articleDao.getInboxArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFavoriteArticles(): Flow<List<Article>> {
        return articleDao.getFavoriteArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getArticleById(id: String): Article? {
        return articleDao.getArticleWithFeedById(id)?.toDomainModel()
    }

    override suspend fun toggleFavorite(id: String) {
        articleDao.toggleFavorite(id)
    }

    override suspend fun deleteAllMetadata() {
        articleDao.deleteAllArticles()
    }

    override suspend fun deleteAllFeeds() {
        feedDao.deleteAllFeeds()
        articleDao.deleteAllArticlesFromFeeds()
    }

    override suspend fun pruneOldArticleContent(maxAgeDays: Int) = withContext(Dispatchers.IO) {
        val minTimestamp = System.currentTimeMillis() - (maxAgeDays.toLong() * 24 * 60 * 60 * 1000)
        articleDao.pruneOldArticleContent(minTimestamp)
    }

    private suspend fun checkConnection(): Result<Unit> {
        if (!networkMonitor.isOnline()) {
            return Result.failure(NarraError.Network.NoConnection())
        }

        val wifiOnly = downloadSettingsManager.downloadOverWifiOnly.first()
        if (wifiOnly && !networkMonitor.isOnWifi()) {
            return Result.failure(NarraError.Network.WifiRequired())
        }

        return Result.success(Unit)
    }

    override suspend fun downloadWebPage(url: String): Result<Article> = withContext(Dispatchers.IO) {
        val connectionCheck = checkConnection()
        if (connectionCheck.isFailure) {
            return@withContext Result.failure(connectionCheck.exceptionOrNull()!!)
        }

        val existingArticle = articleDao.getArticleByUrl(url)
        if (existingArticle != null && existingArticle.isInQueue) {
            return@withContext Result.failure(NarraError.Content.ArticleAlreadyInQueue())
        }

        webDataSource.downloadArticle(url).mapCatching { remoteArticle ->
            val nextOrder = articleDao.getNextQueueOrder()
            
            val localImageUrl = remoteArticle.imageUrl?.let { imageUrl ->
                val fileName = "web_${remoteArticle.id.hashCode()}_${System.currentTimeMillis()}.png"
                imageDataSource.downloadAndSaveImage(imageUrl, fileName)
            }

            val articleEntity = ArticleEntity(
                id = existingArticle?.id ?: remoteArticle.id,
                title = remoteArticle.title,
                source = remoteArticle.source,
                content = remoteArticle.content,
                excerpt = remoteArticle.publishedAt, // Using publishedAt as excerpt if available from WebDataSource
                imageUrl = remoteArticle.imageUrl,
                localImageUrl = localImageUrl,
                url = url,
                feedUrl = remoteArticle.feedUrl,
                progress = 0f,
                currentParagraphIndex = 0,
                currentWordOffset = 0,
                publishedAt = remoteArticle.publishedAt,
                publishedTimestamp = remoteArticle.publishedTimestamp,
                duration = DateUtils.estimateReadingTimeMs(remoteArticle.content),
                isInQueue = true,
                queueOrder = nextOrder,
                createdAt = System.currentTimeMillis(),
                isFavorite = existingArticle?.isFavorite ?: false,
                isFromFeed = existingArticle?.isFromFeed ?: false
            )

            articleDao.insertArticle(articleEntity)
            articleEntity.toDomainModel()
        }
    }

    override suspend fun deleteArticle(id: String) {
        articleDao.deleteArticleById(id)
    }

    override suspend fun deleteFeed(url: String) = withContext(Dispatchers.IO) {
        val feed = feedDao.getFeedByUrl(url)
        if (feed != null) {
            articleDao.deleteArticlesBySourceFromInbox(feed.title)
            feedDao.deleteFeedByUrl(url)
        }
    }

    override suspend fun removeFromQueue(id: String) {
        articleDao.removeFromQueue(id)
    }

    override suspend fun addToQueue(id: String): Result<Unit> {
        val article = articleDao.getArticleById(id) ?: return Result.failure(NarraError.Content.NotFound())
        
        // If content is empty, trigger full download (which also handles image persistence)
        if (article.content.isNullOrEmpty()) {
            val url = article.url ?: return Result.failure(NarraError.Content.EmptyContent())
            return downloadWebPage(url).map { }
        }

        // Ensure persistent image is downloaded if it's missing but we have a URL
        if (article.localImageUrl == null && !article.imageUrl.isNullOrBlank()) {
            val fileName = "article_${article.id.hashCode()}_${System.currentTimeMillis()}.png"
            val localPath = imageDataSource.downloadAndSaveImage(article.imageUrl, fileName)
            if (localPath != null) {
                articleDao.insertArticle(article.copy(localImageUrl = localPath))
            }
        }

        articleDao.addToQueue(id)
        return Result.success(Unit)
    }

    override suspend fun clearHistory() {
        articleDao.clearHistory()
    }

    override suspend fun clearInbox() {
        articleDao.clearInbox()
    }

    override suspend fun clearQueue() {
        articleDao.clearQueue()
    }

    override suspend fun markAsFinished(id: String) {
        articleDao.markAsFinished(id)
    }

    override suspend fun markAsPlayed(id: String) {
        articleDao.markAsPlayed(id)
    }

    override suspend fun markAsUnplayed(id: String) {
        articleDao.markAsUnplayed(id)
    }

    override suspend fun updateArticleProgress(id: String, progress: Float, paragraphIndex: Int, wordOffset: Int, duration: Long?) {
        if (progress >= 1f) {
            articleDao.markAsFinished(id)
        } else {
            articleDao.getArticleById(id)?.let { article ->
                articleDao.insertArticle(article.copy(
                    progress = progress,
                    currentParagraphIndex = paragraphIndex,
                    currentWordOffset = wordOffset,
                    duration = duration ?: article.duration,
                    lastPlayedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    override suspend fun reorderQueue(fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val currentQueue = articleDao.getQueueArticles().map { entities ->
            entities.map { it.article }.sortedBy { it.queueOrder }
        }.first().toMutableList()

        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return@withContext

        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val updatedQueue = currentQueue.mapIndexed { index, article ->
            article.copy(queueOrder = index)
        }

        articleDao.updateArticles(updatedQueue)
    }

    override suspend fun updateQueueOrder(articleIds: List<String>) = withContext(Dispatchers.IO) {
        val currentQueue = articleDao.getQueueArticles().first()
        val updatedQueue = currentQueue.map { wrap ->
            val article = wrap.article
            val newOrder = articleIds.indexOf(article.id)
            if (newOrder != -1) {
                article.copy(queueOrder = newOrder)
            } else {
                article
            }
        }
        articleDao.updateArticles(updatedQueue)
    }

    override suspend fun subscribeToFeed(url: String): Result<String> = withContext(Dispatchers.IO) {
        val connectionCheck = checkConnection()
        if (connectionCheck.isFailure) {
            return@withContext Result.failure(connectionCheck.exceptionOrNull()!!)
        }
        
        remoteFeedDataSource.fetchFeedMetadata(url).mapCatching { feedEntity ->
            feedDao.insertFeed(feedEntity)
            refreshFeeds()
            feedEntity.title
        }
    }

    override suspend fun refreshFeeds(): Result<Unit> = withContext(Dispatchers.IO) {
        val connectionCheck = checkConnection()
        if (connectionCheck.isFailure) {
            return@withContext Result.failure(connectionCheck.exceptionOrNull()!!)
        }
        try {
            val feeds = feedDao.getAllFeeds().first()
            for (feed in feeds) {
                remoteFeedDataSource.fetchArticles(feed).onSuccess { result ->
                    val articles = result.articles
                    val updatedTitle = result.feedTitle
                    
                    if (updatedTitle != null && updatedTitle != feed.title) {
                        feedDao.insertFeed(feed.copy(title = updatedTitle))
                    }

                    val isFirstImport = articleDao.getArticleCountByFeedUrl(feed.url) == 0
                    val sortedArticles = articles.sortedByDescending { it.publishedTimestamp ?: 0L }

                    for ((index, article) in sortedArticles.withIndex()) {
                        val existingArticle = articleDao.getArticleByUrl(article.url ?: "")
                        
                        // If article exists but is missing local image, try to download it now
                        if (existingArticle != null && existingArticle.localImageUrl == null && article.imageUrl != null) {
                            val localImageUrl = article.imageUrl.let { imageUrl ->
                                val fileName = "feed_${article.id.hashCode()}_${System.currentTimeMillis()}.png"
                                imageDataSource.downloadAndSaveImage(imageUrl, fileName)
                            }
                            if (localImageUrl != null) {
                                articleDao.insertArticle(existingArticle.copy(localImageUrl = localImageUrl))
                            }
                        }

                        if (existingArticle == null) {
                            val isOldOnFirstImport = isFirstImport && index >= 5

                            val localImageUrl = article.imageUrl?.let { imageUrl ->
                                val fileName = "feed_${article.id.hashCode()}_${System.currentTimeMillis()}.png"
                                imageDataSource.downloadAndSaveImage(imageUrl, fileName)
                            }

                            val articleEntity = ArticleEntity(
                                id = article.id,
                                title = article.title,
                                source = updatedTitle ?: article.source,
                                content = null,
                                excerpt = article.publishedAt,
                                imageUrl = article.imageUrl,
                                localImageUrl = localImageUrl,
                                url = article.url,
                                feedUrl = feed.url,
                                publishedAt = article.publishedAt,
                                publishedTimestamp = article.publishedTimestamp,
                                isFromFeed = true,
                                isInQueue = false,
                                progress = if (isOldOnFirstImport) 1.0f else 0.0f,
                                finishedAt = if (isOldOnFirstImport) System.currentTimeMillis() else null,
                                lastPlayedAt = if (isOldOnFirstImport) System.currentTimeMillis() else null,
                                createdAt = System.currentTimeMillis()
                            )
                            articleDao.insertArticle(articleEntity)

                            if (feed.notificationsEnabled && articleEntity.progress < 1.0f) {
                                notificationHelper.showNewArticleNotification(feed, articleEntity)
                            }
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun updateFeed(feed: com.mienaiknife.narra.data.local.entities.FeedEntity) {
        feedDao.updateFeed(feed)
    }

    override suspend fun importEpub(inputStream: java.io.InputStream, title: String): Result<Unit> = withContext(Dispatchers.IO) {
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
                    createdAt = System.currentTimeMillis()
                )
                articleDao.insertArticle(articleEntity)
            }
        }
    }

    override suspend fun importOpml(inputStream: java.io.InputStream): Result<Int> = withContext(Dispatchers.IO) {
        opmlDataSource.parseOpml(inputStream).mapCatching { feeds ->
            var count = 0
            feeds.forEach { feed ->
                if (feedDao.getFeedByUrl(feed.url) == null) {
                    feedDao.insertFeed(feed)
                    count++
                }
            }
            if (count > 0) {
                refreshFeeds()
            }
            count
        }
    }

    override suspend fun exportOpml(outputStream: java.io.OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val feeds = feedDao.getAllFeeds().first()
            opmlDataSource.generateOpml(outputStream, feeds)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun backupDatabase(outputStream: java.io.OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            appDatabase.close()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                FileInputStream(dbFile).use { input ->
                    input.copyTo(outputStream)
                }
                Result.success(Unit)
            } else {
                Result.failure(NarraError.Storage.FileNotFound())
            }
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun restoreDatabase(inputStream: java.io.InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            appDatabase.close()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            
            // Delete sidecar files if they exist to prevent corruption with the new database file
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            FileOutputStream(dbFile).use { output ->
                inputStream.copyTo(output)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

}
