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
package com.mienaiknife.narra.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HtmlUtilsTest {

    @Test
    fun `decodeHtmlEntities handles null`() {
        assertNull(HtmlUtils.decodeHtmlEntities(null))
    }

    @Test
    fun `decodeHtmlEntities handles empty string`() {
        assertEquals("", HtmlUtils.decodeHtmlEntities(""))
    }

    @Test
    fun `decodeHtmlEntities handles string without entities`() {
        val input = "Hello World"
        assertEquals(input, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun `decodeHtmlEntities decodes double quotes`() {
        val input = "&quot;Lawyering Without Law&quot; - Ep. 5"
        val expected = "\"Lawyering Without Law\" - Ep. 5"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun `decodeHtmlEntities decodes ampersand`() {
        val input = "Fish &amp; Chips"
        val expected = "Fish & Chips"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun `decodeHtmlEntities decodes angle brackets`() {
        val input = "&lt;b&gt;Bold&lt;/b&gt;"
        val expected = "<b>Bold</b>"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun `decodeHtmlEntities decodes numeric entities`() {
        val input = "&#8216;Single Quotes&#8217;"
        val expected = "‘Single Quotes’"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun `decodeHtmlEntities decodes mixed content`() {
        val input = "Text with &quot;quotes&quot; and &amp; ampersands."
        val expected = "Text with \"quotes\" and & ampersands."
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }
}
