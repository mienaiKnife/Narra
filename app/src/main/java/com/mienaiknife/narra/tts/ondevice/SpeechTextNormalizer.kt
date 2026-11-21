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
 * and ordinals into words, replaces "&" with "and", and turns numeric ranges into "to". Only
 * English output is produced because the on-device engine only ships an English lexicon; models
 * for other languages fail before reaching this class.
 */
internal object SpeechTextNormalizer {
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

                else -> {
                    appendPreserved(c, i)
                    i++
                }
            }
        }

        return NormalizedSpeech(out.toString(), sources.toIntArray())
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
