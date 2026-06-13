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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.mienaiknife.narra.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ToSpeakableTextTest {

    @Mock
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getString(R.string.reader_link_to)).thenReturn(" link to %s ")
    }

    @Test
    fun `simple text remains unchanged`() {
        val text = "Hello World"
        val annotatedString = AnnotatedString(text)
        val result = annotatedString.toSpeakableText(context)
        assertEquals(text, result)
        assertEquals(text.length, result.length)
    }

    @Test
    fun `footnotes are replaced by spaces preserving length`() {
        val annotatedString = buildAnnotatedString {
            append("Text")
            pushStringAnnotation("footnote", "1")
            append("[1]")
            pop()
            append(" more text")
        }
        val result = annotatedString.toSpeakableText(context)
        assertEquals("Text    more text", result)
        assertEquals(annotatedString.length, result.length)
    }

    @Test
    fun `long URL links are shortened and padded to preserve length`() {
        // "Check out https://example.com/very/long/path/to/page for more"
        // URL length: 43 chars
        // Simplified: " link to example.com " -> 21 chars
        // Result should have 22 spaces of padding
        val url = "https://example.com/very/long/path/to/page"
        val annotatedString = buildAnnotatedString {
            append("Check out ")
            pushStringAnnotation("link", url)
            append(url)
            pop()
            append(" for more")
        }
        
        val result = annotatedString.toSpeakableText(context)
        
        assertEquals(annotatedString.length, result.length)
        val expectedPart = " link to example.com                      "
        assertEquals(expectedPart, result.substring(10, 10 + url.length))
    }

    @Test
    fun `short URL links are truncated if summary is longer`() {
        // "See https://t.co"
        // URL length: 12 chars
        // Simplified: " link to t.co " -> 14 chars
        // Result should truncate simplified to 12 chars
        val url = "https://t.co"
        val annotatedString = buildAnnotatedString {
            append("See ")
            pushStringAnnotation("link", url)
            append(url)
            pop()
        }
        
        val result = annotatedString.toSpeakableText(context)
        
        assertEquals(annotatedString.length, result.length)
        // " link to t.co " truncated to 12 chars is " link to t.c"
        assertEquals(" link to t.c", result.substring(4))
    }

    @Test
    fun `non-URL links remain unchanged`() {
        val linkText = "Click here"
        val annotatedString = buildAnnotatedString {
            append("Please ")
            pushStringAnnotation("link", "https://example.com")
            append(linkText)
            pop()
        }
        
        val result = annotatedString.toSpeakableText(context)
        assertEquals(annotatedString.text, result)
        assertEquals(annotatedString.length, result.length)
    }

    @Test
    fun `multiple annotations are handled correctly`() {
        val url = "https://example.com/long"
        val annotatedString = buildAnnotatedString {
            append("Text")
            pushStringAnnotation("footnote", "1")
            append("[1]")
            pop()
            append(" and ")
            pushStringAnnotation("link", url)
            append(url)
            pop()
        }
        
        val result = annotatedString.toSpeakableText(context)
        
        assertEquals(annotatedString.length, result.length)
        assertEquals("Text   ", result.substring(0, 7))
        // " link to example.com " (21 chars) for url (24 chars) -> 3 spaces padding
        assertEquals(" link to example.com    ", result.substring(12))
    }
}
