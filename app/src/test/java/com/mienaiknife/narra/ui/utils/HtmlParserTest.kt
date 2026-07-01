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

import com.mienaiknife.narra.ui.models.ContentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlParserTest {
    @Test
    fun `parse empty html returns empty list`() {
        val result = HtmlParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse simple paragraph`() {
        val html = "<p>Hello World</p>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertTrue(result[0] is ContentBlock.Paragraph)
        assertEquals("Hello World", result[0].text.text)
    }

    @Test
    fun `parse deeply nested tags`() {
        val html = "<div><div><div><p>Deeply nested</p></div></div></div>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertEquals("Deeply nested", result[0].text.text)
    }

    @Test
    fun `parse malformed html handles unclosed tags`() {
        val html = "<p>Unclosed paragraph"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertEquals("Unclosed paragraph", result[0].text.text)
    }

    @Test
    fun `parse ignores script and style tags`() {
        val html =
            """
            <p>Visible text</p>
            <script>alert('hidden');</script>
            <style>body { color: red; }</style>
            """.trimIndent()
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertEquals("Visible text", result[0].text.text)
    }

    @Test
    fun `parse blockquote`() {
        val html = "<blockquote>Quote text</blockquote>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertTrue(result[0] is ContentBlock.BlockQuote || result[0] is ContentBlock.Paragraph)
        assertEquals("Quote text", result[0].text.text)
    }

    @Test
    fun `parse headings`() {
        val html = "<h1>Heading 1</h1><h2>Heading 2</h2>"
        val result = HtmlParser.parse(html)
        assertEquals(2, result.size)
        assertTrue(result[0] is ContentBlock.Heading)
        assertEquals("Heading 1", result[0].text.text)
        assertEquals(1, (result[0] as ContentBlock.Heading).level)

        assertTrue(result[1] is ContentBlock.Heading)
        assertEquals("Heading 2", result[1].text.text)
        assertEquals(2, (result[1] as ContentBlock.Heading).level)
    }

    @Test
    fun `parse images with alt text`() {
        val html = "<img src=\"https://example.com/image.png\" alt=\"Description\">"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertTrue(result[0] is ContentBlock.Image)
        assertEquals("https://example.com/image.png", (result[0] as ContentBlock.Image).url)
        assertEquals("Description", (result[0] as ContentBlock.Image).altText)
    }

    @Test
    fun `parse images nested in paragraphs or spans`() {
        val html = "<p>Text before <span><img src=\"https://example.com/nested.png\" alt=\"Nested\"></span> Text after</p>"
        val result = HtmlParser.parse(html)
        
        // Output might vary based on how flushInline triggers
        assertTrue(result.any { it is ContentBlock.Image })
        assertTrue(result.any { it.text.text.contains("Text before") })
        assertTrue(result.any { it.text.text.contains("Text after") })
    }

    @Test
    fun `parse html snippet`() {
        val html = "<p>Test Paragraph</p>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertEquals("Test Paragraph", result[0].text.text)
    }

    @Test
    fun `parse nested divs with text`() {
        val html = "<div><div>Text in nested div</div></div>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertEquals("Text in nested div", result[0].text.text)
    }

    @Test
    fun `parse div as paragraph`() {
        val html = "<div>Paragraph 1</div><div>Paragraph 2</div>"
        val result = HtmlParser.parse(html)
        assertEquals(2, result.size)
        assertEquals("Paragraph 1", result[0].text.text)
        assertEquals("Paragraph 2", result[1].text.text)
    }

    @Test
    fun `parse p with image then text`() {
        val html = "<p><img src=\"test.png\" alt=\"Image\">Text after image</p>"
        val result = HtmlParser.parse(html)
        assertTrue(result.any { it is ContentBlock.Image })
        assertTrue(result.any { it.text.text.contains("Text after image") })
    }

    @Test
    fun `parse complex mixed content`() {
        val html = "<div>Mixed <span>inline</span> and <p>block</p> content</div>"
        val result = HtmlParser.parse(html)
        println("Result size: ${result.size}")
        result.forEachIndexed { i, b -> println("Block $i: '${b.text.text}'") }
        assertTrue(result.size >= 2)
        assertTrue(result.any { it.text.text.contains("Mixed inline and") })
        assertTrue(result.any { it.text.text.contains("block") })
    }

    @Test
    fun `parse very long paragraph splits it`() {
        val longText = "Sentence one. ".repeat(300) // ~4200 chars
        val html = "<p>$longText</p>"
        val result = HtmlParser.parse(html)

        assertTrue(result.size >= 2)
        assertTrue(result[0].text.length <= 3000)
        assertEquals(longText.trim(), result.joinToString("") { it.text.text }.trim())
    }

    @Test
    fun `parse table`() {
        val html = """
            <table>
                <tr><th>Header 1</th><th>Header 2</th></tr>
                <tr><td>Cell 1</td><td>Cell 2</td></tr>
            </table>
        """.trimIndent()
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        assertTrue(result[0] is ContentBlock.Table)
        val table = result[0] as ContentBlock.Table
        assertEquals(2, table.rows.size)
        assertEquals(2, table.rows[0].size)
        assertTrue(table.rows[0][0].isHeader)
        assertEquals("Header 1", table.rows[0][0].text.text)
        assertEquals("Cell 1", table.rows[1][0].text.text)
        assertTrue(!table.rows[1][0].isHeader)
    }

    @Test
    fun `parse ordered and unordered lists`() {
        val html = """
            <ul><li>Item A</li><li>Item B</li></ul>
            <ol><li>Item 1</li><li>Item 2</li></ol>
        """.trimIndent()
        val result = HtmlParser.parse(html)
        println("List parsing result:")
        result.forEachIndexed { i, b -> println("Block $i: '${b.text.text}'") }
        assertEquals(4, result.size)
        assertEquals("• Item A", result[0].text.text)
        assertEquals("• Item B", result[1].text.text)
        assertEquals("1. Item 1", result[2].text.text)
        assertEquals("2. Item 2", result[3].text.text)
    }

    @Test
    fun `parse strikethrough`() {
        val html = "<p>Normal <del>deleted</del> <s>struck</s> <strike>old</strike></p>"
        val result = HtmlParser.parse(html)
        assertEquals(1, result.size)
        val text = result[0].text
        // "Normal " is 7 chars. "deleted" starts at 7.
        val delStyle = text.spanStyles.find { it.start == 7 }?.item
        assertEquals(androidx.compose.ui.text.style.TextDecoration.LineThrough, delStyle?.textDecoration)
    }

    @Test
    fun `reproduce nested image in button and div`() {
        val html = """
            <div class="imageBlock">
                <button>
                    <img src="https://example.com/image.png" alt="Nested Image">
                </button>
            </div>
        """.trimIndent()
        val result = HtmlParser.parse(html)
        assertTrue(result.any { it is ContentBlock.Image })
        val img = result.find { it is ContentBlock.Image } as ContentBlock.Image
        assertEquals("https://example.com/image.png", img.url)
        assertEquals("Nested Image", img.altText)
    }

    @Test
    fun `reproduce isolated text split by block element`() {
        // This simulates the "Who" isolation issue where a block element (like an image div)
        // splits what should be a continuous flow of text if it was inlined.
        val html = """
            <div>
                <span>"Who</span>
                <div class="imageBlock"><img src="test.png"></div>
                <span>is this guy?"</span>
            </div>
        """.trimIndent()
        val result = HtmlParser.parse(html)
        
        assertTrue(result.any { it is ContentBlock.Image })
        assertTrue(result.any { it.text.text.contains("\"Who") })
        assertTrue(result.any { it.text.text.contains("is this guy?\"") })
    }

    @Test
    fun `parse handles double br inside span correctly`() {
        val html = "<span>First paragraph.<br/><br/>Second paragraph.</span>"
        val result = HtmlParser.parse(html)
        assertEquals(2, result.size)
        assertEquals("First paragraph.", result[0].text.text)
        assertEquals("Second paragraph.", result[1].text.text)
    }

    @Test
    fun `parse resolves relative image urls`() {
        val html = "<img src=\"/api/atproto_images/test.png\">"
        val result = HtmlParser.parse(html, "https://example.com")
        assertEquals(1, result.size)
        assertTrue(result[0] is ContentBlock.Image)
        assertEquals("https://example.com/api/atproto_images/test.png", (result[0] as ContentBlock.Image).url)
    }
}
