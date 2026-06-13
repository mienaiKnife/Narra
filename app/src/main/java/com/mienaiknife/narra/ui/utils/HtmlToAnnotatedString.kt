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

package com.mienaiknife.narra.ui.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.mienaiknife.narra.ui.models.ContentBlock
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlParser {

    fun parse(html: String): List<ContentBlock> {
        val document = Jsoup.parseBodyFragment(html)
        val body = document.body()
        val blocks = mutableListOf<ContentBlock>()

        parseNodes(body.childNodes(), blocks)

        return blocks
    }

    private fun parseNodes(nodes: List<Node>, blocks: MutableList<ContentBlock>) {
        val currentInlineNodes = mutableListOf<Node>()

        fun flushInline() {
            if (currentInlineNodes.isNotEmpty()) {
                val annotatedString = buildAnnotatedString {
                    currentInlineNodes.forEach { traverse(it, this) }
                }
                addBlocksFromAnnotatedString(annotatedString, blocks)
                currentInlineNodes.clear()
            }
        }

        nodes.forEach { node ->
            when (node) {
                is Element -> {
                    val tagName = node.tagName()
                    when {
                        tagName == "p" || tagName == "li" -> {
                            flushInline()
                            addBlocksFromAnnotatedString(parseElement(node), blocks)
                        }
                        tagName == "blockquote" -> {
                            flushInline()
                            blocks.add(ContentBlock.BlockQuote(parseElement(node)))
                        }
                        tagName == "img" -> {
                            flushInline()
                            val src = node.attr("src")
                            if (src.isNotEmpty()) {
                                blocks.add(ContentBlock.Image(src, node.attr("alt").ifEmpty { null }))
                            }
                        }
                        tagName.startsWith("h") && tagName.length == 2 && tagName[1].isDigit() -> {
                            flushInline()
                            val level = tagName.substring(1).toIntOrNull() ?: 1
                            blocks.add(ContentBlock.Heading(parseElement(node), level))
                        }
                        node.isBlock -> {
                            flushInline()
                            parseNodes(node.childNodes(), blocks)
                        }
                        else -> {
                            currentInlineNodes.add(node)
                        }
                    }
                }
                is TextNode -> {
                    currentInlineNodes.add(node)
                }
            }
        }
        flushInline()
    }

    private fun addBlocksFromAnnotatedString(annotatedString: AnnotatedString, blocks: MutableList<ContentBlock>) {
        val parts = splitAnnotatedString(annotatedString, Regex("\\n\\s*\\n+"))
        parts.forEach { part ->
            val trimmed = part.trim()
            if (trimmed.isNotEmpty()) {
                blocks.add(ContentBlock.Paragraph(trimmed))
            }
        }
    }

    private fun splitAnnotatedString(annotatedString: AnnotatedString, regex: Regex): List<AnnotatedString> {
        val text = annotatedString.text
        val result = mutableListOf<AnnotatedString>()
        var lastStart = 0
        regex.findAll(text).forEach { match ->
            result.add(annotatedString.subSequence(lastStart, match.range.first))
            lastStart = match.range.last + 1
        }
        result.add(annotatedString.subSequence(lastStart, text.length))
        return result
    }

    private fun parseElement(element: Element): AnnotatedString {
        val annotatedString = buildAnnotatedString {
            traverse(element, this)
        }
        return annotatedString.trim()
    }

    private fun AnnotatedString.trim(): AnnotatedString {
        val text = this.text
        val start = text.indexOfFirst { !it.isWhitespace() }
        val end = text.indexOfLast { !it.isWhitespace() }
        if (start == -1 || end == -1) return AnnotatedString("")
        return this.subSequence(start, end + 1)
    }

    private fun traverse(node: Node, builder: AnnotatedString.Builder) {
        when (node) {
            is TextNode -> {
                val text = node.wholeText
                builder.append(normalizeWhitespace(text))
            }
            is Element -> {
                val tagName = node.tagName()
                
                // Skip non-content elements
                if (tagName == "style" || tagName == "script" || tagName == "head" || 
                    tagName == "link" || tagName == "meta" || tagName == "svg") {
                    return
                }

                val style = getStyleForTag(tagName)
                
                if (tagName == "sup") {
                    builder.pushStringAnnotation("footnote", "true")
                }
                if (tagName == "a") {
                    builder.pushStringAnnotation("link", node.attr("href"))
                }

                if (style != null) {
                    builder.withStyle(style) {
                        node.childNodes().forEach { traverse(it, builder) }
                    }
                } else {
                    node.childNodes().forEach { traverse(it, builder) }
                }

                if (tagName == "sup" || tagName == "a") {
                    builder.pop()
                }
                
                // Add newlines for block-level tags to ensure separation if they are
                // encountered in a context where they aren't already triggering a flush.
                if (tagName == "br" || tagName == "p" || tagName == "div" || tagName == "li" || 
                    tagName == "blockquote" || (tagName.startsWith("h") && tagName.length == 2)) {
                    builder.append("\n")
                }
            }
        }
    }

    private fun normalizeWhitespace(text: String): String {
        return text.replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
    }

    private fun getStyleForTag(tagName: String): SpanStyle? {
        return when (tagName) {
            "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "h1", "h2", "h3", "h4", "h5", "h6" -> SpanStyle(fontWeight = FontWeight.Bold)
            "code" -> SpanStyle(background = Color.LightGray.copy(alpha = 0.3f))
            "sup" -> SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 12.sp
            )
            "sub" -> SpanStyle(
                baselineShift = BaselineShift.Subscript,
                fontSize = 12.sp
            )
            "a" -> SpanStyle(
                textDecoration = TextDecoration.Underline
            )
            else -> null
        }
    }
}

fun AnnotatedString.toSpeakableText(context: Context, shortenLinks: Boolean = true): String {
    val result = StringBuilder(text)
    
    // 1. Handle footnotes: replace with spaces to preserve length
    val footnotes = getStringAnnotations("footnote", 0, length)
    for (annotation in footnotes) {
        for (i in annotation.start until annotation.end) {
            if (i < result.length) result.setCharAt(i, ' ')
        }
    }

    if (!shortenLinks) return result.toString()

    // 2. Handle links: shorten if they look like URLs, but preserve length via padding
    val links = getStringAnnotations("link", 0, length)
    val linkToPrefix = context.getString(com.mienaiknife.narra.R.string.reader_link_to)

    for (annotation in links) {
        val start = annotation.start
        val end = annotation.end
        val originalLength = end - start
        if (originalLength <= 0) continue

        val linkText = text.substring(start, end).trim()
        if (isUrlLike(linkText)) {
            val simplified = try {
                linkToPrefix.format(simplifyUrl(annotation.item))
            } catch (_: Exception) {
                linkText
            }
            
            if (simplified.length <= originalLength) {
                val padded = simplified.padEnd(originalLength, ' ')
                for (i in 0 until originalLength) {
                    result.setCharAt(start + i, padded[i])
                }
            } else {
                // Truncate if simplified text is somehow longer than original (rare for URLs)
                for (i in 0 until originalLength) {
                    result.setCharAt(start + i, simplified[i])
                }
            }
        }
    }
    
    return result.toString()
}

private fun isUrlLike(text: String): Boolean {
    return text.startsWith("http://") || 
           text.startsWith("https://") || 
           text.contains(Regex("\\.[a-z]{2,3}/")) ||
           text.split("/").size > 2
}

private fun simplifyUrl(url: String): String {
    return try {
        val uri = url.toUri()
        val host = uri.host ?: return url
        host.removePrefix("www.")
    } catch (_: Exception) {
        url
    }
}
