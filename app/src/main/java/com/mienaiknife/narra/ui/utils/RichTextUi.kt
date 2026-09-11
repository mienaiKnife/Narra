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
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mienaiknife.narra.domain.models.RichText
import com.mienaiknife.narra.domain.models.RichTextSpanStyle

/**
 * Maps the Compose-free [RichText] domain model to an [AnnotatedString] for rendering.
 */
fun RichText.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        span.link?.let { addStringAnnotation("link", it, span.start, span.end) }
        if (span.isFootnote) {
            addStringAnnotation("footnote", "true", span.start, span.end)
        }
        span.style?.let { addStyle(it.toSpanStyle(), span.start, span.end) }
    }
}

private fun RichTextSpanStyle.toSpanStyle(): SpanStyle = when (this) {
    RichTextSpanStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    RichTextSpanStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    RichTextSpanStyle.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    RichTextSpanStyle.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    RichTextSpanStyle.CODE -> SpanStyle(background = Color.LightGray.copy(alpha = 0.3f))
    RichTextSpanStyle.SUPERSCRIPT -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp)
    RichTextSpanStyle.SUBSCRIPT -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp)
}
