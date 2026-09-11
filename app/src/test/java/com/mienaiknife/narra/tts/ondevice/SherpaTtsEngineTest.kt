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

        val boundaries = SherpaTtsEngine.estimateWordBoundaries(text, totalSamples, null)

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
        val boundaries = SherpaTtsEngine.estimateWordBoundaries("", 1000, null)
        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun testWhitespaceOnly() {
        val boundaries = SherpaTtsEngine.estimateWordBoundaries("   ", 1000, null)
        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun testPartialEstimationDoesNotCompress() {
        val text = "The quick brown fox"
        val projectedTotal = 10000

        // Without full samples the estimator maps the whole projected duration across the text,
        // so the last word should end near the projected total rather than being compressed.
        val boundaries = SherpaTtsEngine.estimateWordBoundaries(text, projectedTotal, null)

        assertEquals(4, boundaries.size)
        assertTrue(
            "Last word should end near projected total, but was ${boundaries.last().endSample}",
            boundaries.last().endSample > 8000,
        )
    }

    @Test
    fun testSpeechBoundsTrimSilence() {
        // One second of (near) silence with a burst of speech in the middle.
        val samples = FloatArray(1000)
        for (i in 400 until 600) {
            samples[i] = 1.0f
        }

        val bounds = SherpaTtsEngine.detectSpeechBounds(samples)
        assertEquals(400, bounds.first)
        assertEquals(600, bounds.second)
    }

    @Test
    fun testCharWeightPunctuation() {
        assertEquals(3.0f, SherpaTtsEngine.getCharWeight('!'))
        assertEquals(2.0f, SherpaTtsEngine.getCharWeight(','))
        assertEquals(1.2f, SherpaTtsEngine.getCharWeight(' '))
        assertEquals(0.1f, SherpaTtsEngine.getCharWeight('('))
        assertEquals(1.0f, SherpaTtsEngine.getCharWeight('a'))
    }
}
