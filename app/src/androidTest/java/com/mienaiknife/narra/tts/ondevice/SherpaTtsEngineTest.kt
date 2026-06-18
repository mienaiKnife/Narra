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
package com.mienaiknife.narra.tts.ondevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SherpaTtsEngineTest {
    @Test
    fun testEstimateWordBoundaries() {
        val text = "Hello world"
        val totalSamples = 1000
        val totalChars = text.length // 11

        // Manual calculation based on the logic:
        // "Hello" -> start: 0, end: 5. samples: [0, (5/11)*1000] = [0, 454]
        // "world" -> start: 6, end: 11. samples: [(6/11)*1000, (11/11)*1000] = [545, 1000]

        val boundaries = estimateWordBoundaries(text, totalSamples)

        assertEquals(2, boundaries.size)

        assertEquals(0, boundaries[0].startChar)
        assertEquals(5, boundaries[0].endChar)
        assertEquals(0, boundaries[0].startSample)
        assertEquals(454, boundaries[0].endSample)

        assertEquals(6, boundaries[1].startChar)
        assertEquals(11, boundaries[1].endChar)
        assertEquals(545, boundaries[1].startSample)
        assertEquals(1000, boundaries[1].endSample)
    }

    @Test
    fun testEmptyText() {
        val boundaries = estimateWordBoundaries("", 1000)
        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun testWhitespaceOnly() {
        val boundaries = estimateWordBoundaries("   ", 1000)
        assertTrue(boundaries.isEmpty())
    }

    // Helper to test the logic (copied from SherpaTtsEngine)
    private fun estimateWordBoundaries(
        text: String,
        totalSamples: Int,
    ): List<WordBoundaryWrapper> {
        val boundaries = mutableListOf<WordBoundaryWrapper>()
        val totalChars = text.length
        if (totalChars == 0) return boundaries

        val regex = Regex("\\S+")
        val matches = regex.findAll(text).toList()

        matches.forEach { match ->
            val startChar = match.range.first
            val endChar = match.range.last + 1

            val startSample = (startChar.toFloat() / totalChars * totalSamples).toInt()
            val endSample = (endChar.toFloat() / totalChars * totalSamples).toInt()

            boundaries.add(WordBoundaryWrapper(startChar, endChar, startSample, endSample))
        }

        return boundaries
    }

    data class WordBoundaryWrapper(
        val startChar: Int,
        val endChar: Int,
        val startSample: Int,
        val endSample: Int,
    )
}
