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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

        body.children().forEach { element ->
            when (element.tagName()) {
                "p" -> blocks.add(ContentBlock.Paragraph(parseElement(element)))
                "blockquote" -> blocks.add(ContentBlock.BlockQuote(parseElement(element)))
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = element.tagName().substring(1).toInt()
                    blocks.add(ContentBlock.Heading(parseElement(element), level))
                }
                else -> {
                    // Fallback for other tags or top-level text
                    val text = parseElement(element)
                    if (text.isNotEmpty()) {
                        blocks.add(ContentBlock.Paragraph(text))
                    }
                }
            }
        }

        // If no blocks were found but there is text (e.g., plain text input)
        if (blocks.isEmpty() && html.isNotBlank()) {
            // Split by double newline to handle plain text paragraphs
            html.split(Regex("\\n\\s*\\n")).forEach { p ->
                val trimmed = p.trim()
                if (trimmed.isNotEmpty()) {
                    blocks.add(ContentBlock.Paragraph(buildAnnotatedString { append(trimmed) }))
                }
            }
        }

        return blocks
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
            is TextNode -> builder.append(node.text())
            is Element -> {
                val style = getStyleForTag(node.tagName())
                if (style != null) {
                    builder.withStyle(style) {
                        node.childNodes().forEach { traverse(it, builder) }
                    }
                } else {
                    node.childNodes().forEach { traverse(it, builder) }
                }
                
                // Add newlines for certain tags if they are nested
                if (node.tagName() == "br") {
                    builder.append("\n")
                }
            }
        }
    }

    private fun getStyleForTag(tagName: String): SpanStyle? {
        return when (tagName) {
            "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            "code" -> SpanStyle(background = Color.LightGray.copy(alpha = 0.3f))
            else -> null
        }
    }
}
