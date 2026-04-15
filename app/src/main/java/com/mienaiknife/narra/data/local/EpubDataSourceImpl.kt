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

package com.mienaiknife.narra.data.local

import com.mienaiknife.narra.data.models.Article
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class EpubDataSourceImpl @Inject constructor() : EpubDataSource {

    override suspend fun parseEpub(inputStream: InputStream, fallbackTitle: String): Result<List<Article>> {
        return try {
            val book = EpubReader().readEpub(inputStream)
            val bookTitle = book.metadata.firstTitle ?: fallbackTitle
            val author = book.metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() } ?: "Unknown Author"
            
            val spineReferences = book.spine.spineReferences

            // Map resources to their TOC titles for better chapter names
            val tocMap = mutableMapOf<String, String>()
            fun walkToc(refs: List<TOCReference>) {
                for (ref in refs) {
                    ref.resource?.let { res ->
                        if (!ref.title.isNullOrBlank() && tocMap[res.href] == null) {
                            tocMap[res.href] = ref.title
                        }
                    }
                    walkToc(ref.children)
                }
            }
            walkToc(book.tableOfContents.tocReferences)

            val articles = spineReferences.mapIndexedNotNull { index, spineReference ->
                val resource = spineReference.resource
                val content = String(resource.data, charset(resource.inputEncoding ?: "UTF-8"))
                val doc = Jsoup.parse(content)
                
                val body = doc.body()
                
                if (body.text().isNotBlank()) {
                    val cleanText = body.html()

                    // Try to find a good title: TOC -> HTML title -> First heading -> resource title -> fallback
                    val chapterTitle = tocMap[resource.href]
                        ?: doc.title().takeIf { it.isNotBlank() && it != bookTitle }
                        ?: doc.select("h1, h2, h3").firstOrNull()?.text()
                        ?: resource.title
                        ?: "Chapter ${index + 1}"

                    Article(
                        id = UUID.randomUUID().toString(),
                        title = chapterTitle.trim(),
                        source = bookTitle,
                        content = cleanText,
                        imageUrl = null, // Could extract cover image later
                        url = "epub://${bookTitle.hashCode()}/$index",
                        isInQueue = true,
                        queueOrder = index,
                        publishedAt = "By $author" // Reusing publishedAt for author info in EPUB context
                    )
                } else {
                    null
                }
            }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
