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

import java.text.Normalizer
import java.util.Locale

/**
 * Makes text pronounceable by the Sherpa-ONNX 2.x lexicon frontend.
 *
 * [SpeechTextNormalizer] handles everything that can be rewritten without knowing the dictionary
 * (digits, currency, punctuation). This pass knows the words the lexicon actually contains — see
 * [LexiconIndex] — and rewrites the ones the native engine would silently drop:
 *
 * - Known words, contractions, and components of multi-word entries are left alone.
 * - Hyphenated words stay hyphenated when every piece is known; otherwise the pieces are spoken
 *   separately instead of letting the native hyphen-join produce a word it cannot look up.
 * - Possessives prefer a known plural (`company's` -> `companies`), then the bare stem
 *   (`John's` -> `John`), and never emit the separate "s" the native tokenizer would read as "ess".
 * - Short all-caps acronyms are spelled (`LLM` -> `L L M`).
 * - Other unknown words are decomposed into known pieces (`multimodal` -> `multi modal`,
 *   `wokeness` -> `woke ness`) or inflected stems, and spelled as a last resort. A name is never
 *   left silent.
 *
 * Every rewrite keeps the source mapping, so highlighting still expands to the original word.
 * The rules are English-specific: only English lexicons are supported today (see `ModelLexicons`),
 * and another language would need its own strategy rather than spelling its words letter by letter.
 */
internal object SpeechTextPreparer {
    /** Words beyond this length are left alone: spelling them would be noise. */
    private const val MAX_WORD_LENGTH = 40

    /**
     * A word as the native frontend sees it before lookup: letters with internal apostrophes and
     * hyphens. Digits were already expanded by [SpeechTextNormalizer].
     */
    private val WORD = Regex("[\\p{L}]+(?:['’\\-][\\p{L}]+)*['’]?")

    private const val VOWELS = "aeiou"

    /** Suffixes whose removal can leave a known stem, longest first. */
    private val INFLECTION_SUFFIXES = listOf("ing", "est", "ies", "ied", "ed", "es", "er", "ly", "s")
    private val E_RESTORING_SUFFIXES = setOf("ing", "ed", "es", "er", "est")
    private val DOUBLING_SUFFIXES = setOf("ing", "ed", "er", "est")

    /**
     * Derivation suffixes that are not standalone lexicon words. When only the stem is known, the
     * stem is spoken and the suffix is dropped rather than turned into a bogus word.
     */
    private val DERIVATIONAL_SUFFIXES = listOf(
        "ations", "ation", "ments", "ment", "nesses", "ness", "ities", "ity",
        "ables", "able", "ibles", "ible", "ships", "ship", "isms", "ism", "ists",
        "ist", "less", "ful", "ous", "ive", "ance", "ence", "hood", "wards",
        "ward", "wise", "dom", "ary", "ory", "ery", "ish", "age", "ize", "ise",
        "ify", "al", "ic",
    )

    /**
     * Runs [SpeechTextNormalizer] and then the lexicon-aware pass. Without an [index] — no model,
     * a non-lexicon frontend, or an index that failed to build — the normalized text is returned
     * unchanged, so callers never need a special case.
     */
    fun prepare(input: String, index: LexiconIndex?): NormalizedSpeech {
        val normalized = SpeechTextNormalizer.normalize(input)
        if (index == null || normalized.text.isEmpty()) return normalized
        return rewrite(normalized, index)
    }

    private fun rewrite(speech: NormalizedSpeech, index: LexiconIndex): NormalizedSpeech {
        val text = speech.text
        val out = StringBuilder(text.length + 32)
        val sources = IntArrayBuilder(text.length + 32)
        var cursor = 0

        fun copyRange(from: Int, to: Int) {
            for (position in from until to) {
                out.append(text[position])
                sources.add(speech.sourceIndexAt(position))
            }
        }

        for (match in WORD.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            copyRange(cursor, start)
            val replacement = spokenForm(match.value, index)
            if (replacement == null) {
                copyRange(start, end)
            } else {
                appendMapped(out, sources, replacement, speech, start, end)
            }
            cursor = end
        }
        copyRange(cursor, text.length)

        return NormalizedSpeech(out.toString(), sources.toIntArray())
    }

    /**
     * Spoken replacement for [token], or `null` when the token can be copied unchanged. A rewritten
     * token's characters all map to the token's original source range, so a highlight that lands on
     * any spelled letter still expands to the displayed word.
     */
    private fun spokenForm(token: String, index: LexiconIndex): String? {
        if (token.length > MAX_WORD_LENGTH) return null

        // A word that cannot be folded to ASCII (CJK, ß, ...) is left alone rather than mangled.
        val folded = asciiFold(token) ?: return null
        val key = folded.lowercase(Locale.ROOT)

        // Known entries, contractions (it's, let's), and phrase components read correctly natively.
        // A trailing apostrophe is handled first: the native lookup already ignores it, so a known
        // plural possessive like "controllers'" would otherwise slip through with the mark intact.
        if (folded.endsWith("'") && folded.length > 1) {
            val plural = folded.dropLast(1)
            val pluralKey = plural.lowercase(Locale.ROOT)
            if (index.knows(pluralKey)) return plural
            // Plural possessive of an unknown word: drop the plural "s" too rather than spell it.
            if (pluralKey.endsWith("s") && pluralKey.length > 2) {
                val stem = plural.dropLast(1)
                return spokenForm(stem, index) ?: stem
            }
            return spokenWord(pluralKey, index)
        }

        if (index.knows(key)) return folded.takeIf { it != token }

        if (key.endsWith("'s") && key.length > 2) {
            val stem = key.dropLast(2)
            // Never emit the leftover "s": the native tokenizer would read it as "ess".
            return pluralForm(stem, index) ?: spokenForm(stem, index) ?: stem
        }

        if ('-' in key) {
            val parts = key.split('-').filter { it.isNotEmpty() }
            if (parts.isNotEmpty() && parts.all { index.knows(it) }) return null
            // Spacing the pieces out keeps the native hyphen-join from gluing a spelled word.
            return parts.joinToString(" ") { spokenWord(it, index) }
        }

        if (isUppercaseAcronym(folded)) return spellLetters(folded)

        return spokenWord(key, index).takeIf { it != key }
    }

    /** Best-effort spoken form of a single word; always returns something pronounceable. */
    private fun spokenWord(word: String, index: LexiconIndex): String {
        if (index.knows(word)) return word
        decompose(word, index)?.let { return it }
        inflectedStem(word, index)?.let { return it }
        derivationalStem(word, index)?.let { return it }
        return spellLetters(word)
    }

    /**
     * Splits an unknown word into two known pieces, e.g. `multimodal` -> `multi modal`. Splits are
     * tried with the shortest known prefix first so `wokeness` becomes `woke ness` rather than
     * `woken ess`. Pronunciation is what matters here, so a semantically odd split is acceptable.
     */
    private fun decompose(word: String, index: LexiconIndex): String? {
        if (word.length < 4) return null
        for (split in 2 until word.length - 1) {
            val left = word.substring(0, split)
            if (!index.knows(left)) continue
            val right = word.substring(split)
            if (index.knows(right)) return "$left $right"
            inflectedStem(right, index)?.let { return "$left $it" }
        }
        return null
    }

    /** Known plural for a possessive stem: `company` -> `companies`, `controller` -> `controllers`. */
    private fun pluralForm(stem: String, index: LexiconIndex): String? {
        if (stem.length < 2) return null
        if (stem.length > 2 && stem.endsWith("y") && stem[stem.length - 2] !in VOWELS) {
            val ies = stem.dropLast(1) + "ies"
            if (index.knows(ies)) return ies
        }
        val s = stem + "s"
        if (index.knows(s)) return s
        val es = stem + "es"
        if (index.knows(es)) return es
        return null
    }

    /**
     * Known stem behind a regular inflection: `organising` -> `organise`, `cities` -> `city`,
     * `running` -> `run`. Only the stem is spoken; the inflection is not turned into a word.
     */
    private fun inflectedStem(word: String, index: LexiconIndex): String? {
        if (word.length < 4) return null
        for (suffix in INFLECTION_SUFFIXES) {
            if (!word.endsWith(suffix)) continue
            val stem = word.dropLast(suffix.length)
            if (stem.length < 2) continue
            if (index.knows(stem)) return stem
            if (suffix == "ies" || suffix == "ied") {
                val singular = stem + "y"
                if (index.knows(singular)) return singular
            }
            if (suffix in E_RESTORING_SUFFIXES) {
                val restored = stem + "e"
                if (index.knows(restored)) return restored
            }
            if (suffix in DOUBLING_SUFFIXES && stem.length > 2 && stem.last() == stem[stem.length - 2]) {
                val undoubled = stem.dropLast(1)
                if (index.knows(undoubled)) return undoubled
            }
        }
        return null
    }

    /** Known stem behind a non-word suffix: `wokeness` -> `woke`. */
    private fun derivationalStem(word: String, index: LexiconIndex): String? {
        for (suffix in DERIVATIONAL_SUFFIXES) {
            if (!word.endsWith(suffix)) continue
            val stem = word.dropLast(suffix.length)
            if (stem.length < 4) continue
            if (index.knows(stem)) return stem
            // Consonant + "al"/"ise" often drop an "e": "organisational" -> "organise".
            if (index.knows(stem + "e")) return stem + "e"
        }
        return null
    }

    private fun isUppercaseAcronym(word: String): Boolean = word.length in 2..6 && word.all { it in 'A'..'Z' }

    /** `LLM` -> `L L M`. The lexicon has every single letter as a word. */
    private fun spellLetters(word: String): String {
        val letters = word.filter { it.isLetter() }
        if (letters.isEmpty()) return word
        return letters.toCharArray().joinToString(" ") { it.toString() }
    }

    /**
     * Removes accents when that yields ASCII (`café` -> `cafe`). Returns `null` for words that
     * cannot be folded, which callers leave unchanged.
     */
    private fun asciiFold(word: String): String? {
        if (word.all { it.code < 0x80 }) return word
        val decomposed = Normalizer.normalize(word, Normalizer.Form.NFD)
        val folded = StringBuilder(decomposed.length)
        for (char in decomposed) {
            when {
                Character.getType(char) == Character.NON_SPACING_MARK.toInt() -> Unit
                char.code < 0x80 -> folded.append(char)
                else -> return null
            }
        }
        return folded.toString()
    }

    private fun appendMapped(
        out: StringBuilder,
        sources: IntArrayBuilder,
        replacement: String,
        speech: NormalizedSpeech,
        start: Int,
        end: Int,
    ) {
        val firstSource = speech.sourceIndexAt(start)
        val lastSource = speech.sourceIndexAt(end - 1)
        val rangeStart = minOf(firstSource, lastSource)
        val rangeLength = maxOf(firstSource, lastSource) - rangeStart + 1
        for ((index, char) in replacement.withIndex()) {
            out.append(char)
            val offset = if (rangeLength <= 1) 0 else (index * rangeLength) / replacement.length
            sources.add(rangeStart + offset.coerceIn(0, rangeLength - 1))
        }
    }

    /** Growable [IntArray], mirroring the source map built by [SpeechTextNormalizer]. */
    private class IntArrayBuilder(initialCapacity: Int) {
        private var values = IntArray(maxOf(initialCapacity, 16))
        private var size = 0

        fun add(value: Int) {
            if (size == values.size) values = values.copyOf(size * 2)
            values[size++] = value
        }

        fun toIntArray(): IntArray = values.copyOf(size)
    }
}
