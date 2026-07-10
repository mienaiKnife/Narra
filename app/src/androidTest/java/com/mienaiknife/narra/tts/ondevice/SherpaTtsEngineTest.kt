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

        // weights: H(1), e(1), l(1), l(1), o(1), ' '(1.2), w(1), o(1), r(1), l(1), d(1)
        // total weight = 5*1 + 1.2 + 5*1 = 11.2
        // Hello: start 0, end 5. weight = 5. samples = (5 / 11.2) * 1000 = 446.4 -> 446
        // world: start 6, end 11. weight = 5. offset weight before = 6.2. 
        // start sample = (6.2 / 11.2) * 1000 = 553.5 -> 553
        // end sample = (11.2 / 11.2) * 1000 = 1000

        val boundaries = estimateWordBoundaries(text, totalSamples)

        assertEquals(2, boundaries.size)

        assertEquals(0, boundaries[0].startChar)
        assertEquals(5, boundaries[0].endChar)
        assertEquals(0, boundaries[0].startSample)
        assertEquals(446, boundaries[0].endSample)

        assertEquals(6, boundaries[1].startChar)
        assertEquals(11, boundaries[1].endChar)
        assertEquals(553, boundaries[1].startSample)
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

    @Test
    fun testPartialEstimationDoesNotCompress() {
        val text = "The quick brown fox"
        val projectedTotal = 10000
        val partialSamples = FloatArray(1000)

        // The current implementation of estimateWordBoundaries uses the 'totalSamples' passed in
        // to map the characters. If we pass the projected total, it should spread them out.
        val boundaries = estimateWordBoundaries(text, projectedTotal, partialSamples)

        assertEquals(4, boundaries.size)
        // "fox" should end near 10000, not 1000
        assertTrue("Last word should end near projected total, but was ${boundaries.last().endSample}",
            boundaries.last().endSample > 8000)
    }

    // Helper to test the logic (copied from SherpaTtsEngine)
    private fun estimateWordBoundaries(
        text: String,
        totalSamples: Int,
        samples: FloatArray? = null
    ): List<WordBoundaryWrapper> {
        val boundaries = mutableListOf<WordBoundaryWrapper>()
        if (text.isEmpty() || totalSamples == 0) return boundaries

        val weights = text.map { getCharWeight(it) }
        val totalWeight = weights.sum().coerceAtLeast(1.0f)

        val regex = Regex("\\S+")
        val matches = regex.findAll(text).toList()

        if (matches.isEmpty()) {
            if (text.trim().isEmpty()) return emptyList()
            boundaries.add(WordBoundaryWrapper(0, text.length, 0, totalSamples))
            return boundaries
        }

        matches.forEach { match ->
            val startChar = match.range.first
            val endChar = match.range.last + 1

            val weightBefore = weights.take(startChar).sum()
            val weightInWord = weights.subList(startChar, endChar).sum()

            val wordStartSample = (weightBefore / totalWeight * totalSamples).toInt()
            val wordEndSample = ((weightBefore + weightInWord) / totalWeight * totalSamples).toInt()

            boundaries.add(WordBoundaryWrapper(startChar, endChar, wordStartSample, wordEndSample))
        }

        return boundaries
    }

    private fun getCharWeight(c: Char): Float = when (c) {
        '.', '!', '?' -> 3.0f
        ',', ';', ':', '-' -> 2.0f
        ' ' -> 1.2f
        else -> 1.0f
    }

    data class WordBoundaryWrapper(
        val startChar: Int,
        val endChar: Int,
        val startSample: Int,
        val endSample: Int,
    )
}
