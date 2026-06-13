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

package com.mienaiknife.narra.domain.models

data class Article(
    val id: String,
    val title: String,
    val source: String,
    val publishedAt: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val localImageUrl: String? = null,
    val feedImageUrl: String? = null,
    val url: String? = null,
    val feedUrl: String? = null,
    val progress: Float? = null,
    val currentParagraphIndex: Int = 0,
    val currentWordOffset: Int = 0,
    val isFavorite: Boolean = false,
    val isFromFeed: Boolean = false,
    val isInQueue: Boolean = true,
    val isInInbox: Boolean = false,
    val queueOrder: Int = 0,
    val publishedTimestamp: Long? = null,
    val duration: Long? = null,
    val lastPlayedAt: Long? = null
)
