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

import java.text.Normalizer
import java.util.Locale

/**
 * Validates and normalizes a user pronunciation before it is persisted or written to the lexicon.
 *
 * The word is reduced to the same key the native frontend uses (`NormalizeLexiconKey`: NFC,
 * lowercase, NFC) and internal whitespace is collapsed so multi-word entries match the frontend's
 * single-space phrase keys. Punctuation and digits are rejected because `SpeechTextNormalizer`
 * expands or replaces them before synthesis, so an entry containing them could never match.
 */
object PronunciationValidator {
    /** Words longer than this are almost certainly pasted text, not a pronunciation entry. */
    const val MAX_WORD_LENGTH: Int = 80

    private val WHITESPACE = Regex("\\s+")

    sealed interface Result {
        data class Valid(
            val word: String,
            val normalizedWord: String,
            val ipa: String,
        ) : Result

        data class Invalid(
            val error: Error,
            val detail: String? = null,
        ) : Result
    }

    enum class Error {
        EMPTY_WORD,
        WORD_TOO_LONG,
        INVALID_WORD_CHARACTER,
        EMPTY_PRONUNCIATION,
        UNKNOWN_SYMBOL,
        STRESS_ON_CONSONANT,
        TOO_MANY_SYMBOLS,
    }

    fun validate(word: String, arpabet: String): Result {
        val normalizedWord = normalizeWord(word)
            ?: return Result.Invalid(wordError(word))

        return when (val converted = ArpabetToIpa.convert(arpabet)) {
            is ArpabetToIpa.Result.Success -> Result.Valid(
                word = collapseWhitespace(word),
                normalizedWord = normalizedWord,
                ipa = converted.ipa,
            )

            is ArpabetToIpa.Result.Failure -> Result.Invalid(
                error = when (converted.reason) {
                    ArpabetToIpa.Error.EMPTY -> Error.EMPTY_PRONUNCIATION
                    ArpabetToIpa.Error.TOO_MANY_SYMBOLS -> Error.TOO_MANY_SYMBOLS
                    ArpabetToIpa.Error.UNKNOWN_SYMBOL -> Error.UNKNOWN_SYMBOL
                    ArpabetToIpa.Error.STRESS_ON_CONSONANT -> Error.STRESS_ON_CONSONANT
                },
                detail = converted.token,
            )
        }
    }

    /** Returns the normalized lexicon key, or `null` when [raw] cannot be a word. */
    fun normalizeWord(raw: String): String? {
        val word = collapseWhitespace(raw)
        if (word.isEmpty()) return null
        if (word.length > MAX_WORD_LENGTH) return null
        if (word.none { it.isLetter() }) return null
        if (word.any { !it.isLetter() && it != ' ' && it != '\'' && it != '-' }) return null
        return Normalizer.normalize(
            word.lowercase(Locale.ROOT),
            Normalizer.Form.NFC,
        )
    }

    private fun wordError(raw: String): Error {
        val word = collapseWhitespace(raw)
        if (word.isEmpty()) return Error.EMPTY_WORD
        if (word.length > MAX_WORD_LENGTH) return Error.WORD_TOO_LONG
        return Error.INVALID_WORD_CHARACTER
    }

    /**
     * Normalizes to NFC first so a decomposed accent composes into a letter before the character
     * check. Without this, `Cafe` + U+0301 would be rejected because the combining mark is not a
     * letter.
     */
    private fun collapseWhitespace(raw: String): String = Normalizer.normalize(
        raw.replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .trim()
            .replace(WHITESPACE, " "),
        Normalizer.Form.NFC,
    )
}
