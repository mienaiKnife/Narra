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

sealed class ContentBlock {
    abstract val text: RichText

    data class Paragraph(
        override val text: RichText,
    ) : ContentBlock()

    data class BlockQuote(
        override val text: RichText,
    ) : ContentBlock()

    data class Heading(
        override val text: RichText,
        val level: Int,
    ) : ContentBlock()

    data class Image(
        val url: String,
        val altText: String?,
    ) : ContentBlock() {
        override val text: RichText = RichText(altText ?: "")
    }

    data object HorizontalRule : ContentBlock() {
        override val text: RichText = RichText("")
    }

    data class Table(
        val rows: List<List<Cell>>,
    ) : ContentBlock() {
        override val text: RichText = RichText(
            buildString {
                rows.forEach { row ->
                    row.forEach { cell ->
                        append(cell.text.text)
                        append(" ")
                    }
                    append("\n")
                }
            },
        )

        data class Cell(
            val text: RichText,
            val isHeader: Boolean = false,
        )
    }
}
