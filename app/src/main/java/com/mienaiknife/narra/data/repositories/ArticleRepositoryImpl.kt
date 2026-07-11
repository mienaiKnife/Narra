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
import com.mienaiknife.narra.data.local.entities.toDomainModel
import com.mienaiknife.narra.data.remote.WebDataSource
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.repository.ArticleRepository
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val webDataSource: WebDataSource,
    private val imageDataSource: ImageDataSource,
    private val networkMonitor: NetworkMonitor,
    private val downloadSettingsManager: DownloadSettingsManager,
) : ArticleRepository {

    override fun getAllArticles(): Flow<List<Article>> = articleDao.getAllArticles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getArticlesBySource(source: String): Flow<List<Article>> = articleDao.getArticlesBySource(source).map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getArticlesByFeedUrl(feedUrl: String): Flow<List<Article>> = articleDao.getArticlesByFeedUrl(feedUrl).map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun searchArticles(query: String): Flow<List<Article>> = articleDao.searchArticles(query).map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getQueueArticles(): Flow<List<Article>> = articleDao.getQueueArticles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getHistoryArticles(): Flow<List<Article>> = articleDao.getHistoryArticles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getInboxArticles(): Flow<List<Article>> = articleDao.getInboxArticles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getFavoriteArticles(): Flow<List<Article>> = articleDao.getFavoriteArticles().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override suspend fun getArticleById(id: String): Article? = articleDao.getArticleWithFeedById(id)?.toDomainModel()

    override suspend fun toggleFavorite(id: String) {
        articleDao.toggleFavorite(id)
    }

    override suspend fun removeFromQueue(id: String) {
        articleDao.removeFromQueue(id)
    }

    override suspend fun addToQueue(id: String): Result<Unit> {
        val article = articleDao.getArticleById(id) ?: return Result.failure(NarraError.Content.NotFound())

        if (article.content.isNullOrEmpty()) {
            val url = article.url ?: return Result.failure(NarraError.Content.EmptyContent())
            return downloadWebPage(url).map { }
        }

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

    override suspend fun deleteArticle(id: String) {
        articleDao.deleteArticleById(id)
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

    override suspend fun markAllAsPlayedInFeed(feedUrl: String) {
        articleDao.markAllAsPlayedInFeed(feedUrl)
    }

    override suspend fun markAllAsUnplayedInFeed(feedUrl: String) {
        articleDao.markAllAsUnplayedInFeed(feedUrl)
    }

    override suspend fun updateArticleProgress(
        id: String,
        progress: Float,
        paragraphIndex: Int,
        wordOffset: Int,
        duration: Long?,
    ) {
        if (progress >= 1f) {
            articleDao.markAsFinished(id)
        } else {
            articleDao.getArticleById(id)?.let { article ->
                articleDao.insertArticle(
                    article.copy(
                        progress = progress,
                        currentParagraphIndex = paragraphIndex,
                        currentWordOffset = wordOffset,
                        duration = duration ?: article.duration,
                        lastPlayedAt = System.currentTimeMillis(),
                    ),
                )
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

            val localImageUrl = (remoteArticle.imageUrl ?: existingArticle?.imageUrl)?.let { imageUrl ->
                val fileName = "web_${remoteArticle.id.hashCode()}_${System.currentTimeMillis()}.png"
                imageDataSource.downloadAndSaveImage(imageUrl, fileName)
            }

            val articleEntity = ArticleEntity(
                id = existingArticle?.id ?: remoteArticle.id,
                title = remoteArticle.title.takeIf { it != "Untitled" } ?: existingArticle?.title ?: "Untitled",
                source = existingArticle?.source?.takeIf { existingArticle.isFromFeed || !UrlUtils.isUrlOrDomainLike(it) }
                    ?: remoteArticle.source,
                content = remoteArticle.content,
                excerpt = remoteArticle.publishedAt ?: existingArticle?.publishedAt,
                imageUrl = remoteArticle.imageUrl ?: existingArticle?.imageUrl,
                localImageUrl = localImageUrl ?: existingArticle?.localImageUrl,
                url = url,
                feedUrl = remoteArticle.feedUrl ?: existingArticle?.feedUrl,
                progress = if ((existingArticle?.progress ?: 0f) >= 1f) 0f else (existingArticle?.progress ?: 0f),
                currentParagraphIndex = if ((existingArticle?.progress ?: 0f) >= 1f) 0 else existingArticle?.currentParagraphIndex ?: 0,
                currentWordOffset = if ((existingArticle?.progress ?: 0f) >= 1f) 0 else existingArticle?.currentWordOffset ?: 0,
                publishedAt = remoteArticle.publishedAt ?: existingArticle?.publishedAt,
                publishedTimestamp = remoteArticle.publishedTimestamp ?: existingArticle?.publishedTimestamp,
                duration = DateUtils.estimateReadingTimeMs(remoteArticle.content),
                isInQueue = true,
                queueOrder = nextOrder,
                createdAt = existingArticle?.createdAt ?: System.currentTimeMillis(),
                isFavorite = existingArticle?.isFavorite ?: false,
                isFromFeed = existingArticle?.isFromFeed ?: false,
            )

            articleDao.insertArticle(articleEntity)
            articleEntity.toDomainModel()
        }
    }

    override suspend fun pruneOldArticleContent(maxAgeDays: Int) = withContext(Dispatchers.IO) {
        val minTimestamp = System.currentTimeMillis() - (maxAgeDays.toLong() * 24 * 60 * 60 * 1000)
        articleDao.pruneOldArticleContent(minTimestamp)
    }
}
