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

import java.util.Locale

/**
 * Converts American English ARPAbet to the IPA phones used by the shared Sherpa-ONNX 2.x lexicon.
 *
 * The lexicon's parser accepts `word || phone phone ...` lines and splits each token into
 * codepoints when its last codepoint is not alphanumeric. That means a mapping such as `CH` -> `t ʃ`
 * or `IY1` -> `ˈ iː` is written as ordinary space-separated phones and needs no special syntax.
 * Stress marks (`ˈ` primary, `ˌ` secondary) are emitted immediately before the stressed vowel,
 * which is equivalent to the lexicon's convention of attaching the mark to the previous token.
 *
 * The mapping deliberately uses only phones present in the shared English lexicon: it is the same
 * `tokens.txt` inventory that the on-device models consume. ARPAbet is American English, so this is
 * not a general-purpose G2P frontend.
 */
object ArpabetToIpa {
    /**
     * Bumped when [VOWELS] or [CONSONANTS] changes. The lexicon patch revision includes it so an
     * app update regenerates existing merged lexicons even when the user's list is unchanged.
     */
    const val VERSION: Int = 1

    /** Conservative cap so a pasted wall of text cannot bloat the lexicon or the UI. */
    const val MAX_TOKENS: Int = 128

    private val TOKEN = Regex("^([A-Za-z]+)([0-2])?$")
    private val SEPARATORS = Regex("[\\s\\-_,/]+")

    private data class Vowel(
        val unstressed: String,
        val primary: String,
        val secondary: String,
    )

    private val VOWELS: Map<String, Vowel> =
        mapOf(
            "AA" to Vowel(unstressed = "ɑ", primary = "ˈ ɑː", secondary = "ˌ ɑː"),
            "AE" to Vowel(unstressed = "æ", primary = "ˈ æ", secondary = "ˌ æ"),
            "AH" to Vowel(unstressed = "ə", primary = "ˈ ʌ", secondary = "ˌ ʌ"),
            "AO" to Vowel(unstressed = "ɔ", primary = "ˈ ɔː", secondary = "ˌ ɔː"),
            "AW" to Vowel(unstressed = "a ʊ", primary = "ˈ a ʊ", secondary = "ˌ a ʊ"),
            "AY" to Vowel(unstressed = "a ɪ", primary = "ˈ a ɪ", secondary = "ˌ a ɪ"),
            "EH" to Vowel(unstressed = "ɛ", primary = "ˈ ɛ", secondary = "ˌ ɛ"),
            "ER" to Vowel(unstressed = "ɚ", primary = "ˈ ɜː", secondary = "ˌ ɜː"),
            "EY" to Vowel(unstressed = "e ɪ", primary = "ˈ e ɪ", secondary = "ˌ e ɪ"),
            "IH" to Vowel(unstressed = "ɪ", primary = "ˈ ɪ", secondary = "ˌ ɪ"),
            "IY" to Vowel(unstressed = "i", primary = "ˈ iː", secondary = "ˌ iː"),
            "OW" to Vowel(unstressed = "o ʊ", primary = "ˈ o ʊ", secondary = "ˌ o ʊ"),
            "OY" to Vowel(unstressed = "ɔ ɪ", primary = "ˈ ɔ ɪ", secondary = "ˌ ɔ ɪ"),
            "UH" to Vowel(unstressed = "ʊ", primary = "ˈ ʊ", secondary = "ˌ ʊ"),
            "UW" to Vowel(unstressed = "u", primary = "ˈ uː", secondary = "ˌ uː"),
        )

    private val CONSONANTS: Map<String, String> =
        mapOf(
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

    /** Vowel symbols accepted in the pronunciation field, without a stress digit. */
    val vowelSymbols: Set<String> get() = VOWELS.keys

    /** Consonant symbols accepted in the pronunciation field. */
    val consonantSymbols: Set<String> get() = CONSONANTS.keys

    sealed interface Result {
        data class Success(val ipa: String, val phones: List<String>) : Result

        data class Failure(val reason: Error, val token: String? = null) : Result
    }

    enum class Error {
        EMPTY,
        TOO_MANY_SYMBOLS,
        UNKNOWN_SYMBOL,
        STRESS_ON_CONSONANT,
    }

    /**
     * Converts space, hyphen, comma, slash or underscore separated ARPAbet to a lexicon phone
     * string. Vowels may carry a stress digit (0, 1 or 2); a missing digit means unstressed.
     */
    fun convert(input: String): Result {
        val tokens = input.trim().split(SEPARATORS).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Result.Failure(Error.EMPTY)
        if (tokens.size > MAX_TOKENS) return Result.Failure(Error.TOO_MANY_SYMBOLS)

        val phones = ArrayList<String>(tokens.size + 2)
        for (raw in tokens) {
            val match = TOKEN.matchEntire(raw)
                ?: return Result.Failure(Error.UNKNOWN_SYMBOL, raw)
            val base = match.groupValues[1].uppercase(Locale.ROOT)
            val stressDigit = match.groupValues[2]

            val vowel = VOWELS[base]
            if (vowel != null) {
                phones += when (stressDigit) {
                    "1" -> vowel.primary
                    "2" -> vowel.secondary
                    else -> vowel.unstressed
                }
                continue
            }

            val consonant = CONSONANTS[base]
                ?: return Result.Failure(Error.UNKNOWN_SYMBOL, raw)
            if (stressDigit.isNotEmpty()) {
                return Result.Failure(Error.STRESS_ON_CONSONANT, raw)
            }
            phones += consonant
        }

        return Result.Success(ipa = phones.joinToString(" "), phones = phones)
    }
}
