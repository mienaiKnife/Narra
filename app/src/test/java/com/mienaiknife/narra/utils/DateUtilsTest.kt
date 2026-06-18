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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {
    @Test
    fun `formatPublishedDate handles null or blank`() {
        assertNull(DateUtils.formatPublishedDate(null))
        assertNull(DateUtils.formatPublishedDate(""))
        assertNull(DateUtils.formatPublishedDate("  "))
    }

    @Test
    fun `formatPublishedDate handles current year`() {
        val currentYear = LocalDate.now().year
        val dateString = "$currentYear-05-21"
        val formatted = DateUtils.formatPublishedDate(dateString)
        assertEquals("May 21", formatted)
    }

    @Test
    fun `formatPublishedDate handles different year`() {
        val dateString = "2023-05-21"
        val formatted = DateUtils.formatPublishedDate(dateString)
        assertEquals("May 21, 2023", formatted)
    }

    @Test
    fun `parseToTimestamp handles various formats`() {
        // ISO Date Time
        val timestamp = DateUtils.parseToTimestamp("2025-05-21T10:00:00Z")
        assertTrue(timestamp!! > 0)

        // RFC 1123
        val timestampRfc = DateUtils.parseToTimestamp("Wed, 21 May 2025 10:00:00 GMT")
        assertEquals(timestamp, timestampRfc)

        // Simple date
        val timestampSimple = DateUtils.parseToTimestamp("2025-05-21")
        assertTrue(timestampSimple!! > 0)
    }

    @Test
    fun `formatElapsedTime formats correctly`() {
        // Less than an hour
        assertEquals("0:05", DateUtils.formatElapsedTime(5 * 1000))
        assertEquals("1:30", DateUtils.formatElapsedTime(90 * 1000))

        // More than an hour
        assertEquals("1:00:00", DateUtils.formatElapsedTime(3600 * 1000))
        assertEquals("1:05:01", DateUtils.formatElapsedTime((3600 + 301) * 1000))
    }

    @Test
    fun `estimateReadingTimeMs gives reasonable estimates`() {
        val text = "This is a test with six words."
        // "This", "is", "a", "test", "with", "six", "words." -> 7 words
        // 7 words * 300ms = 2100ms
        assertEquals(2100L, DateUtils.estimateReadingTimeMs(text))

        // Strips HTML
        val htmlText = "<p>This is <b>bold</b> text.</p>"
        // "<p>This is <b>bold</b> text.</p>" -> strips to "This is bold text."
        // "This", "is", "bold", "text." -> 4 words
        // 4 * 300 = 1200ms
        assertEquals(1200L, DateUtils.estimateReadingTimeMs(htmlText))

        // Coerces to at least 1s
        assertEquals(1000L, DateUtils.estimateReadingTimeMs("One"))
    }
}
