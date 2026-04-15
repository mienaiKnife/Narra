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

import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.utils.DateUtils
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import java.util.UUID
import javax.inject.Inject

class WebDataSourceImpl @Inject constructor() : WebDataSource {

    override suspend fun downloadArticle(url: String): Result<Article> {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val readability4J = Readability4J(url, doc.outerHtml())
            val parsedArticle = readability4J.parse()

            if (parsedArticle.content == null) {
                return Result.failure(Exception("Failed to extract content from $url"))
            }

            val publishedAt = doc.select("meta[property=article:published_time]").attr("content").ifEmpty { null }
                ?: doc.select("meta[name=publish-date]").attr("content").ifEmpty { null }
                ?: doc.select("meta[property=og:pubdate]").attr("content").ifEmpty { null }
                ?: doc.select("meta[name=pubdate]").attr("content").ifEmpty { null }
                ?: doc.select("meta[name=date]").attr("content").ifEmpty { null }
                ?: doc.select("time[itemprop=datePublished]").attr("datetime").ifEmpty { null }
                ?: doc.select("time").attr("datetime").ifEmpty { null }

            var imageUrl = parsedArticle.byline
            if (imageUrl == null) {
                val ogImage = doc.select("meta[property=og:image]").attr("content")
                val twitterImage = doc.select("meta[name=twitter:image]").attr("content")
                imageUrl = ogImage.ifEmpty { twitterImage }.ifEmpty { null }
            }

            val article = Article(
                id = UUID.randomUUID().toString(),
                title = parsedArticle.title ?: doc.title() ?: "Untitled",
                source = doc.location().let { java.net.URL(it).host } ?: "Web",
                content = parsedArticle.content ?: "",
                publishedAt = publishedAt,
                publishedTimestamp = DateUtils.parseToTimestamp(publishedAt),
                imageUrl = imageUrl,
                url = url,
                isInQueue = true
            )

            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
