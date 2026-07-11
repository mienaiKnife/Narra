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

import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class ContentRepositoryImpl @Inject constructor(
    private val articleRepository: ArticleRepositoryImpl,
    private val feedRepository: FeedRepositoryImpl,
    private val importExportRepository: ImportExportRepositoryImpl,
) : ContentRepository {

    // ArticleRepository delegation
    override fun getQueueArticles(): Flow<List<Article>> = articleRepository.getQueueArticles()
    override fun getHistoryArticles(): Flow<List<Article>> = articleRepository.getHistoryArticles()
    override fun getInboxArticles(): Flow<List<Article>> = articleRepository.getInboxArticles()
    override fun getFavoriteArticles(): Flow<List<Article>> = articleRepository.getFavoriteArticles()
    override fun getAllArticles(): Flow<List<Article>> = articleRepository.getAllArticles()
    override fun getArticlesBySource(source: String): Flow<List<Article>> = articleRepository.getArticlesBySource(source)
    override fun getArticlesByFeedUrl(feedUrl: String): Flow<List<Article>> = articleRepository.getArticlesByFeedUrl(feedUrl)
    override fun searchArticles(query: String): Flow<List<Article>> = articleRepository.searchArticles(query)
    override suspend fun getArticleById(id: String): Article? = articleRepository.getArticleById(id)
    override suspend fun downloadWebPage(url: String): Result<Article> = articleRepository.downloadWebPage(url)
    override suspend fun removeFromQueue(id: String) = articleRepository.removeFromQueue(id)
    override suspend fun addToQueue(id: String): Result<Unit> = articleRepository.addToQueue(id)
    override suspend fun deleteArticle(id: String) = articleRepository.deleteArticle(id)
    override suspend fun clearHistory() = articleRepository.clearHistory()
    override suspend fun clearInbox() = articleRepository.clearInbox()
    override suspend fun clearQueue() = articleRepository.clearQueue()
    override suspend fun markAsFinished(id: String) = articleRepository.markAsFinished(id)
    override suspend fun markAsPlayed(id: String) = articleRepository.markAsPlayed(id)
    override suspend fun markAsUnplayed(id: String) = articleRepository.markAsUnplayed(id)
    override suspend fun markAllAsPlayedInFeed(feedUrl: String) = articleRepository.markAllAsPlayedInFeed(feedUrl)
    override suspend fun markAllAsUnplayedInFeed(feedUrl: String) = articleRepository.markAllAsUnplayedInFeed(feedUrl)
    override suspend fun toggleFavorite(id: String) = articleRepository.toggleFavorite(id)
    override suspend fun updateArticleProgress(id: String, progress: Float, paragraphIndex: Int, wordOffset: Int, duration: Long?) = articleRepository.updateArticleProgress(id, progress, paragraphIndex, wordOffset, duration)
    override suspend fun reorderQueue(fromIndex: Int, toIndex: Int) = articleRepository.reorderQueue(fromIndex, toIndex)
    override suspend fun updateQueueOrder(articleIds: List<String>) = articleRepository.updateQueueOrder(articleIds)
    override suspend fun pruneOldArticleContent(maxAgeDays: Int) = articleRepository.pruneOldArticleContent(maxAgeDays)

    // FeedRepository delegation
    override suspend fun subscribeToFeed(url: String): Result<String> = feedRepository.subscribeToFeed(url)
    override suspend fun refreshFeeds(): Result<Unit> = feedRepository.refreshFeeds()
    override suspend fun updateFeed(feed: FeedEntity) = feedRepository.updateFeed(feed)
    override suspend fun deleteFeed(url: String) = feedRepository.deleteFeed(url)
    override suspend fun deleteAllFeeds() = feedRepository.deleteAllFeeds()

    // ImportExportRepository delegation
    override suspend fun importEpub(inputStream: InputStream, title: String): Result<Unit> = importExportRepository.importEpub(inputStream, title)
    override suspend fun exportOpml(outputStream: OutputStream): Result<Unit> = importExportRepository.exportOpml(outputStream)
    override suspend fun backupDatabase(outputStream: OutputStream): Result<Unit> = importExportRepository.backupDatabase(outputStream)
    override suspend fun restoreDatabase(inputStream: InputStream): Result<Unit> = importExportRepository.restoreDatabase(inputStream)
    override suspend fun deleteAllMetadata() = importExportRepository.deleteAllMetadata()

    override suspend fun importOpml(inputStream: InputStream): Result<Int> {
        val result = importExportRepository.importOpml(inputStream)
        if (result.isSuccess && result.getOrThrow() > 0) {
            refreshFeeds()
        }
        return result
    }
}
