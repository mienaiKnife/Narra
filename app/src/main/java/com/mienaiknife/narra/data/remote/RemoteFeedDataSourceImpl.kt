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

import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.domain.NarraError
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.utils.DateUtils
import com.prof18.rssparser.RssParser
import org.jsoup.Jsoup
import java.util.UUID
import javax.inject.Inject

class RemoteFeedDataSourceImpl @Inject constructor(
    private val rssParser: RssParser
) : RemoteFeedDataSource {

    override suspend fun fetchFeedMetadata(url: String): Result<FeedEntity> {
        if (!UrlUtils.isPublicUrl(url)) {
            return Result.failure(NarraError.Network.NoConnection())
        }

        return try {
            var targetUrl = url
            var channel = try {
                rssParser.getRssChannel(targetUrl)
            } catch (_: Exception) {
                null
            }

            if (channel == null) {
                // Try feed discovery
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                
                val feedLink = doc.select("link[rel=alternate][type=application/rss+xml]").attr("abs:href")
                    .ifEmpty { doc.select("link[rel=alternate][type=application/atom+xml]").attr("abs:href") }
                
                if (feedLink.isNotEmpty() && UrlUtils.isPublicUrl(feedLink)) {
                    targetUrl = feedLink
                    channel = rssParser.getRssChannel(targetUrl)
                }
            }

            if (channel == null) {
                return Result.failure(NarraError.Content.InvalidFeed())
            }

            var imageUrl = channel.image?.url
            val link = channel.link
            var title = channel.title?.trim() ?: "Untitled Feed"

            // If title looks like a URL or a domain name, try to get a better one
            if (isUrlOrDomainLike(title)) {
                if (link != null && UrlUtils.isPublicUrl(link)) {
                    try {
                        val doc = Jsoup.connect(link)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .get()
                        val siteTitle = doc.title().trim()
                        if (siteTitle.isNotEmpty() && !isUrlOrDomainLike(siteTitle)) {
                            title = siteTitle
                        }
                    } catch (_: Exception) {}
                }
            }

            // Still a URL or domain? Fallback to cleaned domain name if it's a URL
            if (isUrlOrDomainLike(title) && title.contains("://")) {
                title = UrlUtils.getDomainName(title)
            }

            if (imageUrl == null && link != null && UrlUtils.isPublicUrl(link)) {
                try {
                    val doc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get()
                    
                    imageUrl = doc.select("link[rel~=(?i)^(shortcut|apple-touch-)?icon]").attr("abs:href")
                    if (imageUrl.isNotEmpty() && !UrlUtils.isPublicUrl(imageUrl)) {
                        imageUrl = null
                    }
                    
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = doc.select("meta[property=og:image]").attr("abs:href")
                        if (imageUrl.isNotEmpty() && !UrlUtils.isPublicUrl(imageUrl)) {
                            imageUrl = null
                        }
                    }
                    
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = null
                    }
                } catch (_: Exception) {
                    // Ignore favicon fetching errors
                }
            }

            Result.success(
                FeedEntity(
                    url = targetUrl,
                    title = title,
                    description = channel.description,
                    imageUrl = imageUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    override suspend fun fetchArticles(feed: FeedEntity): Result<RemoteFeedDataSource.FetchArticlesResult> {
        return try {
            val channel = rssParser.getRssChannel(feed.url)
            val updatedTitle = channel.title?.trim()?.let {
                if (it.contains("://") || it.contains("www.") || it == "RSS" || it == "Atom") {
                    null // Don't use it if it looks like a URL
                } else {
                    it
                }
            }

            val articles = channel.items.mapNotNull { item ->
                val url = item.link ?: return@mapNotNull null
                
                android.util.Log.d("RemoteFeedDataSource", "Processing item: ${item.title}")
                android.util.Log.d("RemoteFeedDataSource", "Item standard image: ${item.image}")

                // Aggressive image extraction
                val imageUrl = item.image 
                    ?: extractImageFromHtml(item.description).also { if (it != null) android.util.Log.d("RemoteFeedDataSource", "Extracted from description: $it") }
                    ?: extractImageFromHtml(item.content).also { if (it != null) android.util.Log.d("RemoteFeedDataSource", "Extracted from content: $it") }

                Article(
                    id = UUID.randomUUID().toString(),
                    title = item.title ?: "Untitled",
                    source = if (updatedTitle != null && !isUrlOrDomainLike(updatedTitle)) updatedTitle else feed.title,
                    content = "", // Content is fetched on demand or from item.content if available
                    publishedAt = item.pubDate,
                    publishedTimestamp = DateUtils.parseToTimestamp(item.pubDate),
                    imageUrl = imageUrl,
                    url = url,
                    feedUrl = feed.url,
                    isFromFeed = true,
                    isInQueue = false
                )
            }
            Result.success(RemoteFeedDataSource.FetchArticlesResult(articles, updatedTitle))
        } catch (e: Exception) {
            Result.failure(NarraError.Unknown(e))
        }
    }

    private fun isUrlOrDomainLike(text: String): Boolean {
        return text.contains("://") || 
               text.contains("www.") || 
               text.matches(Regex(".*\\.[a-z]{2,6}$", RegexOption.IGNORE_CASE)) ||
               text == "RSS" || 
               text == "Atom" || 
               text == "Untitled Feed"
    }

    private fun extractImageFromHtml(html: String?): String? {
        if (html.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.parse(html)
            val img = doc.select("img").firstOrNull()
            if (img == null) {
                android.util.Log.d("RemoteFeedDataSource", "No img tag found in HTML")
                return null
            }

            val absSrc = img.attr("abs:src")
            val src = img.attr("src")
            val dataSrc = img.attr("data-src")
            
            android.util.Log.d("RemoteFeedDataSource", "Found img tag. abs:src='$absSrc', src='$src', data-src='$dataSrc'")

            val imageUrl = absSrc.takeIf { it.isNotBlank() }
                ?: src.takeIf { it.isNotBlank() }
                ?: dataSrc.takeIf { it.isNotBlank() }
            
            if (imageUrl == null) {
                android.util.Log.d("RemoteFeedDataSource", "No usable image URL found in attributes")
                return null
            }

            val isPublic = UrlUtils.isPublicUrl(imageUrl)
            android.util.Log.d("RemoteFeedDataSource", "Extracted URL: '$imageUrl', isPublic=$isPublic")

            imageUrl.takeIf { isPublic }
        } catch (e: Exception) {
            android.util.Log.e("RemoteFeedDataSource", "Failed to extract image from HTML", e)
            null
        }
    }
}
