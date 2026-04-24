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

import com.mienaiknife.narra.data.models.Article
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun getQueueArticles(): Flow<List<Article>>
    fun getHistoryArticles(): Flow<List<Article>>
    fun getInboxArticles(): Flow<List<Article>>
    fun getFavoriteArticles(): Flow<List<Article>>
    fun getAllArticles(): Flow<List<Article>>
    fun getArticlesBySource(source: String): Flow<List<Article>>
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
    suspend fun subscribeToFeed(url: String): Result<String>
    suspend fun refreshFeeds(): Result<Unit>
    suspend fun deleteFeed(url: String)
    suspend fun importEpub(inputStream: java.io.InputStream, title: String): Result<Unit>
    suspend fun importOpml(inputStream: java.io.InputStream): Result<Int>
    suspend fun exportOpml(outputStream: java.io.OutputStream): Result<Unit>
    suspend fun markAsFinished(id: String)
    suspend fun markAsPlayed(id: String)
    suspend fun markAsUnplayed(id: String)
    suspend fun updateArticleProgress(id: String, progress: Float, paragraphIndex: Int, wordOffset: Int, duration: Long? = null)
    suspend fun toggleFavorite(id: String)
    suspend fun deleteAllMetadata()
    suspend fun deleteAllFeeds()
    suspend fun backupDatabase(outputStream: java.io.OutputStream): Result<Unit>
    suspend fun restoreDatabase(inputStream: java.io.InputStream): Result<Unit>
}
