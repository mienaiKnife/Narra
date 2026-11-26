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
package com.mienaiknife.narra.domain.pronunciation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationValidatorTest {
    private fun valid(word: String, arpabet: String): PronunciationValidator.Result.Valid {
        val result = PronunciationValidator.validate(word, arpabet)
        assertTrue(
            "expected a valid result for \"$word\" but was $result",
            result is PronunciationValidator.Result.Valid,
        )
        return result as PronunciationValidator.Result.Valid
    }

    private fun invalid(word: String, arpabet: String): PronunciationValidator.Result.Invalid {
        val result = PronunciationValidator.validate(word, arpabet)
        assertTrue(
            "expected an invalid result for \"$word\" but was $result",
            result is PronunciationValidator.Result.Invalid,
        )
        return result as PronunciationValidator.Result.Invalid
    }

    @Test
    fun `valid entry returns the display word normalized key and IPA`() {
        val result = valid("Hello", "HH AH0 L OW1")

        assertEquals("Hello", result.word)
        assertEquals("hello", result.normalizedWord)
        assertEquals("h ə l ˈ o ʊ", result.ipa)
        assertEquals(PronunciationValidator.normalizeWord("Hello"), result.normalizedWord)
    }

    @Test
    fun `normalization is case insensitive and collapses whitespace`() {
        val result = valid("  New\tYork\nCity  ", "N UW1 Y AO2 R K S IH1 T IY0")

        assertEquals("New York City", result.word)
        assertEquals("new york city", result.normalizedWord)
        assertEquals("hello", valid("HELLO", "HH AH0 L OW1").normalizedWord)
    }

    @Test
    fun `curly apostrophes normalize to ASCII`() {
        val curly = valid("O’Brien", "OW0 B R AY1 AH0 N")
        val ascii = valid("O'Brien", "OW0 B R AY1 AH0 N")

        assertEquals("O'Brien", curly.word)
        assertEquals("o'brien", curly.normalizedWord)
        assertEquals(ascii.word, curly.word)
        assertEquals(ascii.normalizedWord, curly.normalizedWord)
        assertEquals("o'brien", PronunciationValidator.normalizeWord("O’Brien"))
    }

    @Test
    fun `hyphenated words and multi word phrases are accepted`() {
        val hyphen = valid("well-known", "W EH1 L N OW1 N")
        assertEquals("well-known", hyphen.word)
        assertEquals("well-known", hyphen.normalizedWord)

        val phrase = valid("Baton  Rouge", "B AE1 T AH0 N R UW1 ZH")
        assertEquals("Baton Rouge", phrase.word)
        assertEquals("baton rouge", phrase.normalizedWord)
    }

    @Test
    fun `accented letters are accepted and NFC normalized`() {
        val composed = valid("Café", "K AE0 F EY1")
        val decomposed = valid("Cafe\u0301", "K AE0 F EY1")

        // Even the display word is composed, so both spellings of the input look identical.
        assertEquals("Café", composed.word)
        assertEquals("Café", decomposed.word)
        assertEquals("café", composed.normalizedWord)
        assertEquals("café", decomposed.normalizedWord)
        assertEquals(composed.normalizedWord, decomposed.normalizedWord)
        assertEquals("café", PronunciationValidator.normalizeWord("Cafe\u0301"))
    }

    @Test
    fun `empty and blank words are rejected`() {
        for (word in listOf("", " ", "   ", "\t\n", "\r\n")) {
            assertEquals(word, PronunciationValidator.Error.EMPTY_WORD, invalid(word, "K AE1 T").error)
        }
    }

    @Test
    fun `words longer than MAX_WORD_LENGTH are rejected`() {
        val atLimit = "a".repeat(PronunciationValidator.MAX_WORD_LENGTH)
        assertEquals(atLimit, valid(atLimit, "K AE1 T").normalizedWord)

        val overLimit = "a".repeat(PronunciationValidator.MAX_WORD_LENGTH + 1)
        assertEquals(
            PronunciationValidator.Error.WORD_TOO_LONG,
            invalid(overLimit, "K AE1 T").error,
        )
    }

    @Test
    fun `letters mixed with digits or punctuation are rejected`() {
        val words = listOf(
            "word123",
            "hello!",
            "hello?",
            "hello.",
            "hello,",
            "hello;",
            "a||b",
            "a#b",
            "a&b",
            "a/b",
            "a@b",
            "a+b",
            "a(b)",
        )

        for (word in words) {
            assertEquals(
                word,
                PronunciationValidator.Error.INVALID_WORD_CHARACTER,
                invalid(word, "K AE1 T").error,
            )
        }
    }

    @Test
    fun `words without any letters are rejected as invalid characters`() {
        for (word in listOf("123", "0", "42", "||", "#", "&", "/", "@", "...", "-", "'")) {
            assertEquals(
                word,
                PronunciationValidator.Error.INVALID_WORD_CHARACTER,
                invalid(word, "K AE1 T").error,
            )
        }
    }

    @Test
    fun `empty and blank pronunciations are rejected`() {
        for (arpabet in listOf("", " ", "   ", "\t\n", "- , / _")) {
            assertEquals(
                arpabet,
                PronunciationValidator.Error.EMPTY_PRONUNCIATION,
                invalid("hello", arpabet).error,
            )
        }
    }

    @Test
    fun `invalid pronunciation errors propagate with the offending token`() {
        val unknown = invalid("hello", "HH AH0 XX")
        assertEquals(PronunciationValidator.Error.UNKNOWN_SYMBOL, unknown.error)
        assertEquals("XX", unknown.detail)

        val stress = invalid("hello", "HH B1")
        assertEquals(PronunciationValidator.Error.STRESS_ON_CONSONANT, stress.error)
        assertEquals("B1", stress.detail)

        val tooMany = invalid("hello", List(ArpabetToIpa.MAX_TOKENS + 1) { "K" }.joinToString(" "))
        assertEquals(PronunciationValidator.Error.TOO_MANY_SYMBOLS, tooMany.error)
        assertNull(tooMany.detail)
    }

    @Test
    fun `normalizeWord returns the lexicon key for valid words`() {
        assertEquals("hello", PronunciationValidator.normalizeWord("  HELLO  "))
        assertEquals("new york", PronunciationValidator.normalizeWord("New\tYork"))
        assertEquals("o'brien", PronunciationValidator.normalizeWord("O’Brien"))
        assertEquals("o'brien", PronunciationValidator.normalizeWord("O'Brien"))
        assertEquals("well-known", PronunciationValidator.normalizeWord("Well-Known"))
        assertEquals("café", PronunciationValidator.normalizeWord("Café"))
        assertEquals("café", PronunciationValidator.normalizeWord("Cafe\u0301"))
    }

    @Test
    fun `normalizeWord returns null for invalid words`() {
        val words = listOf(
            "",
            "   ",
            "\t\n",
            "hello!",
            "word123",
            "||",
            "#",
            "a/b",
            "@",
            "a".repeat(PronunciationValidator.MAX_WORD_LENGTH + 1),
        )

        for (word in words) {
            assertNull(word, PronunciationValidator.normalizeWord(word))
        }
    }
}
