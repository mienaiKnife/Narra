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

import com.mienaiknife.narra.domain.models.TtsModelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SherpaTtsEngineTest {
    @Test
    fun testLexiconIsRequiredForPhonemeBasedModels() {
        listOf(
            TtsModelType.VITS,
            TtsModelType.MATCHA,
            TtsModelType.KOKORO,
            TtsModelType.ZIPVOICE,
            TtsModelType.KITTEN,
        ).forEach { type ->
            assertTrue("$type should require a lexicon", SherpaTtsEngine.requiresLexicon(type))
        }
    }

    @Test
    fun testLexiconIsNotRequiredForSelfContainedFrontends() {
        assertFalse(SherpaTtsEngine.requiresLexicon(TtsModelType.POCKET))
        assertFalse(SherpaTtsEngine.requiresLexicon(TtsModelType.SUPERTONIC))
    }

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

    @Test
    fun testSplitIntoSentences() {
        val text = "First sentence. Second one! Third? Last"
        val sentences = SherpaTtsEngine.splitIntoSentences(text, minWords = 1).map { text.substring(it) }

        assertEquals(listOf("First sentence.", "Second one!", "Third?", "Last"), sentences)
    }

    @Test
    fun testSplitIntoSentencesTrimsWhitespaceAndKeepsOffsets() {
        val text = "  One.   Two.  "
        val sentences = SherpaTtsEngine.splitIntoSentences(text, minWords = 1)

        assertEquals(2, sentences.size)
        assertEquals("One.", text.substring(sentences[0].first, sentences[0].last + 1))
        assertEquals("Two.", text.substring(sentences[1].first, sentences[1].last + 1))
    }

    @Test
    fun testSplitIntoSentencesKeepsClosingQuoteWithSentence() {
        val text = "He said \"stop.\" Then left."
        val sentences = SherpaTtsEngine.splitIntoSentences(text, minWords = 1).map { text.substring(it) }

        assertEquals(listOf("He said \"stop.\"", "Then left."), sentences)
    }

    @Test
    fun testSplitIntoSentencesDoesNotSplitDecimals() {
        val text = "Pi is 3.14 today."
        val sentences = SherpaTtsEngine.splitIntoSentences(text, minWords = 1).map { text.substring(it) }

        assertEquals(listOf("Pi is 3.14 today."), sentences)
    }

    @Test
    fun testSplitIntoSentencesHandlesCjkPunctuation() {
        val text = "你好。再见！"
        val sentences = SherpaTtsEngine.splitIntoSentences(text, minWords = 1).map { text.substring(it) }

        assertEquals(listOf("你好。", "再见！"), sentences)
    }

    @Test
    fun testSplitIntoSentencesGroupsShortSentences() {
        // "Dr." must not be synthesized as a sentence of its own.
        val text = "Dr. Smith arrived. He was late."
        val sentences = SherpaTtsEngine.splitIntoSentences(text).map { text.substring(it) }

        assertEquals(listOf("Dr. Smith arrived. He was late."), sentences)
    }

    @Test
    fun testSplitIntoSentencesSplitsOnceEnoughWordsAccumulate() {
        val text = "This first sentence has six words. This second sentence also has six words."
        val sentences = SherpaTtsEngine.splitIntoSentences(text).map { text.substring(it) }

        assertEquals(
            listOf(
                "This first sentence has six words.",
                "This second sentence also has six words.",
            ),
            sentences,
        )
    }

    @Test
    fun testFramesPlayedSinceHandlesCounterWrap() {
        assertEquals(0, SherpaTtsEngine.framesPlayedSince(100, 100))
        assertEquals(400, SherpaTtsEngine.framesPlayedSince(500, 100))

        // The audio track's head is a 32-bit counter and wraps back to zero.
        val nearWrap = 0xFFFF_FF00L
        assertEquals(356, SherpaTtsEngine.framesPlayedSince(100, nearWrap))
    }

    @Test
    fun testComputeStartThresholdFramesStartsQuicklyForShortClips() {
        val sampleRate = 24_000
        val capacityFrames = sampleRate * 15
        val threshold = SherpaTtsEngine.computeStartThresholdFrames(
            sampleRate = sampleRate,
            minBufferBytes = 1920 * Float.SIZE_BYTES,
            capacityFrames = capacityFrames,
        )

        assertEquals(1200, threshold)
    }

    @Test
    fun testComputeStartThresholdFramesRespectsSmallerMinBufferAndCapacity() {
        assertEquals(
            256,
            SherpaTtsEngine.computeStartThresholdFrames(
                sampleRate = 24_000,
                minBufferBytes = 256 * Float.SIZE_BYTES,
                capacityFrames = 360_000,
            ),
        )
        assertEquals(
            500,
            SherpaTtsEngine.computeStartThresholdFrames(
                sampleRate = 24_000,
                minBufferBytes = -1,
                capacityFrames = 500,
            ),
        )
    }
}
