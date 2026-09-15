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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakableTextTest {
    private fun identity(text: String) = SpeakableText(text, IntArray(text.length) { it })

    @Test
    fun `mapTtsToOriginal maps exclusive end to original length`() {
        val speakable = identity("Hello world")
        assertEquals(11, speakable.mapTtsToOriginal(11))
        assertEquals(11, speakable.mapTtsToOriginal(99))
    }

    @Test
    fun `mapTtsToOriginal maps transliterated exclusive end past the last entry`() {
        // "Æ" (1 original char) transliterated to "AE" (2 TTS chars), both mapping to index 0.
        val speakable = SpeakableText("AE", intArrayOf(0, 0))
        assertEquals(0, speakable.mapTtsToOriginal(0))
        assertEquals(1, speakable.mapTtsToOriginal(2))
    }

    @Test
    fun `expandToWordRange expands sub-word ranges to the whole hyphenated word`() {
        val speakable = identity("Ex-FTC boss")
        assertEquals(0 until 6, speakable.expandToWordRange(0, 2))
        assertEquals(0 until 6, speakable.expandToWordRange(3, 6))
        assertEquals(7 until 11, speakable.expandToWordRange(7, 11))
    }

    @Test
    fun `expandToWordRange expands numeric compounds`() {
        val speakable = identity("a 92-year-old precedent")
        assertEquals(2 until 13, speakable.expandToWordRange(2, 4))
        assertEquals(2 until 13, speakable.expandToWordRange(5, 9))
        assertEquals(2 until 13, speakable.expandToWordRange(10, 13))
    }

    @Test
    fun `expandToWordRange keeps whole-word ranges unchanged`() {
        val speakable = identity("Hello world")
        assertEquals(6 until 11, speakable.expandToWordRange(6, 11))
    }

    @Test
    fun `expandToWordRange preserves multi-word ranges like dates`() {
        // "On April 10, 2026 we ship" -> "April 10, 2026" spans [3, 17)
        val speakable = identity("On April 10, 2026 we ship")
        assertEquals(3 until 17, speakable.expandToWordRange(3, 17))
    }

    @Test
    fun `expandToWordRange preserves multi-word phrases`() {
        val speakable = identity("New York City is big")
        assertEquals(0 until 13, speakable.expandToWordRange(0, 13))
    }

    @Test
    fun `expandToWordRange never extends past trailing whitespace`() {
        // Reported range includes the space after the date but not the next word.
        val speakable = identity("April 10, 2026 next")
        assertEquals(0 until 15, speakable.expandToWordRange(0, 15))
    }

    @Test
    fun `expandToWordRange still widens a sub-word range inside a date`() {
        // "10" reported on its own inside "April 10, 2026" should widen to "10,".
        val speakable = identity("April 10, 2026 next")
        assertEquals(6 until 9, speakable.expandToWordRange(6, 8))
    }

    @Test
    fun `expandToWordRange skips leading whitespace`() {
        val speakable = identity("Hi there")
        assertEquals(3 until 8, speakable.expandToWordRange(2, 4))
    }

    @Test
    fun `expandToWordRange returns empty for an empty reported range`() {
        val speakable = identity("Hello world")
        assertTrue(speakable.expandToWordRange(0, 0).isEmpty())
    }

    @Test
    fun `expandToWordRange handles empty text`() {
        assertTrue(SpeakableText("").expandToWordRange(0, 0).isEmpty())
    }
}
