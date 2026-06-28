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
        assertTrue(result[0] is ContentBlock.BlockQuote)
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
        
        assertEquals(3, result.size)
        assertEquals("Text before", result[0].text.text)
        assertTrue(result[1] is ContentBlock.Image)
        assertEquals("https://example.com/nested.png", (result[1] as ContentBlock.Image).url)
        assertEquals("Text after", result[2].text.text)
    }

    @Test
    fun `parse full html document`() {
        val html = "<!DOCTYPE html><html><head><title>Title</title></head><body><p>Test Paragraph</p></body></html>"
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
        assertEquals(2, result.size)
        assertTrue(result[0] is ContentBlock.Image)
        assertEquals("Text after image", result[1].text.text)
    }

    @Test
    fun `parse complex mixed content`() {
        val html = "<div>Mixed <span>inline</span> and <p>block</p> content</div>"
        val result = HtmlParser.parse(html)
        // Expecting "Mixed inline and" and "block" and "content"
        // Actually, the current parser logic with flushInline in walk:
        // 1. "Mixed inline and" (flushed when <p> is encountered)
        // 2. "block" (processed by <p> handler)
        // 3. "content" (flushed at the end of <div>)
        assertEquals(3, result.size)
        assertEquals("Mixed inline and", result[0].text.text)
        assertEquals("block", result[1].text.text)
        assertEquals("content", result[2].text.text)
    }
}
