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

package com.mienaiknife.narra.domain.repository

import com.mienaiknife.narra.domain.models.Article
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun getQueueArticles(): Flow<List<Article>>
    fun getHistoryArticles(): Flow<List<Article>>
    fun getInboxArticles(): Flow<List<Article>>
    fun getFavoriteArticles(): Flow<List<Article>>
    fun getAllArticles(): Flow<List<Article>>
    fun getArticlesBySource(source: String): Flow<List<Article>>
    fun getArticlesByFeedUrl(feedUrl: String): Flow<List<Article>>
    fun searchArticles(query: String): Flow<List<Article>>
    suspend fun getArticleById(id: String): Article?
    suspend fun downloadWebPage(url: String): Result<Article>
    suspend fun removeFromQueue(id: String)
    suspend fun addToQueue(id: String): Result<Unit>
    suspend fun deleteArticle(id: String)
    suspend fun clearHistory()
    suspend fun clearInbox()
    suspend fun clearQueue()
    suspend fun reorderQueue(fromIndex: Int, toIndex: Int)
    suspend fun updateQueueOrder(articleIds: List<String>)
    suspend fun markAllAsPlayedInFeed(feedUrl: String)
    suspend fun markAllAsUnplayedInFeed(feedUrl: String)
    suspend fun markAsFinished(id: String)
    suspend fun markAsPlayed(id: String)
    suspend fun markAsUnplayed(id: String)
    suspend fun updateArticleProgress(id: String, progress: Float, paragraphIndex: Int, wordOffset: Int, duration: Long? = null)
    suspend fun toggleFavorite(id: String)
    suspend fun pruneOldArticleContent(maxAgeDays: Int)
}
