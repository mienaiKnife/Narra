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
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.models.SampleArticles
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestArticlePopulator @Inject constructor(
    private val articleDao: ArticleDao
) {
    suspend fun populate() {
        SampleArticles.all.forEach { article ->
            val entity = ArticleEntity(
                id = article.id,
                title = article.title,
                source = article.source,
                content = article.content,
                publishedAt = article.publishedAt,
                progress = article.progress ?: 0f,
                isFavorite = article.isFavorite,
                isFromFeed = article.isFromFeed,
                isInQueue = article.isInQueue,
                queueOrder = article.queueOrder,
                createdAt = System.currentTimeMillis()
            )
            articleDao.insertArticle(entity)
        }
    }
}
