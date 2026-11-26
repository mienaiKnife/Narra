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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArpabetToIpaTest {
    private data class VowelExpectation(
        val symbol: String,
        val unstressed: String,
        val primary: String,
        val secondary: String,
    )

    private val vowels = listOf(
        VowelExpectation("AA", "ɑ", "ˈ ɑː", "ˌ ɑː"),
        VowelExpectation("AE", "æ", "ˈ æ", "ˌ æ"),
        VowelExpectation("AH", "ə", "ˈ ʌ", "ˌ ʌ"),
        VowelExpectation("AO", "ɔ", "ˈ ɔː", "ˌ ɔː"),
        VowelExpectation("AW", "a ʊ", "ˈ a ʊ", "ˌ a ʊ"),
        VowelExpectation("AY", "a ɪ", "ˈ a ɪ", "ˌ a ɪ"),
        VowelExpectation("EH", "ɛ", "ˈ ɛ", "ˌ ɛ"),
        VowelExpectation("ER", "ɚ", "ˈ ɜː", "ˌ ɜː"),
        VowelExpectation("EY", "e ɪ", "ˈ e ɪ", "ˌ e ɪ"),
        VowelExpectation("IH", "ɪ", "ˈ ɪ", "ˌ ɪ"),
        VowelExpectation("IY", "i", "ˈ iː", "ˌ iː"),
        VowelExpectation("OW", "o ʊ", "ˈ o ʊ", "ˌ o ʊ"),
        VowelExpectation("OY", "ɔ ɪ", "ˈ ɔ ɪ", "ˌ ɔ ɪ"),
        VowelExpectation("UH", "ʊ", "ˈ ʊ", "ˌ ʊ"),
        VowelExpectation("UW", "u", "ˈ uː", "ˌ uː"),
    )

    private val consonants = mapOf(
        "B" to "b",
        "CH" to "t ʃ",
        "D" to "d",
        "DH" to "ð",
        "F" to "f",
        "G" to "ɡ",
        "HH" to "h",
        "JH" to "d ʒ",
        "K" to "k",
        "L" to "l",
        "M" to "m",
        "N" to "n",
        "NG" to "ŋ",
        "P" to "p",
        "R" to "ɹ",
        "S" to "s",
        "SH" to "ʃ",
        "T" to "t",
        "TH" to "θ",
        "V" to "v",
        "W" to "w",
        "Y" to "j",
        "Z" to "z",
        "ZH" to "ʒ",
    )

    private fun success(input: String): ArpabetToIpa.Result.Success {
        val result = ArpabetToIpa.convert(input)
        assertTrue("expected success for \"$input\" but was $result", result is ArpabetToIpa.Result.Success)
        return result as ArpabetToIpa.Result.Success
    }

    private fun failure(input: String): ArpabetToIpa.Result.Failure {
        val result = ArpabetToIpa.convert(input)
        assertTrue("expected failure for \"$input\" but was $result", result is ArpabetToIpa.Result.Failure)
        return result as ArpabetToIpa.Result.Failure
    }

    @Test
    fun `public inventories match the symbols covered by these tests`() {
        assertEquals(vowels.map { it.symbol }.toSet(), ArpabetToIpa.vowelSymbols)
        assertEquals(consonants.keys, ArpabetToIpa.consonantSymbols)
    }

    @Test
    fun `every vowel maps to its IPA for every stress level`() {
        // Each phone entry may contain the stress mark plus a one or two codepoint vowel.
        for (vowel in vowels) {
            assertEquals("${vowel.symbol} unstressed", vowel.unstressed, success(vowel.symbol).ipa)
            assertEquals("${vowel.symbol}0", vowel.unstressed, success("${vowel.symbol}0").ipa)
            assertEquals("${vowel.symbol}1", vowel.primary, success("${vowel.symbol}1").ipa)
            assertEquals("${vowel.symbol}2", vowel.secondary, success("${vowel.symbol}2").ipa)

            assertEquals(listOf(vowel.unstressed), success(vowel.symbol).phones)
            assertEquals(listOf(vowel.unstressed), success("${vowel.symbol}0").phones)
            assertEquals(listOf(vowel.primary), success("${vowel.symbol}1").phones)
            assertEquals(listOf(vowel.secondary), success("${vowel.symbol}2").phones)
        }
    }

    @Test
    fun `every consonant maps to its IPA`() {
        for ((symbol, ipa) in consonants) {
            assertEquals(symbol, ipa, success(symbol).ipa)
            assertEquals(symbol, listOf(ipa), success(symbol).phones)
        }
    }

    @Test
    fun `lowercase and mixed case input are accepted`() {
        assertEquals("k ˈ ɑː l d i", success("k aa1 l d iy0").ipa)
        assertEquals(success("K AA1 L D IY0").phones, success("k aa1 l d iy0").phones)
        assertEquals(success("M AY1").phones, success("m ay1").phones)
        assertEquals(success("AA1").phones, success("aA1").phones)
    }

    @Test
    fun `spaces hyphens commas slashes and underscores are equivalent separators`() {
        val expected = "k ˈ ɑː l d i"
        val inputs = listOf(
            "K AA1 L D IY0",
            "K-AA1-L-D-IY0",
            "K,AA1,L,D,IY0",
            "K/AA1/L/D/IY0",
            "K_AA1_L_D_IY0",
            "  K -- AA1,, L / D  IY0  ",
            "K\tAA1\nL\r\nD\tIY0",
        )

        for (input in inputs) {
            assertEquals(input, expected, success(input).ipa)
        }
    }

    @Test
    fun `a missing stress digit means unstressed`() {
        for (vowel in vowels) {
            assertEquals(vowel.symbol, vowel.unstressed, success(vowel.symbol).ipa)
            assertEquals(vowel.symbol, listOf(vowel.unstressed), success(vowel.symbol).phones)
        }
    }

    @Test
    fun `diphthong stress marks precede the first element`() {
        val primary = success("M AY1")
        assertEquals(listOf("m", "ˈ a ɪ"), primary.phones)
        assertEquals("m ˈ a ɪ", primary.ipa)

        assertEquals(listOf("m", "ˌ a ɪ"), success("M AY2").phones)
        assertEquals(listOf("m", "a ɪ"), success("M AY0").phones)
        assertEquals(listOf("m", "ˈ a ʊ"), success("M AW1").phones)
        assertEquals(listOf("m", "ˌ e ɪ"), success("M EY2").phones)
        assertEquals(listOf("m", "ˌ ɔ ɪ"), success("M OY2").phones)
    }

    @Test
    fun `realistic words convert through the whole pipeline`() {
        assertEquals("k ˈ ɑː l d i", success("K AA1 L D IY0").ipa)
        assertEquals("h ə l ˈ o ʊ", success("HH AH0 L OW1").ipa)
        assertEquals("w ˈ ɜː l d", success("W ER1 L D").ipa)
    }

    @Test
    fun `empty and blank input is rejected as EMPTY`() {
        val inputs = listOf("", " ", "   ", "\t", "\n", "\r\n", " - , / _ ")

        for (input in inputs) {
            val result = failure(input)
            assertEquals(input, ArpabetToIpa.Error.EMPTY, result.reason)
            assertNull(input, result.token)
        }
    }

    @Test
    fun `unknown symbols report the offending token`() {
        val inputs = listOf("XX", "EE", "Q", "AA3", "AA10", "A1A", "1AA", "123", "K!", "AA 3")

        for (input in inputs) {
            val result = failure(input)
            assertEquals(input, ArpabetToIpa.Error.UNKNOWN_SYMBOL, result.reason)
            assertEquals(input, input.substringAfterLast(' '), result.token)
        }
    }

    @Test
    fun `a stress digit on a consonant is rejected with the token`() {
        for (token in listOf("B1", "CH2", "K0", "SH1", "ZH2")) {
            val result = failure(token)
            assertEquals(token, ArpabetToIpa.Error.STRESS_ON_CONSONANT, result.reason)
            assertEquals(token, token, result.token)
        }
    }

    @Test
    fun `at most MAX_TOKENS symbols are accepted`() {
        val atLimit = List(ArpabetToIpa.MAX_TOKENS) { "K" }.joinToString(" ")
        assertEquals(ArpabetToIpa.MAX_TOKENS, success(atLimit).phones.size)

        val overLimit = List(ArpabetToIpa.MAX_TOKENS + 1) { "K" }.joinToString(" ")
        val result = failure(overLimit)
        assertEquals(ArpabetToIpa.Error.TOO_MANY_SYMBOLS, result.reason)
        assertNull(result.token)
    }

    @Test
    fun `phones use the IPA inventory instead of ASCII stand-ins`() {
        assertEquals("ɡ", success("G").ipa)
        assertFalse("ASCII g must not stand in for the IPA script g", success("G").phones.contains("g"))
        assertEquals("ɹ", success("R").ipa)
        assertFalse("ASCII r must not stand in for the IPA approximant", success("R").phones.contains("r"))
        assertEquals("j", success("Y").ipa)
        assertEquals("t ʃ", success("CH").ipa)
        assertEquals("d ʒ", success("JH").ipa)

        val allowed = buildSet {
            for (vowel in vowels) {
                add(vowel.unstressed)
                add(vowel.primary)
                add(vowel.secondary)
            }
            addAll(consonants.values)
        }
        val phones = success("HH AH0 L OW1 W ER1 L D G R Y CH").phones
        assertTrue("unexpected phone in $phones", allowed.containsAll(phones))
    }
}
