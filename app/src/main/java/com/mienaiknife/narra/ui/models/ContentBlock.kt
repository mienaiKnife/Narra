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
package com.mienaiknife.narra.ui.models

import androidx.compose.ui.text.AnnotatedString

sealed class ContentBlock {
    abstract val text: AnnotatedString

    data class Paragraph(
        override val text: AnnotatedString,
    ) : ContentBlock()

    data class BlockQuote(
        override val text: AnnotatedString,
    ) : ContentBlock()

    data class Heading(
        override val text: AnnotatedString,
        val level: Int,
    ) : ContentBlock()

    data class Image(
        val url: String,
        val altText: String?,
    ) : ContentBlock() {
        override val text: AnnotatedString = AnnotatedString(altText ?: "")
    }

    data object HorizontalRule : ContentBlock() {
        override val text: AnnotatedString = AnnotatedString("")
    }

    data class Table(
        val rows: List<List<Cell>>,
    ) : ContentBlock() {
        override val text: AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
            rows.forEach { row ->
                row.forEach { cell ->
                    append(cell.text)
                    append(" ")
                }
                append("\n")
            }
        }

        data class Cell(
            val text: AnnotatedString,
            val isHeader: Boolean = false,
        )
    }
}
