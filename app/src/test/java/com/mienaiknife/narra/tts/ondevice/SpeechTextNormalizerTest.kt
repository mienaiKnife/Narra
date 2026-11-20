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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextNormalizerTest {

    private fun normalized(input: String): String = SpeechTextNormalizer.normalize(input).text

    @Test
    fun `plain prose is unchanged`() {
        val text = "Mei or, the Models Engineering Index calculates language models' performance."
        assertEquals(text, normalized(text))
    }

    @Test
    fun `small integers become words`() {
        assertEquals("Scores changed by three points.", normalized("Scores changed by 3 points."))
        assertEquals("There are zero apples.", normalized("There are 0 apples."))
        assertEquals("He is nineteen.", normalized("He is 19."))
        assertEquals("twenty-one", normalized("21"))
    }

    @Test
    fun `four digit numbers and formatted numbers are cardinals`() {
        assertEquals("two thousand twenty-six", normalized("2026"))
        assertEquals("one thousand", normalized("1,000"))
        assertEquals("one million", normalized("1,000,000"))
        assertEquals("one hundred thousand", normalized("100,000"))
    }

    @Test
    fun `long unformatted digit strings are read digit by digit`() {
        assertEquals("one two zero three eight two", normalized("120382"))
        assertEquals("one one nine zero zero", normalized("11900"))
        assertEquals("forty-one", normalized("41"))
    }

    @Test
    fun `decimals use point`() {
        assertEquals("four point one", normalized("4.1"))
        assertEquals("three point one four", normalized("3.14"))
    }

    @Test
    fun `percentages and currency`() {
        assertEquals("five percent", normalized("5%"))
        assertEquals("five dollars", normalized("$5"))
        assertEquals("one dollar", normalized("$1"))
        assertEquals("two point five dollars", normalized("$2.50"))
        assertEquals("twelve percent", normalized("12%"))
    }

    @Test
    fun `ordinals`() {
        assertEquals("first", normalized("1st"))
        assertEquals("second", normalized("2nd"))
        assertEquals("third", normalized("3rd"))
        assertEquals("fourth", normalized("4th"))
        assertEquals("twelfth", normalized("12th"))
        assertEquals("twentieth", normalized("20th"))
        assertEquals("twenty-first", normalized("21st"))
        assertEquals("thirty-second", normalized("32nd"))
        assertEquals("one hundredth", normalized("100th"))
    }

    @Test
    fun `ampersands and bullets are speech friendly`() {
        assertEquals("Rock and Roll", normalized("Rock & Roll"))
        assertEquals(" CVE entry", normalized("• CVE entry"))
    }

    @Test
    fun `em dashes become pauses instead of being dropped`() {
        assertEquals("Mei, or, the Models Engineering Index", normalized("Mei—or, the Models Engineering Index"))
        assertEquals("a, b", normalized("a – b"))
    }

    @Test
    fun `ellipses collapse to a single period`() {
        assertEquals("Wait. what?", normalized("Wait... what?"))
        assertEquals("Wait. what?", normalized("Wait… what?"))
        assertEquals("Wait. what?", normalized("Wait.. what?"))
        assertEquals("Wait. what?", normalized("Wait. . . what?"))
        assertEquals("I don't know.", normalized("I don't know..."))
    }

    @Test
    fun `repeated sentence punctuation collapses to one mark`() {
        assertEquals("Wait? what?", normalized("Wait?! what?"))
        assertEquals("Really! I mean it.", normalized("Really!!! I mean it."))
        assertEquals("What? yes.", normalized("What?? yes."))
        assertEquals("Sure! What?", normalized("Sure!! What?!"))
    }

    @Test
    fun `single periods and abbreviations are preserved`() {
        assertEquals("U.S. policy.", normalized("U.S. policy."))
        assertEquals("three point one four", normalized("3.14"))
    }

    @Test
    fun `typographic apostrophes keep contractions intact`() {
        // The pronunciation lexicon keys contractions with U+0027; a curly apostrophe makes the
        // native frontend split them ("I’m" -> "I" + "m", heard as "I em").
        assertEquals(
            "I'm sure it's John's.",
            normalized("I’m sure it’s John’s."),
        )
        assertEquals(
            "I've and I'll and we're and don't",
            normalized("I’ve and I’ll and we’re and don’t"),
        )
        assertEquals("'quoted'", normalized("‘quoted’"))
    }

    @Test
    fun `apostrophe normalization keeps source offsets`() {
        val original = "I’ve seen it."
        val speech = SpeechTextNormalizer.normalize(original)

        assertEquals("I've seen it.", speech.text)
        val range = speech.sourceRange(0, 4)!!
        assertEquals("I’ve", original.substring(range.first, range.last + 1))
    }

    @Test
    fun `identifiers keep their letters separate from numbers`() {
        assertEquals("v zero point one", normalized("v0.1"))
        assertEquals("H two O", normalized("H2O"))
        assertEquals("mp three", normalized("mp3"))
    }

    @Test
    fun `numeric ranges read as to`() {
        assertEquals("one thousand nine hundred ninety to two thousand", normalized("1990-2000"))
        assertEquals("two to three", normalized("2-3"))
    }

    @Test
    fun `hyphenated identifiers are separated from their number`() {
        assertEquals("CVE two thousand twenty-six", normalized("CVE-2026"))
        assertEquals("twenty-six", normalized("twenty-six"))
    }

    @Test
    fun `negative numbers keep their sign`() {
        assertEquals("minus five", normalized("-5"))
        assertEquals("a minus five variance", normalized("a -5 variance"))
    }

    @Test
    fun `source mapping points back into the original text`() {
        val original = "There are 2026 models in 4.1 seconds."
        val speech = SpeechTextNormalizer.normalize(original)

        assertEquals(
            "There are two thousand twenty-six models in four point one seconds.",
            speech.text,
        )

        // "two" is the first synthesized word of the number, so it maps inside "2026".
        val twoStart = speech.text.indexOf("two")
        val twoRange = speech.sourceRange(twoStart, twoStart + 3)
        assertNotNull(twoRange)
        assertTrue(twoRange!!.first >= original.indexOf("2026"))
        assertTrue(twoRange.last < original.indexOf("2026") + 4)

        // Words that were not rewritten map to the same characters in the original.
        val modelsStart = speech.text.indexOf("models")
        val modelsRange = speech.sourceRange(modelsStart, modelsStart + 6)!!
        assertEquals(original.substring(modelsRange.first, modelsRange.last + 1), "models")
    }

    @Test
    fun `mapped ranges never exceed the original length`() {
        val original = "$1,000,000 & 120382 • 21st — 2026-11-19"
        val speech = SpeechTextNormalizer.normalize(original)

        for (start in speech.text.indices) {
            for (length in 1..minOf(12, speech.text.length - start)) {
                val range = speech.sourceRange(start, start + length) ?: continue
                assertTrue("range starts before 0", range.first >= 0)
                assertTrue(
                    "range end ${range.last + 1} exceeds ${original.length}",
                    range.last + 1 <= original.length,
                )
            }
        }
    }

    @Test
    fun `empty and whitespace input`() {
        assertEquals("", normalized(""))
        assertEquals("   ", normalized("   "))
        val empty = SpeechTextNormalizer.normalize("")
        assertTrue(empty.text.isEmpty())
        assertEquals(null, empty.sourceRange(0, 1))
    }
}
