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
import com.mienaiknife.narra.domain.repository.FeedRepository
import com.mienaiknife.narra.ui.utils.NetworkMonitor
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.utils.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val remoteFeedDataSource: RemoteFeedDataSource,
    private val imageDataSource: ImageDataSource,
    private val networkMonitor: NetworkMonitor,
    private val downloadSettingsManager: DownloadSettingsManager,
    private val notificationHelper: NotificationHelper,
) : FeedRepository {

    private companion object {
        const val MAX_CONCURRENT_IMAGE_DOWNLOADS = 4
    }

    override fun getAllFeeds(): Flow<List<FeedEntity>> = feedDao.getAllFeeds()

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

    override suspend fun deleteFeed(url: String) = withContext(Dispatchers.IO) {
        articleDao.deleteArticlesByFeedUrlFromInbox(url)
        feedDao.deleteFeedByUrl(url)
    }

    override suspend fun refreshFeeds(): Result<Unit> = withContext(Dispatchers.IO) {
        val connectionCheck = checkConnection()
        if (connectionCheck.isFailure) {
            return@withContext Result.failure(connectionCheck.exceptionOrNull()!!)
        }

        val feeds = feedDao.getAllFeeds().first()
        val failures = mutableListOf<Throwable>()

        for (feed in feeds) {
            remoteFeedDataSource.fetchArticles(feed).fold(
                onSuccess = { result ->
                    try {
                        processFeed(feed, result)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failures += e
                    }
                },
                onFailure = { failures += it },
            )
        }

        if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(NarraError.Unknown(failures.first()))
        }
    }

    private suspend fun processFeed(
        feed: FeedEntity,
        result: RemoteFeedDataSource.FetchArticlesResult,
    ) {
        val updatedTitle = result.feedTitle
        if (updatedTitle != null && updatedTitle != feed.title && !UrlUtils.isUrlOrDomainLike(updatedTitle)) {
            feedDao.insertFeed(feed.copy(title = updatedTitle))
        }

        val isFirstImport = articleDao.getArticleCountByFeedUrl(feed.url) == 0
        val inboxLimitStr = downloadSettingsManager.inboxInitialLimit.first()
        val inboxLimit = when (inboxLimitStr) {
            "All" -> Int.MAX_VALUE
            else -> inboxLimitStr.toIntOrNull() ?: 5
        }
        val sortedArticles = result.articles.sortedByDescending { it.publishedTimestamp ?: 0L }

        val urls = sortedArticles.mapNotNull { it.url }
        val existingByUrl =
            if (urls.isEmpty()) {
                emptyMap()
            } else {
                articleDao.getArticlesByUrls(urls).associateBy { it.url }
            }

        val localImages = downloadFeedImages(sortedArticles, existingByUrl)

        for ((index, article) in sortedArticles.withIndex()) {
            val existing = existingByUrl[article.url]
            val downloadedImage = localImages[article.id]
            when {
                existing == null -> {
                    val isOldOnFirstImport = isFirstImport && index >= inboxLimit
                    val articleEntity =
                        ArticleEntity(
                            id = article.id,
                            title = article.title,
                            source = updatedTitle ?: article.source,
                            content = null,
                            excerpt = article.publishedAt,
                            imageUrl = article.imageUrl,
                            localImageUrl = downloadedImage,
                            url = article.url,
                            feedUrl = feed.url,
                            publishedAt = article.publishedAt,
                            publishedTimestamp = article.publishedTimestamp,
                            isFromFeed = true,
                            isInInbox = !isOldOnFirstImport,
                            isInQueue = false,
                            progress = 0.0f,
                            finishedAt = null,
                            lastPlayedAt = null,
                            createdAt = System.currentTimeMillis(),
                        )
                    articleDao.insertArticle(articleEntity)

                    if (feed.notificationsEnabled && articleEntity.progress < 1.0f) {
                        notificationHelper.showNewArticleNotification(feed, articleEntity)
                    }
                }
                !existing.isFromFeed -> {
                    articleDao.insertArticle(
                        existing.copy(
                            isFromFeed = true,
                            feedUrl = feed.url,
                            source = updatedTitle ?: existing.source,
                            localImageUrl = existing.localImageUrl ?: downloadedImage,
                        ),
                    )
                }
                existing.localImageUrl == null && downloadedImage != null -> {
                    articleDao.updateLocalImageUrl(existing.id, downloadedImage)
                }
            }
        }
    }

    private suspend fun downloadFeedImages(
        articles: List<Article>,
        existingByUrl: Map<String?, ArticleEntity>,
    ): Map<String, String> {
        val requiringImages =
            articles.filter { article ->
                !article.imageUrl.isNullOrBlank() && existingByUrl[article.url]?.localImageUrl == null
            }
        if (requiringImages.isEmpty()) return emptyMap()

        return coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_IMAGE_DOWNLOADS)
            requiringImages
                .map { article ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val fileName = "feed_${article.id.hashCode()}_${System.currentTimeMillis()}"
                            article.id to imageDataSource.downloadAndSaveImage(article.imageUrl!!, fileName)
                        }
                    }
                }.awaitAll()
                .mapNotNull { (id, path) -> path?.let { id to it } }
                .toMap()
        }
    }

    override suspend fun updateFeed(feed: FeedEntity) {
        feedDao.updateFeed(feed)
    }

    override suspend fun deleteAllFeeds() {
        feedDao.deleteAllFeeds()
        articleDao.deleteAllArticlesFromFeeds()
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
}
