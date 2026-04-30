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

import androidx.room.Embedded
import androidx.room.Relation
import com.mienaiknife.narra.data.models.Article

data class ArticleWithFeed(
    @Embedded val article: ArticleEntity,
    @Relation(
        parentColumn = "feedUrl",
        entityColumn = "url"
    )
    val feed: FeedEntity?
)

fun ArticleWithFeed.toDomainModel(): Article {
    return article.toDomainModel(
        feedImageUrl = feed?.imageUrl,
        sourceOverride = if (article.isFromFeed) feed?.title else null
    )
}
