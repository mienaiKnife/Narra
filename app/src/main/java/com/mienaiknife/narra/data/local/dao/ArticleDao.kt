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

package com.mienaiknife.narra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.ArticleWithFeed
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Transaction
    @Query("SELECT * FROM articles WHERE isInQueue = 1 ORDER BY queueOrder ASC, COALESCE(publishedTimestamp, createdAt) ASC")
    fun getQueueArticles(): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles WHERE isInQueue = 0 AND finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun getHistoryArticles(): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles ORDER BY COALESCE(publishedTimestamp, createdAt) DESC")
    fun getAllArticles(): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles WHERE isFromFeed = 1 AND isInQueue = 0 ORDER BY COALESCE(publishedTimestamp, createdAt) DESC")
    fun getInboxArticles(): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteArticles(): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles WHERE source = :source ORDER BY COALESCE(publishedTimestamp, createdAt) DESC")
    fun getArticlesBySource(source: String): Flow<List<ArticleWithFeed>>

    @Transaction
    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleWithFeedById(id: String): ArticleWithFeed?

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: String)

    @Query("UPDATE articles SET isInQueue = 0, content = NULL WHERE id = :id")
    suspend fun removeFromQueue(id: String)

    @Query("SELECT COALESCE(MAX(queueOrder), -1) + 1 FROM articles WHERE isInQueue = 1")
    suspend fun getNextQueueOrder(): Int

    @Query("UPDATE articles SET isInQueue = 1, queueOrder = (SELECT COALESCE(MAX(queueOrder), -1) + 1 FROM articles WHERE isInQueue = 1), finishedAt = NULL, progress = (CASE WHEN progress >= 1.0 THEN 0.0 ELSE progress END), currentParagraphIndex = (CASE WHEN progress >= 1.0 THEN 0 ELSE currentParagraphIndex END), currentWordOffset = (CASE WHEN progress >= 1.0 THEN 0 ELSE currentWordOffset END) WHERE id = :id")
    suspend fun addToQueue(id: String)

    @Query("UPDATE articles SET isInQueue = 0, progress = 1.0, finishedAt = :finishedAt, content = NULL WHERE id = :id")
    suspend fun markAsFinished(id: String, finishedAt: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET isInQueue = 0, progress = 1.0, finishedAt = :finishedAt, content = NULL WHERE id = :id")
    suspend fun markAsPlayed(id: String, finishedAt: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET progress = 0.0, finishedAt = NULL, currentParagraphIndex = 0, currentWordOffset = 0 WHERE id = :id")
    suspend fun markAsUnplayed(id: String)

    @Query("DELETE FROM articles WHERE isInQueue = 0 AND finishedAt IS NOT NULL")
    suspend fun clearHistory()

    @Query("DELETE FROM articles WHERE isInQueue = 0 AND isFromFeed = 1")
    suspend fun clearInbox()

    @Query("UPDATE articles SET isInQueue = 0, content = NULL")
    suspend fun clearQueue()

    @Update
    suspend fun updateArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("DELETE FROM articles WHERE isFromFeed = 1")
    suspend fun deleteAllArticlesFromFeeds()

    @Query("DELETE FROM articles WHERE source = :source AND isFromFeed = 1 AND isInQueue = 0")
    suspend fun deleteArticlesBySourceFromInbox(source: String)

    @Query("UPDATE articles SET duration = :duration WHERE id = :id")
    suspend fun updateArticleDuration(id: String, duration: Long)
}
