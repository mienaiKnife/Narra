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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                return Result.failure(com.mienaiknife.narra.domain.NarraError.Content.ParsingFailed())
            }

            val publishedAt = extractPublishedDate(doc)
            val imageUrl = extractImageUrl(doc, parsedArticle.byline)

            val article = Article(
                id = UUID.randomUUID().toString(),
                title = (parsedArticle.title ?: doc.title()).ifEmpty { "Untitled" },
                source = doc.location().let { 
                    try { java.net.URL(it).host } catch (_: Exception) { null }
                } ?: "Web",
                content = parsedArticle.content ?: "",
                publishedAt = publishedAt,
                publishedTimestamp = DateUtils.parseToTimestamp(publishedAt),
                imageUrl = imageUrl,
                url = url,
                feedUrl = null,
                duration = DateUtils.estimateReadingTimeMs(parsedArticle.content),
                isInQueue = true
            )

            Result.success(article)
        } catch (e: org.jsoup.HttpStatusException) {
            Result.failure(com.mienaiknife.narra.domain.NarraError.Network.ServerError(e.statusCode, e.message))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(com.mienaiknife.narra.domain.NarraError.Network.Timeout())
        } catch (e: Exception) {
            Result.failure(com.mienaiknife.narra.domain.NarraError.Unknown(e))
        }
    }

    private fun extractPublishedDate(doc: org.jsoup.nodes.Document): String? {
        // Try JSON-LD first
        val jsonLdTags = doc.select("script[type=application/ld+json]")
        for (tag in jsonLdTags) {
            try {
                val json = Json.parseToJsonElement(tag.data())
                val date = findKeyInJson(json, "datePublished") ?: findKeyInJson(json, "dateCreated")
                if (date != null) return date
            } catch (_: Exception) {
                // Ignore malformed JSON-LD
            }
        }

        // Standard meta tags
        return doc.select("meta[property=article:published_time]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=publish-date]").attr("content").ifEmpty { null }
            ?: doc.select("meta[property=og:pubdate]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=pubdate]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=date]").attr("content").ifEmpty { null }
            ?: doc.select("time[itemprop=datePublished]").attr("datetime").ifEmpty { null }
            ?: doc.select("time").attr("datetime").ifEmpty { null }
    }

    private fun extractImageUrl(doc: org.jsoup.nodes.Document, byline: String?): String? {
        // Try JSON-LD first
        val jsonLdTags = doc.select("script[type=application/ld+json]")
        for (tag in jsonLdTags) {
            try {
                val json = Json.parseToJsonElement(tag.data())
                val image = findKeyInJson(json, "image")
                if (image != null) return image
            } catch (_: Exception) {
            }
        }

        return doc.select("meta[property=og:image]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=twitter:image]").attr("content").ifEmpty { null }
            ?: doc.select("meta[property=og:image:url]").attr("content").ifEmpty { null }
            ?: byline?.ifEmpty { null }
    }

    private fun findKeyInJson(element: kotlinx.serialization.json.JsonElement, key: String): String? {
        return when (element) {
            is JsonObject -> {
                element[key]?.jsonPrimitive?.content
                    ?: element.values.firstNotNullOfOrNull { findKeyInJson(it, key) }
            }
            is kotlinx.serialization.json.JsonArray -> {
                element.firstNotNullOfOrNull { findKeyInJson(it, key) }
            }
            else -> null
        }
    }
}
