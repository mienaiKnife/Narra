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

import java.util.Locale

/**
 * Speech-ready text plus the mapping back to the original text it was produced from.
 *
 * Sherpa-ONNX 2.x synthesizes from a pronunciation lexicon, so digits (which the lexicon does not
 * contain) have to be spelled out. [SpeechTextNormalizer] rewrites them, which changes character
 * offsets; [sourceRange] translates a range in [text] back to the paragraph the reader displays so
 * word highlighting keeps pointing at the right characters.
 */
internal class NormalizedSpeech(
    val text: String,
    private val sourceIndexByChar: IntArray,
) {
    /**
     * Maps a `[startChar, endChar)` range in [text] to the original text, or `null` when the range
     * is empty or out of bounds.
     */
    fun sourceRange(startChar: Int, endChar: Int): IntRange? {
        if (startChar < 0 || startChar >= endChar || endChar > sourceIndexByChar.size) return null
        val start = sourceIndexByChar[startChar]
        val end = sourceIndexByChar[endChar - 1]
        return start..end
    }

    /**
     * Original index for one character of [text]. Used when composing a second rewrite pass, which
     * has to carry the existing mapping forward.
     */
    internal fun sourceIndexAt(charIndex: Int): Int = sourceIndexByChar[charIndex]

    companion object {
        /** Mapping for text that was already speech-ready. */
        fun identity(text: String): NormalizedSpeech = NormalizedSpeech(text, IntArray(text.length) { it })
    }
}

/**
 * Rewrites text into something Sherpa-ONNX 2.x can pronounce from its English lexicon.
 *
 * The 2.x lexicons contain no digits, so "2026" or "CVE-2026-120382" are dropped as out-of-
 * vocabulary words and the sentence loses information. This expands numbers, currency, percentages
 * and ordinals into words, replaces "&" with "and", turns numeric ranges into "to", converts
 * typographic apostrophes to the ASCII apostrophe the lexicon uses as a key for contractions, and
 * collapses runs of sentence punctuation ("...", "?!", "!!!") to a single mark. Only English output
 * is produced because the on-device engine only ships an English lexicon; models for other languages
 * fail before reaching this class.
 */
internal object SpeechTextNormalizer {
    /**
     * Apostrophe-like characters that appear in web and EPUB text but are not the ASCII apostrophe
     * the pronunciation lexicon uses as a word key.
     */
    private const val TYPOGRAPHIC_APOSTROPHES = "’‘‛ʼ′‵"

    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen",
    )
    private val tens = listOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
    )
    private val scales = listOf(
        1_000_000_000L to "billion",
        1_000_000L to "million",
        1_000L to "thousand",
    )
    private val ordinalIrregulars = mapOf(
        "one" to "first",
        "two" to "second",
        "three" to "third",
        "four" to "fourth",
        "five" to "fifth",
        "six" to "sixth",
        "seven" to "seventh",
        "eight" to "eighth",
        "nine" to "ninth",
        "ten" to "tenth",
        "eleven" to "eleventh",
        "twelve" to "twelfth",
        "thirteen" to "thirteenth",
        "fourteen" to "fourteenth",
        "fifteen" to "fifteenth",
        "sixteen" to "sixteenth",
        "seventeen" to "seventeenth",
        "eighteen" to "eighteenth",
        "nineteen" to "nineteenth",
        "twenty" to "twentieth",
        "thirty" to "thirtieth",
        "forty" to "fortieth",
        "fifty" to "fiftieth",
        "sixty" to "sixtieth",
        "seventy" to "seventieth",
        "eighty" to "eightieth",
        "ninety" to "ninetieth",
        "hundred" to "hundredth",
        "thousand" to "thousandth",
        "million" to "millionth",
        "billion" to "billionth",
    )

    fun normalize(input: String): NormalizedSpeech {
        if (input.isEmpty()) return NormalizedSpeech("", IntArray(0))

        val out = StringBuilder(input.length + 16)
        val sources = ArrayList<Int>(input.length + 16)

        fun appendPreserved(c: Char, source: Int) {
            out.append(c)
            sources.add(source)
        }

        fun trimTrailingSpace() {
            if (out.isNotEmpty() && out.last() == ' ') {
                out.setLength(out.length - 1)
                sources.removeAt(sources.size - 1)
            }
        }

        fun appendExpanded(replacement: String, sourceStart: Int, sourceLength: Int) {
            for ((index, c) in replacement.withIndex()) {
                out.append(c)
                val source = if (sourceLength <= 1) {
                    sourceStart
                } else {
                    sourceStart + (index * sourceLength) / replacement.length
                }
                sources.add(source.coerceIn(sourceStart, sourceStart + sourceLength - 1))
            }
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                // Bullets carry no sound and are not in the lexicon.
                c == '•' -> i++

                // Runs of sentence punctuation are unsafe: the native frontend ends a sentence at
                // every '.', '!', '?', ';' and ':', so "Wait...", "What?!" and "Wow!!" leave it
                // synthesizing sentences that contain nothing but punctuation, which the vocoder
                // renders as a click/noise. Collapse the run to one representative mark.
                isSentencePunctuation(c) -> {
                    val (end, punctuation) = collapseSentencePunctuation(input, i)
                    appendExpanded(punctuation.toString(), i, end - i)
                    i = end
                }

                // Em/en dashes are in the lexicon's out-of-vocabulary bucket and get dropped,
                // losing the pause. A comma reads the same way and is pronounceable.
                c == '—' || c == '–' -> {
                    trimTrailingSpace()
                    appendExpanded(", ", i, 1)
                    // Consume a following space so "a – b" does not end up with two.
                    i += if (i + 1 < input.length && input[i + 1].isWhitespace()) 2 else 1
                }

                c == '&' -> {
                    appendExpanded("and", i, 1)
                    i++
                }

                // "1990-2000" is a range; "CVE-2026" is a prefix that should not be glued to the
                // number. A letter-hyphen-letter sequence (e.g. "twenty-six") is left alone.
                c == '-' &&
                    i + 1 < input.length &&
                    input[i + 1].isDigit() &&
                    i > 0 &&
                    input[i - 1].isDigit() -> {
                    appendExpanded(" to ", i, 1)
                    i++
                }

                c == '-' &&
                    i + 1 < input.length &&
                    input[i + 1].isDigit() &&
                    i > 0 &&
                    input[i - 1].isLetter() -> {
                    appendPreserved(' ', i)
                    i++
                }

                c == '-' && i + 1 < input.length && input[i + 1].isDigit() -> {
                    val end = numberEnd(input, i + 1)
                    val words = "minus " + speakNumber(input.substring(i + 1, end))
                    appendExpanded(words, i, end - i)
                    i = end
                }

                c == '$' && i + 1 < input.length && input[i + 1].isDigit() -> {
                    val end = numberEnd(input, i + 1)
                    val words = speakNumber(input.substring(i + 1, end))
                    val value = input.substring(i + 1, end).replace(",", "").toDoubleOrNull()
                    val unit = if (value == 1.0) " dollar" else " dollars"
                    appendExpanded(words + unit, i, end - i)
                    i = end
                }

                c.isDigit() -> {
                    // Keep identifiers readable: "v0.1" would otherwise become the single OOV
                    // token "vzero". A space lets the tokenizer split the letter from the number.
                    if (i > 0 && input[i - 1].isLetter()) appendPreserved(' ', i - 1)

                    val end = numberEnd(input, i)
                    val token = input.substring(i, end)
                    val ordinalSuffix = ordinalSuffixAt(input, end)
                    val isPercent = ordinalSuffix == null && end < input.length && input[end] == '%'
                    val replacement = when {
                        ordinalSuffix != null -> speakOrdinal(token)
                        else -> speakNumber(token)
                    }
                    val consumedEnd = when {
                        ordinalSuffix != null -> end + 2
                        isPercent -> end + 1
                        else -> end
                    }
                    val suffix = if (isPercent) " percent" else ""
                    appendExpanded(replacement + suffix, i, consumedEnd - i)
                    // "H2O" must not become the single token "twoO".
                    if (consumedEnd < input.length && input[consumedEnd].isLetter()) {
                        appendPreserved(' ', consumedEnd)
                    }
                    i = consumedEnd
                }

                // The English lexicon keys contractions with U+0027 ("i'm", "i've"). Web articles
                // and EPUBs use typographic apostrophes, which miss the lookup: the native
                // tokenizer then falls back to splitting "I’m" into "I" + "m" (heard as "I em")
                // and "I’ve" into "I" + "ve" (heard as "I vee"). Normalize the apostrophe so the
                // contraction is looked up as a whole word.
                c in TYPOGRAPHIC_APOSTROPHES -> {
                    appendPreserved('\'', i)
                    i++
                }

                else -> {
                    appendPreserved(c, i)
                    i++
                }
            }
        }

        return NormalizedSpeech(out.toString(), sources.toIntArray())
    }

    private fun isSentencePunctuation(c: Char): Boolean = c in ".!?;:…"

    /**
     * Collapses a run of sentence punctuation into one mark. Returns the exclusive end of the run
     * and the mark to emit. Adjacent dots with spaces between them count as an ellipsis, and a
     * question or exclamation mark wins over a period so "?!" reads as a question.
     */
    private fun collapseSentencePunctuation(text: String, start: Int): Pair<Int, Char> {
        var index = start
        var hasQuestion = false
        var hasExclamation = false
        var hasPeriod = false
        var hasSemicolon = false
        var hasColon = false
        while (index < text.length) {
            when (text[index]) {
                '?' -> {
                    hasQuestion = true
                    index++
                }
                '!' -> {
                    hasExclamation = true
                    index++
                }
                '.', '…' -> {
                    hasPeriod = true
                    index++
                }
                ';' -> {
                    hasSemicolon = true
                    index++
                }
                ':' -> {
                    hasColon = true
                    index++
                }
                else -> {
                    // Spaces belong to the run only when they are part of a spaced ellipsis.
                    if (text[index].isWhitespace() && hasPeriod) {
                        var lookahead = index
                        while (lookahead < text.length && text[lookahead].isWhitespace()) lookahead++
                        if (lookahead < text.length && (text[lookahead] == '.' || text[lookahead] == '…')) {
                            index = lookahead
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
            }
        }
        val punctuation = when {
            hasQuestion -> '?'
            hasExclamation -> '!'
            hasPeriod -> '.'
            hasSemicolon -> ';'
            hasColon -> ':'
            // Unreachable: every branch of the loop sets one of the flags above.
            else -> text[start]
        }
        return index to punctuation
    }

    /** Returns the index just past a number token (`1,234.5`), or [start] when there is none. */
    private fun numberEnd(text: String, start: Int): Int {
        var i = start
        while (i < text.length && (text[i].isDigit() || (text[i] == ',' && i + 1 < text.length && text[i + 1].isDigit()))) {
            i++
        }
        if (i + 1 < text.length && text[i] == '.' && text[i + 1].isDigit()) {
            i++
            while (i < text.length && text[i].isDigit()) i++
        }
        return i
    }

    private fun ordinalSuffixAt(text: String, index: Int): String? {
        if (index + 2 > text.length) return null
        val suffix = text.substring(index, minOf(index + 2, text.length)).lowercase(Locale.ROOT)
        if (suffix !in setOf("st", "nd", "rd", "th")) return null
        val after = index + 2
        if (after < text.length && text[after].isLetter()) return null
        return suffix
    }

    private fun speakNumber(token: String): String {
        val hasSeparators = token.contains(',')
        val (integralText, fractionText) = token.split('.', limit = 2).let {
            it[0].replace(",", "") to it.getOrNull(1).orEmpty()
        }
        val integral = integralText.toLongOrNull() ?: return speakDigits(integralText)
        val integralWords = when {
            integralText.length > 4 && !hasSeparators -> speakDigits(integralText)
            else -> numberToWords(integral)
        }
        val trimmedFraction = fractionText.trimEnd('0')
        return if (trimmedFraction.isEmpty()) {
            integralWords
        } else {
            "$integralWords point ${speakDigits(trimmedFraction)}"
        }
    }

    private fun speakOrdinal(token: String): String {
        val value = token.replace(",", "").toLongOrNull() ?: return "number ${speakDigits(token)}"
        return ordinalOf(numberToWords(value))
    }

    private fun ordinalOf(words: String): String {
        val splitAt = maxOf(words.lastIndexOf(' '), words.lastIndexOf('-'))
        val head = if (splitAt >= 0) words.substring(0, splitAt + 1) else ""
        val last = if (splitAt >= 0) words.substring(splitAt + 1) else words
        return head + (ordinalIrregulars[last] ?: last + "th")
    }

    private fun speakDigits(digits: String): String = digits.map { ones[it.digitToInt()] }.joinToString(" ")

    private fun numberToWords(value: Long): String {
        if (value == 0L) return "zero"
        if (value < 0L) return "minus " + numberToWords(-value)

        val parts = mutableListOf<String>()
        var rest = value
        for ((scaleValue, scaleName) in scales) {
            if (rest >= scaleValue) {
                parts += "${numberToWords(rest / scaleValue)} $scaleName"
                rest %= scaleValue
            }
        }
        if (rest >= 100L) {
            parts += "${ones[(rest / 100L).toInt()]} hundred"
            rest %= 100L
        }
        if (rest > 0L) parts += belowHundred(rest.toInt())
        return parts.joinToString(" ")
    }

    private fun belowHundred(value: Int): String = when {
        value < 20 -> ones[value]
        value % 10 == 0 -> tens[value / 10]
        else -> "${tens[value / 10]}-${ones[value % 10]}"
    }
}
