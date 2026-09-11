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
package com.mienaiknife.narra.data.parsing

import com.mienaiknife.narra.domain.models.ContentBlock
import com.mienaiknife.narra.domain.models.RichText
import com.mienaiknife.narra.domain.models.RichTextSpan
import com.mienaiknife.narra.domain.models.RichTextSpanStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlParser {
    private data class ListContext(val type: String, var index: Int = 0)

    fun parse(
        html: String,
        baseUrl: String? = null,
    ): List<ContentBlock> {
        val document =
            if (baseUrl != null) {
                Jsoup.parseBodyFragment(html, baseUrl)
            } else {
                Jsoup.parseBodyFragment(html)
            }
        val body = document.body()
        val blocks = mutableListOf<ContentBlock>()

        parseNodes(body.childNodes(), blocks)

        return blocks
    }

    private fun parseNodes(
        nodes: List<Node>,
        blocks: MutableList<ContentBlock>,
    ) {
        val currentInlineNodes = mutableListOf<Node>()
        var pendingListPrefix: String? = null

        fun flushInline() {
            if (currentInlineNodes.isNotEmpty() || pendingListPrefix != null) {
                val richText =
                    RichTextBuilder().apply {
                        pendingListPrefix?.let { append(it) }
                        pendingListPrefix = null
                        currentInlineNodes.forEach { traverse(it, this) }
                    }.build()
                val trimmed = richText.trim()
                if (!trimmed.isBlank()) {
                    addBlocks(trimmed, blocks)
                }
                currentInlineNodes.clear()
            }
        }

        fun walk(
            nodeList: List<Node>,
            listContext: ListContext? = null,
        ) {
            nodeList.forEach { node ->
                when (node) {
                    is Element -> {
                        val tagName = node.tagName()
                        when {
                            tagName == "img" || tagName == "svg" -> {
                                flushInline()
                                val src =
                                    if (tagName == "img") {
                                        node.absUrl("src").ifEmpty { node.attr("src") }
                                    } else {
                                        val base64 = java.util.Base64.getEncoder().encodeToString(node.outerHtml().toByteArray())
                                        "data:image/svg+xml;base64,$base64"
                                    }

                                if (src.isNotEmpty()) {
                                    val alt =
                                        node.attr("alt").ifEmpty {
                                            node.parent()?.takeIf { it.tagName() == "span" }?.attr("alt")
                                        }?.ifEmpty { null }
                                    blocks.add(ContentBlock.Image(src, alt))
                                }
                            }
                            tagName == "hr" -> {
                                flushInline()
                                blocks.add(ContentBlock.HorizontalRule)
                            }
                            tagName == "table" -> {
                                flushInline()
                                blocks.add(parseTable(node))
                            }
                            tagName == "ul" || tagName == "ol" -> {
                                flushInline()
                                walk(node.childNodes(), ListContext(tagName))
                                flushInline()
                            }
                            tagName == "li" -> {
                                flushInline()
                                listContext?.let { it.index++ }
                                pendingListPrefix = if (listContext?.type == "ol") "${listContext.index}. " else "• "

                                walk(node.childNodes(), listContext)
                                flushInline()
                            }
                            tagName == "p" || (tagName.startsWith("h") && tagName.length == 2 && tagName[1].isDigit()) -> {
                                flushInline()
                                if (node.select("p, li, div, blockquote, img, svg, table, ul, ol, hr").isNotEmpty()) {
                                    walk(node.childNodes(), listContext)
                                    flushInline()
                                } else {
                                    if (tagName == "p") {
                                        addBlocks(parseElement(node), blocks)
                                    } else {
                                        val level = tagName.substring(1).toIntOrNull() ?: 1
                                        blocks.add(ContentBlock.Heading(parseElement(node), level))
                                    }
                                }
                            }
                            tagName == "div" -> {
                                if (node.select("p, li, div, blockquote, img, svg, table, ul, ol, hr").isNotEmpty()) {
                                    flushInline()
                                    walk(node.childNodes(), listContext)
                                    flushInline()
                                } else {
                                    flushInline()
                                    addBlocks(parseElement(node), blocks)
                                }
                            }
                            tagName == "blockquote" -> {
                                if (node.select("p, li, div, blockquote, img, svg, table, ul, ol, hr").isNotEmpty()) {
                                    flushInline()
                                    walk(node.childNodes(), listContext)
                                    flushInline()
                                } else {
                                    flushInline()
                                    blocks.add(ContentBlock.BlockQuote(parseElement(node)))
                                }
                            }
                            node.isBlock -> {
                                flushInline()
                                walk(node.childNodes(), listContext)
                                flushInline()
                            }
                            else -> {
                                if (node.select("p, li, div, blockquote, img, svg, table, ul, ol, hr").isNotEmpty()) {
                                    walk(node.childNodes(), listContext)
                                } else {
                                    currentInlineNodes.add(node)
                                }
                            }
                        }
                    }
                    is TextNode -> {
                        currentInlineNodes.add(node)
                    }
                }
            }
        }

        walk(nodes)
        flushInline()
    }

    private fun addBlocks(
        richText: RichText,
        blocks: MutableList<ContentBlock>,
    ) {
        val parts = splitRichText(richText, Regex("\\n\\s*\\n+"))
        parts.forEach { part ->
            val trimmed = part.trim()
            if (!trimmed.isBlank()) {
                // Split long paragraphs to avoid TTS engine limits (typically ~4000 chars)
                val splitParts = splitLongParagraph(trimmed, maxLength = 3000)
                splitParts.forEach {
                    blocks.add(ContentBlock.Paragraph(it))
                }
            }
        }
    }

    private fun splitLongParagraph(
        richText: RichText,
        maxLength: Int,
    ): List<RichText> {
        if (richText.length <= maxLength) return listOf(richText)

        val result = mutableListOf<RichText>()
        val text = richText.text
        var currentStart = 0

        while (currentStart < text.length) {
            var currentEnd = (currentStart + maxLength).coerceAtMost(text.length)

            if (currentEnd < text.length) {
                // Try to find a good breaking point (sentence end or space)
                val searchRange = text.substring(currentStart, currentEnd)
                val lastSentenceEnd = searchRange.lastIndexOfAny(listOf(".", "!", "?", "。", "！", "？"))

                if (lastSentenceEnd != -1 && lastSentenceEnd > maxLength / 2) {
                    currentEnd = currentStart + lastSentenceEnd + 1
                } else {
                    val lastSpace = searchRange.lastIndexOf(' ')
                    if (lastSpace != -1 && lastSpace > maxLength / 2) {
                        currentEnd = currentStart + lastSpace + 1
                    }
                }
            }

            result.add(richText.subSequence(currentStart, currentEnd))
            currentStart = currentEnd
        }

        return result
    }

    private fun splitRichText(
        richText: RichText,
        regex: Regex,
    ): List<RichText> {
        val text = richText.text
        val result = mutableListOf<RichText>()
        var lastStart = 0
        regex.findAll(text).forEach { match ->
            result.add(richText.subSequence(lastStart, match.range.first))
            lastStart = match.range.last + 1
        }
        result.add(richText.subSequence(lastStart, text.length))
        return result
    }

    private fun parseElement(element: Element): RichText {
        val richText =
            RichTextBuilder().apply {
                traverse(element, this)
            }.build()
        return richText.trim()
    }

    private fun traverse(
        node: Node,
        builder: RichTextBuilder,
    ) {
        when (node) {
            is TextNode -> {
                val text = node.wholeText
                builder.append(normalizeWhitespace(text))
            }
            is Element -> {
                val tagName = node.tagName()

                // Skip non-content elements
                if (tagName == "style" ||
                    tagName == "script" ||
                    tagName == "head" ||
                    tagName == "link" ||
                    tagName == "meta" ||
                    tagName == "svg" ||
                    tagName == "img" ||
                    tagName == "html" ||
                    tagName == "body"
                ) {
                    if (tagName == "html" || tagName == "body") {
                        node.childNodes().forEach { traverse(it, builder) }
                    }
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

                // Add a single newline for block-level tags to ensure separation if they are
                // encountered in a context where they aren't already triggering a flush.
                if (tagName == "p" ||
                    tagName == "div" ||
                    tagName == "li" ||
                    tagName == "blockquote" ||
                    (tagName.startsWith("h") && tagName.length == 2)
                ) {
                    builder.append("\n")
                } else if (tagName == "br") {
                    builder.append("\n")
                } else if (tagName == "hr") {
                    builder.append("\n---\n")
                }
            }
        }
    }

    private fun parseTable(element: Element): ContentBlock.Table {
        val rows = mutableListOf<List<ContentBlock.Table.Cell>>()
        element.select("tr").forEach { tr ->
            val cells = mutableListOf<ContentBlock.Table.Cell>()
            tr.select("th, td").forEach { cell ->
                cells.add(
                    ContentBlock.Table.Cell(
                        text = parseElement(cell),
                        isHeader = cell.tagName() == "th",
                    ),
                )
            }
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }
        return ContentBlock.Table(rows)
    }

    private fun normalizeWhitespace(text: String): String {
        // Standard HTML whitespace rules collapse all whitespace into a single space.
        return text
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
    }

    private fun getStyleForTag(tagName: String): RichTextSpanStyle? = when (tagName) {
        "b", "strong" -> RichTextSpanStyle.BOLD
        "i", "em" -> RichTextSpanStyle.ITALIC
        "u" -> RichTextSpanStyle.UNDERLINE
        "del", "s", "strike" -> RichTextSpanStyle.STRIKETHROUGH
        "h1", "h2", "h3", "h4", "h5", "h6" -> RichTextSpanStyle.BOLD
        "code" -> RichTextSpanStyle.CODE
        "sup" -> RichTextSpanStyle.SUPERSCRIPT
        "sub" -> RichTextSpanStyle.SUBSCRIPT
        "a" -> RichTextSpanStyle.UNDERLINE
        else -> null
    }
}

private class RichTextBuilder {
    private enum class AnnotationKind { LINK, FOOTNOTE }

    private val text = StringBuilder()
    private val spans = mutableListOf<RichTextSpan>()
    private val styles = ArrayDeque<RichTextSpanStyle>()
    private val links = ArrayDeque<String>()
    private val annotations = ArrayDeque<AnnotationKind>()
    private var footnoteDepth = 0

    fun append(value: String) {
        val start = text.length
        text.append(value)
        val end = text.length
        if (start == end) return
        styles.forEach { style -> spans.add(RichTextSpan(start, end, style = style)) }
        links.lastOrNull()?.let { href -> spans.add(RichTextSpan(start, end, link = href)) }
        if (footnoteDepth > 0) spans.add(RichTextSpan(start, end, isFootnote = true))
    }

    fun pushStringAnnotation(tag: String, value: String) {
        when (tag) {
            "link" -> {
                links.addLast(value)
                annotations.addLast(AnnotationKind.LINK)
            }
            "footnote" -> {
                footnoteDepth++
                annotations.addLast(AnnotationKind.FOOTNOTE)
            }
        }
    }

    fun pop() {
        when (annotations.removeLastOrNull()) {
            AnnotationKind.LINK -> links.removeLastOrNull()
            AnnotationKind.FOOTNOTE -> footnoteDepth = (footnoteDepth - 1).coerceAtLeast(0)
            null -> Unit
        }
    }

    fun withStyle(
        style: RichTextSpanStyle,
        block: RichTextBuilder.() -> Unit,
    ) {
        styles.addLast(style)
        block()
        styles.removeLastOrNull()
    }

    fun build(): RichText = RichText(text.toString(), spans.toList())
}
