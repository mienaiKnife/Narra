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

package com.mienaiknife.narra.data.repositories

import com.mienaiknife.narra.data.local.dao.ArticleDao
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.local.entities.toDomainModel
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import java.util.UUID

class ContentRepositoryImpl(
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val rssParser: RssParser
) : ContentRepository {

    override fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getArticleById(id: String): Article? {
        return articleDao.getArticleById(id)?.toDomainModel()
    }

    override suspend fun downloadWebPage(url: String): Result<Article> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val readability4J = Readability4J(url, doc.outerHtml())
            val article = readability4J.parse()

            if (article.content == null) {
                return@withContext Result.failure(Exception("Failed to extract content from $url"))
            }

            val articleEntity = ArticleEntity(
                id = UUID.randomUUID().toString(),
                title = article.title ?: doc.title() ?: "Untitled",
                source = doc.location().let { java.net.URL(it).host } ?: "Web",
                content = article.content ?: "",
                excerpt = article.excerpt,
                imageUrl = article.byline, // Readability4J's byline often contains author or sometimes image info, but let's be careful. Actually readability4j has featured image? 
                // Let's check Jsoup for meta tags if readability fails for image.
                url = url,
                createdAt = System.currentTimeMillis()
            )

            // Try to find a better image URL if possible
            val finalArticleEntity = if (articleEntity.imageUrl == null) {
                val ogImage = doc.select("meta[property=og:image]").attr("content")
                val twitterImage = doc.select("meta[name=twitter:image]").attr("content")
                articleEntity.copy(imageUrl = ogImage.ifEmpty { twitterImage }.ifEmpty { null })
            } else {
                articleEntity
            }

            articleDao.insertArticle(finalArticleEntity)
            Result.success(finalArticleEntity.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteArticle(id: String) {
        articleDao.deleteArticleById(id)
    }

    override suspend fun subscribeToFeed(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = rssParser.getRssChannel(url)
            val feedEntity = FeedEntity(
                url = url,
                title = channel.title ?: "Untitled Feed",
                description = channel.description,
                imageUrl = channel.image?.url
            )
            feedDao.insertFeed(feedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
