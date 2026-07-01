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
package com.mienaiknife.narra.utils

import org.jsoup.Jsoup

object HtmlUtils {
    /**
     * Decodes HTML entities (like &quot;) from a string.
     */
    fun decodeHtmlEntities(text: String?): String? {
        if (text == null) return null
        if (!text.contains('&')) return text
        return try {
            Jsoup.parse(text).text()
        } catch (e: Exception) {
            text
        }
    }
}
