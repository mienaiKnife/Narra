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

import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.local.entities.toDomainModel
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.utils.DateUtils
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import java.util.UUID

class ContentRepositoryImpl(
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val rssParser: RssParser
) : ContentRepository {

    override fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
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

    override suspend fun getArticleById(id: String): Article? {
        return articleDao.getArticleById(id)?.toDomainModel()
    }

    override suspend fun downloadWebPage(url: String): Result<Article> = withContext(Dispatchers.IO) {
        try {
            val existingArticle = articleDao.getArticleByUrl(url)
            if (existingArticle != null) {
                if (existingArticle.isInQueue) {
                    return@withContext Result.failure(Exception("Article already in queue"))
                } else {
                    // Move from history to queue
                    val updatedArticle = existingArticle.copy(
                        isInQueue = true,
                        createdAt = System.currentTimeMillis() // Move to top of queue?
                    )
                    articleDao.insertArticle(updatedArticle)
                    return@withContext Result.success(updatedArticle.toDomainModel())
                }
            }

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val readability4J = Readability4J(url, doc.outerHtml())
            val article = readability4J.parse()

            if (article.content == null) {
                return@withContext Result.failure(Exception("Failed to extract content from $url"))
            }

            val publishedAt = doc.select("meta[property=article:published_time]").attr("content").ifEmpty { null }
                    ?: doc.select("meta[name=publish-date]").attr("content").ifEmpty { null }
                    ?: doc.select("meta[property=og:pubdate]").attr("content").ifEmpty { null }
                    ?: doc.select("meta[name=pubdate]").attr("content").ifEmpty { null }
                    ?: doc.select("meta[name=date]").attr("content").ifEmpty { null }
                    ?: doc.select("time[itemprop=datePublished]").attr("datetime").ifEmpty { null }
                    ?: doc.select("time").attr("datetime").ifEmpty { null }

            val articleEntity = ArticleEntity(
                id = UUID.randomUUID().toString(),
                title = article.title ?: doc.title() ?: "Untitled",
                source = doc.location().let { java.net.URL(it).host } ?: "Web",
                content = article.content ?: "",
                excerpt = article.excerpt,
                imageUrl = article.byline,
                url = url,
                publishedAt = publishedAt,
                publishedTimestamp = DateUtils.parseToTimestamp(publishedAt),
                createdAt = System.currentTimeMillis()
            )

            // Try to find a better image URL if possible
            val finalArticleEntity = if (articleEntity.imageUrl == null) {
                val ogImage = doc.select("meta[property=og:image]").attr("content")
                val twitterImage = doc.select("meta[name=twitter:image]").attr("content")
                articleEntity.copy(imageUrl = ogImage.ifEmpty { twitterImage }.ifEmpty { null })
            } else {
                articleEntity
            }

            articleDao.insertArticle(finalArticleEntity)
            Result.success(finalArticleEntity.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteArticle(id: String) {
        articleDao.deleteArticleById(id)
    }

    override suspend fun removeFromQueue(id: String) {
        articleDao.removeFromQueue(id)
    }

    override suspend fun addToQueue(id: String) {
        articleDao.addToQueue(id)
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

    override suspend fun updateArticleProgress(id: String, progress: Float, paragraphIndex: Int, wordOffset: Int) {
        if (progress >= 1f) {
            articleDao.markAsFinished(id)
        } else {
            articleDao.getArticleById(id)?.let { article ->
                articleDao.insertArticle(article.copy(
                    progress = progress,
                    currentParagraphIndex = paragraphIndex,
                    currentWordOffset = wordOffset
                ))
            }
        }
    }

    override suspend fun reorderQueue(fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val currentQueue = articleDao.getQueueArticles().map { entities ->
            entities.sortedBy { it.queueOrder }
        }.first().toMutableList()

        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return@withContext

        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val updatedQueue = currentQueue.mapIndexed { index, article ->
            article.copy(queueOrder = index)
        }

        articleDao.updateArticles(updatedQueue)
    }

    override suspend fun subscribeToFeed(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = url
            var channel = try {
                rssParser.getRssChannel(targetUrl)
            } catch (e: Exception) {
                null
            }

            if (channel == null) {
                // Try feed discovery
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                
                val feedLink = doc.select("link[rel=alternate][type=application/rss+xml]").attr("abs:href")
                    .ifEmpty { doc.select("link[rel=alternate][type=application/atom+xml]").attr("abs:href") }
                
                if (feedLink.isNotEmpty()) {
                    targetUrl = feedLink
                    channel = rssParser.getRssChannel(targetUrl)
                }
            }

            if (channel == null) {
                return@withContext Result.failure(Exception("Could not find a valid RSS feed at $url"))
            }

            var imageUrl = channel.image?.url
            val link = channel.link
            val title = channel.title ?: "Untitled Feed"

            if (imageUrl == null && link != null) {
                try {
                    val doc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get()
                    
                    imageUrl = doc.select("link[rel~=(?i)^(shortcut|apple-touch-)?icon]").attr("abs:href")
                    if (imageUrl.isEmpty()) {
                        imageUrl = doc.select("meta[property=og:image]").attr("abs:href")
                    }
                    if (imageUrl.isEmpty()) {
                        imageUrl = null
                    }
                } catch (e: Exception) {
                    // Ignore favicon fetching errors
                }
            }

            val feedEntity = FeedEntity(
                url = targetUrl,
                title = title,
                description = channel.description,
                imageUrl = imageUrl
            )
            feedDao.insertFeed(feedEntity)
            Result.success(title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshFeeds() = withContext(Dispatchers.IO) {
        try {
            val feeds = feedDao.getAllFeeds().first()
            for (feed in feeds) {
                val channel = try {
                    rssParser.getRssChannel(feed.url)
                } catch (e: Exception) {
                    continue
                }

                for (item in channel.items) {
                    val url = item.link ?: continue
                    val existingArticle = articleDao.getArticleByUrl(url)
                    if (existingArticle == null) {
                        val articleEntity = ArticleEntity(
                            id = UUID.randomUUID().toString(),
                            title = item.title ?: "Untitled",
                            source = feed.title,
                            content = item.content ?: item.description,
                            excerpt = item.description,
                            imageUrl = item.image,
                            url = url,
                            publishedAt = item.pubDate,
                            publishedTimestamp = DateUtils.parseToTimestamp(item.pubDate),
                            isFromFeed = true,
                            isInQueue = false,
                            createdAt = System.currentTimeMillis()
                        )
                        articleDao.insertArticle(articleEntity)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
