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

package com.mienaiknife.narra.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mienaiknife.narra.domain.models.Article

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["isInQueue", "queueOrder", "sortTimestamp"]),
        Index(value = ["isFromFeed", "sortTimestamp"]),
        Index(value = ["isFavorite", "sortTimestamp"]),
        Index(value = ["feedUrl"]),
        Index(value = ["source", "sortTimestamp"]),
        Index(value = ["url"], unique = true),
        Index(value = ["title"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["finishedAt"])
    ]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val content: String?,
    val excerpt: String? = null,
    val imageUrl: String? = null,
    val localImageUrl: String? = null,
    val url: String? = null,
    val progress: Float = 0f,
    val currentParagraphIndex: Int = 0,
    val currentWordOffset: Int = 0,
    val isFavorite: Boolean = false,
    val isFromFeed: Boolean = false,
    val isInQueue: Boolean = true,
    val queueOrder: Int = 0,
    val publishedAt: String? = null,
    val publishedTimestamp: Long? = null,
    val feedUrl: String? = null,
    val duration: Long? = null,
    val finishedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortTimestamp: Long = publishedTimestamp ?: createdAt
)

fun ArticleEntity.toDomainModel(
    feedImageUrl: String? = null,
    sourceOverride: String? = null
): Article {
    return Article(
        id = id,
        title = title,
        source = sourceOverride ?: source,
        publishedAt = publishedAt,
        content = content ?: "",
        imageUrl = imageUrl,
        localImageUrl = localImageUrl,
        feedImageUrl = feedImageUrl,
        url = url,
        feedUrl = feedUrl,
        progress = progress,
        currentParagraphIndex = currentParagraphIndex,
        currentWordOffset = currentWordOffset,
        isFavorite = isFavorite,
        isFromFeed = isFromFeed,
        isInQueue = isInQueue,
        queueOrder = queueOrder,
        publishedTimestamp = publishedTimestamp,
        duration = duration,
        lastPlayedAt = lastPlayedAt
    )
}
