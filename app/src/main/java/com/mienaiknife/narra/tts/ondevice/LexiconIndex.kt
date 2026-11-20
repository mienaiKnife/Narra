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

import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

/**
 * Membership index over the pronunciation lexicon Sherpa-ONNX 2.x will read.
 *
 * The native frontend silently drops any word it cannot find, so [SpeechTextPreparer] needs to know
 * which words are safe before synthesis. The lexicon has ~168k keys; keeping them as strings would
 * cost several megabytes, so keys are stored as sorted 64-bit FNV-1a hashes (a little over a
 * megabyte) and looked up with binary search.
 *
 * A hash collision can only produce a false positive: a word that is not really in the lexicon is
 * treated as known, left alone, and dropped by the native engine. With 2^64 hash space and ~168k
 * keys the expected number of collisions is below 10^-9, and it never causes a false negative (a
 * real word being rewritten), so it is preferable to holding every key in memory.
 */
internal class LexiconIndex private constructor(
    private val singleWordKeys: LongArray,
    private val phraseWordKeys: LongArray,
) {
    /** Whether [word] is a single-word lexicon entry. */
    fun isKnown(word: String): Boolean = contains(singleWordKeys, normalizeQuery(word))

    /** Whether [word] appears inside a multi-word lexicon entry (e.g. `baton` in `Baton rouge`). */
    fun isPhraseWord(word: String): Boolean = contains(phraseWordKeys, normalizeQuery(word))

    /** Whether the native engine can pronounce [word] as-is, either as an entry or as a phrase part. */
    fun knows(word: String): Boolean {
        val key = normalizeQuery(word)
        if (key.isEmpty()) return false
        val hash = hashOf(key)
        return contains(singleWordKeys, hash) || contains(phraseWordKeys, hash)
    }

    private fun contains(keys: LongArray, key: String): Boolean {
        if (key.isEmpty() || keys.isEmpty()) return false
        return contains(keys, hashOf(key))
    }

    private fun contains(keys: LongArray, hash: Long): Boolean = keys.binarySearch(hash) >= 0

    companion object {
        private const val KEY_SEPARATOR = "||"
        private const val FNV_OFFSET_BASIS = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
        private const val FNV_PRIME = 0x100000001b3L

        /** Loads a lexicon file (base or patched). Comment lines, including the patch marker, are skipped. */
        fun load(file: File): LexiconIndex {
            if (!file.isFile || file.length() == 0L) {
                throw IOException("Pronunciation lexicon is missing or empty: ${file.absolutePath}")
            }

            val singleWordKeys = LongArrayBuilder()
            val phraseWordKeys = LongArrayBuilder()
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val separator = trimmed.indexOf(KEY_SEPARATOR)
                        if (separator > 0) {
                            val key = normalizeKey(trimmed.substring(0, separator))
                            if (key.isNotEmpty()) {
                                if (key.any { it.isWhitespace() }) {
                                    for (part in key.split(' ')) {
                                        if (part.isNotEmpty()) phraseWordKeys.add(hashOf(part))
                                    }
                                } else {
                                    singleWordKeys.add(hashOf(key))
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
            return LexiconIndex(singleWordKeys.toSortedDistinctArray(), phraseWordKeys.toSortedDistinctArray())
        }

        /** Builds an index without a file, for tests and fixtures. */
        fun fromWords(
            singleWords: Collection<String>,
            phraseWords: Collection<String> = emptyList(),
        ): LexiconIndex {
            val words = LongArrayBuilder()
            for (word in singleWords) {
                val key = normalizeKey(word)
                if (key.isNotEmpty()) words.add(hashOf(key))
            }
            val phrases = LongArrayBuilder()
            for (word in phraseWords) {
                val key = normalizeKey(word)
                if (key.isNotEmpty()) phrases.add(hashOf(key))
            }
            return LexiconIndex(words.toSortedDistinctArray(), phrases.toSortedDistinctArray())
        }

        /**
         * Matches the native loader (`NormalizeLexiconKey`): NFC, lowercase, NFC. Punctuation is
         * kept, because entries such as `mr.` are stored with it.
         */
        private fun normalizeKey(raw: String): String {
            val nfc = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
            return Normalizer.normalize(nfc.lowercase(Locale.ROOT), Normalizer.Form.NFC)
        }

        /**
         * A lookup key. The native OOV retry strips leading and trailing non-word characters before
         * retrying the lexicon, so `"(hello)"` is looked up as `hello`. Apostrophes inside the key
         * survive because the lexicon keys contractions (`don't`).
         */
        private fun normalizeQuery(raw: String): String {
            val key = normalizeKey(raw)
            val start = key.indexOfFirst { it.isLetterOrDigit() }
            if (start < 0) return ""
            val end = key.indexOfLast { it.isLetterOrDigit() }
            return key.substring(start, end + 1)
        }

        private fun hashOf(key: String): Long {
            var hash = FNV_OFFSET_BASIS
            for (char in key) {
                hash = hash xor char.code.toLong()
                hash *= FNV_PRIME
            }
            return hash
        }
    }

    /** Growable [LongArray] so loading does not box 168k keys. */
    private class LongArrayBuilder(initialCapacity: Int = 1024) {
        private var values = LongArray(initialCapacity)
        private var size = 0

        fun add(value: Long) {
            if (size == values.size) values = values.copyOf(size * 2)
            values[size++] = value
        }

        fun toSortedDistinctArray(): LongArray {
            val sorted = values.copyOf(size)
            sorted.sort()
            var unique = 0
            for (index in sorted.indices) {
                if (index == 0 || sorted[index] != sorted[index - 1]) {
                    sorted[unique++] = sorted[index]
                }
            }
            return sorted.copyOf(unique)
        }
    }
}
