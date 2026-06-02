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

package com.mienaiknife.narra.data.remote

import com.mienaiknife.narra.domain.models.Article

interface WebDataSource {
    /**
     * Downloads and parses a web page, extracting its content.
     * Returns a partial Article object with title, content, excerpt, etc.
     */
    suspend fun downloadArticle(url: String): Result<Article>
}
