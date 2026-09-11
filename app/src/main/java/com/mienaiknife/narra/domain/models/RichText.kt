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
package com.mienaiknife.narra.domain.models

/**
 * A Compose-free representation of styled text. The UI layer maps this to an
 * `AnnotatedString`, while the data/presentation layers can consume the plain text and spans.
 */
data class RichText(
    val text: String,
    val spans: List<RichTextSpan> = emptyList(),
) {
    val length: Int get() = text.length

    fun isBlank(): Boolean = text.isBlank()

    fun subSequence(start: Int, end: Int): RichText {
        val newText = text.substring(start, end)
        val newSpans =
            spans.mapNotNull { span ->
                val spanStart = maxOf(span.start, start)
                val spanEnd = minOf(span.end, end)
                if (spanStart >= spanEnd) {
                    null
                } else {
                    span.copy(start = spanStart - start, end = spanEnd - start)
                }
            }
        return RichText(newText, newSpans)
    }

    fun trim(): RichText {
        val start = text.indexOfFirst { !it.isWhitespace() }
        val end = text.indexOfLast { !it.isWhitespace() }
        if (start == -1 || end == -1) return RichText("")
        return subSequence(start, end + 1)
    }
}

data class RichTextSpan(
    val start: Int,
    val end: Int,
    val style: RichTextSpanStyle? = null,
    val link: String? = null,
    val isFootnote: Boolean = false,
)

enum class RichTextSpanStyle {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE,
    SUPERSCRIPT,
    SUBSCRIPT,
}
