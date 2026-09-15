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
package com.mienaiknife.narra.domain.models

/**
 * Represents text that is ready for TTS synthesis, with a mapping back to the original text.
 */
data class SpeakableText(
    val text: String,
    /**
     * Maps each index in [text] back to the corresponding index in the original text.
     * If null, the mapping is identity (1:1).
     */
    val ttsToOriginalMap: IntArray? = null,
) {
    /**
     * Maps an index from TTS-space to Original-space.
     *
     * [ttsIndex] may be an exclusive end index (one past the last character), in which case the
     * original exclusive end is returned rather than clamping to the last character.
     */
    fun mapTtsToOriginal(ttsIndex: Int): Int {
        val map = ttsToOriginalMap ?: return ttsIndex
        if (map.isEmpty()) return 0
        if (ttsIndex >= map.size) return map.last() + 1
        return map[ttsIndex.coerceAtLeast(0)]
    }

    /**
     * Maps an index from Original-space to TTS-space.
     */
    fun mapOriginalToTts(originalIndex: Int): Int {
        val map = ttsToOriginalMap ?: return originalIndex
        val found = map.indexOfFirst { it >= originalIndex }
        return if (found != -1) found else text.length
    }

    /**
     * Expands the reported TTS-space range to whole words without ever shrinking it.
     *
     * Some engines report sub-word ranges (e.g. "92", "year", "old" for "92-year-old"), which would
     * otherwise highlight only part of the word at a time; those are widened to the containing word.
     * Ranges that already span multiple words (e.g. a date like "April 10, 2026") are preserved
     * rather than truncated to the first word.
     *
     * Returns an empty range when [ttsEnd] does not extend past [ttsStart] or when there is no word
     * at [ttsStart].
     */
    fun expandToWordRange(ttsStart: Int, ttsEnd: Int): IntRange {
        if (text.isEmpty() || ttsEnd <= ttsStart) return IntRange.EMPTY

        var anchor = ttsStart.coerceIn(0, text.length - 1)
        // If the reported range begins on whitespace, move onto the next word.
        while (anchor < text.length && text[anchor].isWhitespace()) anchor++
        if (anchor >= text.length) return IntRange.EMPTY

        var start = anchor
        while (start > 0 && !text[start - 1].isWhitespace()) start--

        // Snap the end up to the end of the word containing the last reported character, but never
        // pull it back before the reported end so multi-word ranges are kept intact.
        var end = ttsEnd.coerceIn(start, text.length)
        val lastIndex = end - 1
        if (lastIndex in 0 until text.length && !text[lastIndex].isWhitespace()) {
            var expandedEnd = lastIndex
            while (expandedEnd < text.length && !text[expandedEnd].isWhitespace()) expandedEnd++
            end = expandedEnd
        }
        if (end <= start) {
            end = anchor
            while (end < text.length && !text[end].isWhitespace()) end++
        }

        return start until end
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeakableText

        if (text != other.text) return false
        if (ttsToOriginalMap != null) {
            if (other.ttsToOriginalMap == null) return false
            if (!ttsToOriginalMap.contentEquals(other.ttsToOriginalMap)) return false
        } else if (other.ttsToOriginalMap != null) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (ttsToOriginalMap?.contentHashCode() ?: 0)
        return result
    }
}
