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

import com.mienaiknife.narra.domain.models.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class EpubDataSourceImpl @Inject constructor(
    private val imageDataSource: ImageDataSource
) : EpubDataSource {

    override suspend fun parseEpub(context: android.content.Context, inputStream: InputStream, fallbackTitle: String): Result<List<Article>> {
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

            val coverImageResource = book.coverImage
            val coverImageUrl = coverImageResource?.let { resource ->
                val fileName = "epub_cover_${bookTitle.hashCode()}.png"
                imageDataSource.saveImage(resource.data, fileName)
            }

            val articles = coroutineScope {
                spineReferences.mapIndexed { index, spineReference ->
                    async(Dispatchers.Default) {
                        val resource = spineReference.resource ?: return@async null

                        // Read data
                        val contentBytes = resource.data ?: return@async null
                        val content = String(contentBytes, charset(resource.inputEncoding ?: "UTF-8"))
                        val doc = Jsoup.parse(content)

                        val body = doc.body()

                        if (body.text().isNotBlank()) {
                            // Pre-clean HTML for TTS: Remove interactive or decorative elements
                            body.select("a, button, input, select, textarea").forEach { it.unwrap() }
                            body.select("script, style, noscript, iframe, svg").remove()

                            // Keep image alt text if available, otherwise remove img tags
                            body.select("img").forEach { img ->
                                val alt = img.attr("alt")
                                if (alt.isNotBlank()) {
                                    img.replaceWith(org.jsoup.nodes.TextNode(" [Image: $alt] "))
                                } else {
                                    img.remove()
                                }
                            }

                            val cleanText = body.html()

                            // Try to find a good title: TOC -> HTML title -> First heading -> resource title -> fallback
                            var chapterTitle = tocMap[resource.href]
                                ?: doc.title().takeIf { it.isNotBlank() && it != bookTitle }
                                ?: doc.select("h1, h2, h3").firstOrNull()?.text()
                                ?: resource.title
                                ?: "Chapter ${index + 1}"

                            // If title is just a number or generic "Chapter", try to append the start of the text
                            if (chapterTitle.matches(Regex("^(?i)Chapter\\s*\\d*$|^\\d+$"))) {
                                val firstSentence = body.text().take(40).substringBefore(".")
                                if (firstSentence.isNotBlank() && firstSentence.length > 5) {
                                    chapterTitle = "$chapterTitle: $firstSentence..."
                                }
                            }

                            Article(
                                id = UUID.randomUUID().toString(),
                                title = chapterTitle.trim(),
                                source = bookTitle,
                                content = cleanText,
                                imageUrl = coverImageUrl,
                                url = "epub://${bookTitle.hashCode()}/$index",
                                isInQueue = true,
                                queueOrder = index,
                                publishedAt = "By $author" // Reusing publishedAt for author info in EPUB context
                            )
                        } else {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(com.mienaiknife.narra.domain.NarraError.Unknown(e))
        }
    }
}
