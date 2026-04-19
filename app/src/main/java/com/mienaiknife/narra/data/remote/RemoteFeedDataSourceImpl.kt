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
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.utils.DateUtils
import com.prof18.rssparser.RssParser
import org.jsoup.Jsoup
import java.util.UUID
import javax.inject.Inject

class RemoteFeedDataSourceImpl @Inject constructor(
    private val rssParser: RssParser
) : RemoteFeedDataSource {

    override suspend fun fetchFeedMetadata(url: String): Result<FeedEntity> {
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
                
                if (feedLink.isNotEmpty()) {
                    targetUrl = feedLink
                    channel = rssParser.getRssChannel(targetUrl)
                }
            }

            if (channel == null) {
                return Result.failure(com.mienaiknife.narra.domain.NarraError.Content.InvalidFeed())
            }

            var imageUrl = channel.image?.url
            val link = channel.link
            val title = channel.title ?: "Untitled Feed"

            if (imageUrl == null && link != null) {
                try {
                    val doc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get()
                    
                    imageUrl = doc.select("link[rel~=(?i)^(shortcut|apple-touch-)?icon]").attr("abs:href")
                    if (imageUrl.isEmpty()) {
                        imageUrl = doc.select("meta[property=og:image]").attr("abs:href")
                    }
                    if (imageUrl.isEmpty()) {
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
            Result.failure(e)
        }
    }

    override suspend fun fetchArticles(feed: FeedEntity): Result<List<Article>> {
        return try {
            val channel = rssParser.getRssChannel(feed.url)
            val articles = channel.items.mapNotNull { item ->
                val url = item.link ?: return@mapNotNull null
                Article(
                    id = UUID.randomUUID().toString(),
                    title = item.title ?: "Untitled",
                    source = feed.title,
                    content = "", // Content is fetched on demand or from item.content if available
                    publishedAt = item.pubDate,
                    publishedTimestamp = DateUtils.parseToTimestamp(item.pubDate),
                    imageUrl = item.image,
                    url = url,
                    isFromFeed = true,
                    isInQueue = false
                )
            }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
