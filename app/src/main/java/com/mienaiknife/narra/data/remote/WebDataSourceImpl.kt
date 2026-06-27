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
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.UUID
import javax.inject.Inject

class WebDataSourceImpl
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
) : WebDataSource {
    override suspend fun downloadArticle(url: String): Result<Article> {
        if (!UrlUtils.isPublicUrl(url)) {
            return Result.failure(
                com.mienaiknife.narra.domain.NarraError.Network
                    .NoConnection(),
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                        ).header("Referer", "https://www.google.com/")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        com.mienaiknife.narra.domain.NarraError.Network
                            .ServerError(response.code, response.message),
                    )
                }

                val html =
                    response.body?.string()
                        ?: return@withContext Result.failure(
                            com.mienaiknife.narra.domain.NarraError.Content
                                .ParsingFailed(),
                        )
                val doc = Jsoup.parse(html, url)

                preCleanDocument(doc)

                val readability4J = Readability4J(url, doc.outerHtml())
                val parsedArticle = readability4J.parse()

                if (parsedArticle.content == null) {
                    return@withContext Result.failure(
                        com.mienaiknife.narra.domain.NarraError.Content
                            .ParsingFailed(),
                    )
                }

                val publishedAt = extractPublishedDate(doc)
                val imageUrl = extractImageUrl(doc, parsedArticle.byline)

                val article =
                    Article(
                        id = UUID.randomUUID().toString(),
                        title = (parsedArticle.title ?: doc.title()).ifEmpty { "Untitled" },
                        source =
                        doc.location().let {
                            try {
                                java.net.URL(it).host
                            } catch (_: Exception) {
                                null
                            }
                        } ?: "Web",
                        content = parsedArticle.content ?: "",
                        publishedAt = publishedAt,
                        publishedTimestamp = DateUtils.parseToTimestamp(publishedAt),
                        imageUrl = imageUrl,
                        url = url,
                        feedUrl = null,
                        duration = DateUtils.estimateReadingTimeMs(parsedArticle.content),
                        isInQueue = true,
                    )

                Result.success(article)
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(
                    com.mienaiknife.narra.domain.NarraError.Network
                        .Timeout(),
                )
            } catch (e: Exception) {
                Result.failure(
                    com.mienaiknife.narra.domain.NarraError
                        .Unknown(e),
                )
            }
        }
    }

    private fun preCleanDocument(doc: org.jsoup.nodes.Document) {
        // Protect inline SVGs by converting them to img tags with data URIs
        // Readability strips <svg> but preserves <img>
        doc.select("svg").forEach { svg ->
            val svgHtml = svg.outerHtml()
            val base64 = java.util.Base64.getEncoder().encodeToString(svgHtml.toByteArray())
            val dataUri = "data:image/svg+xml;base64,$base64"

            val img = doc.createElement("img")
            img.attr("src", dataUri)

            // Try to find alt text in parent or svg itself
            val alt = svg.attr("alt").ifEmpty {
                svg.parent()?.takeIf { it.tagName() == "span" }?.attr("alt")
            }?.ifEmpty { null }

            if (alt != null) {
                img.attr("alt", alt)
            }

            svg.replaceWith(img)
        }

        val junkSelectors =
            listOf(
                "nav",
                "footer",
                "aside",
                "script",
                "style",
                "noscript",
                "iframe",
                "form",
                ".social",
                ".share",
                ".ad-",
                ".banner",
                ".related",
                ".recommend",
                ".comment",
                "#social",
                "#share",
                "#ad-",
                "#banner",
                "#related",
                "#recommend",
                "#comment",
                "[class*=social]",
                "[class*=share]",
                "[class*=related]",
                "[class*=recommend]",
                "[id*=social]",
                "[id*=share]",
                "[id*=related]",
                "[id*=recommend]",
            )
        junkSelectors.forEach { selector ->
            doc.select(selector).forEach { element ->
                // Don't remove high-level structural tags even if they match a junk selector
                if (element.tagName() !in listOf("body", "html", "article", "main")) {
                    element.remove()
                }
            }
        }
    }

    private fun extractPublishedDate(doc: org.jsoup.nodes.Document): String? {
        // Try JSON-LD first
        val jsonLdTags = doc.select("script[type=application/ld+json]")
        for (tag in jsonLdTags) {
            try {
                val json = Json.parseToJsonElement(tag.data())
                val date =
                    findKeyInJson(json, "datePublished")
                        ?: findKeyInJson(json, "dateCreated")
                        ?: findKeyInJson(json, "uploadDate")
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
            ?: doc.select("meta[property=og:article:published_time]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=dc.date]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=dc.date.issued]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=dcterms.created]").attr("content").ifEmpty { null }
            // Specific Substack meta tag
            ?: doc.select("meta[property=og:article:published_time]").attr("content").ifEmpty { null }
            ?: doc.select("time[itemprop=datePublished]").attr("datetime").ifEmpty { null }
            ?: doc
                .select("time[datetime]")
                .firstOrNull()
                ?.attr("datetime")
                ?.ifEmpty { null }
    }

    private fun extractImageUrl(
        doc: org.jsoup.nodes.Document,
        byline: String?,
    ): String? {
        // Try JSON-LD first
        val jsonLdTags = doc.select("script[type=application/ld+json]")
        for (tag in jsonLdTags) {
            try {
                val json = Json.parseToJsonElement(tag.data())
                val image =
                    findKeyInJson(json, "image")
                        ?: findKeyInJson(json, "thumbnailUrl")
                if (image != null) return image
            } catch (_: Exception) {
            }
        }

        return doc.select("meta[property=og:image:secure_url]").attr("content").ifEmpty { null }
            ?: doc.select("meta[property=og:image]").attr("content").ifEmpty { null }
            ?: doc.select("meta[name=twitter:image]").attr("content").ifEmpty { null }
            ?: doc.select("meta[property=og:image:url]").attr("content").ifEmpty { null }
            ?: doc.select("link[rel=image_src]").attr("href").ifEmpty { null }
            ?: byline?.ifEmpty { null }
    }

    private fun findKeyInJson(
        element: kotlinx.serialization.json.JsonElement,
        key: String,
    ): String? = when (element) {
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
