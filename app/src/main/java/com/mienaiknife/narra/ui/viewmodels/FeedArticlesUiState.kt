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
package com.mienaiknife.narra.ui.viewmodels

import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.models.Article

data class FeedArticlesUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val showPlayed: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val feedTitle: String = "",
    val downloadingArticleIds: Set<String> = emptySet(),
)
