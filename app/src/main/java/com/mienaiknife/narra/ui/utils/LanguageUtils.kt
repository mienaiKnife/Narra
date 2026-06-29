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
package com.mienaiknife.narra.ui.utils

import android.icu.text.Transliterator

object LanguageUtils {
    private const val TRANSLITERATOR_ID = "Any-Latin; Latin-ASCII"
    private var transliterator: Transliterator? = null

    /**
     * Checks if the text contains any non-Latin characters that might need transliteration.
     */
    fun needsTransliteration(text: String): Boolean {
        for (char in text) {
            if (isNonLatin(char)) return true
        }
        return false
    }

    private fun isNonLatin(char: Char): Boolean {
        val block = Character.UnicodeBlock.of(char)
        return block != Character.UnicodeBlock.BASIC_LATIN &&
            block != Character.UnicodeBlock.LATIN_1_SUPPLEMENT &&
            block != Character.UnicodeBlock.LATIN_EXTENDED_A &&
            block != Character.UnicodeBlock.LATIN_EXTENDED_B &&
            char.isLetter()
    }

    /**
     * Transliterates non-Latin text to Latin characters and provides a mapping back to original indices.
     */
    fun transliterateWithMapping(text: String): Pair<String, IntArray> {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || !needsTransliteration(text)) {
            return text to IntArray(text.length) { it }
        }

        val result = StringBuilder()
        val mapping = mutableListOf<Int>()

        var i = 0
        while (i < text.length) {
            if (!isNonLatin(text[i])) {
                result.append(text[i])
                mapping.add(i)
                i++
            } else {
                // Segment non-latin block
                var j = i
                while (j < text.length && isNonLatin(text[j])) j++

                val block = text.substring(i, j)
                val translit = transliterateToLatin(block)

                for (k in translit.indices) {
                    result.append(translit[k])
                    // Map to the approximate original character within the block
                    val originalOffset = i + (k * block.length / translit.length).coerceAtMost(block.length - 1)
                    mapping.add(originalOffset)
                }
                i = j
            }
        }
        return result.toString() to mapping.toIntArray()
    }

    /**
     * Transliterates non-Latin text to Latin characters.
     */
    private fun transliterateToLatin(text: String): String {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            return text
        }
        
        if (transliterator == null) {
            try {
                transliterator = Transliterator.getInstance(TRANSLITERATOR_ID)
            } catch (e: Exception) {
                android.util.Log.e("LanguageUtils", "Failed to get Transliterator", e)
                return text
            }
        }
        return transliterator?.transliterate(text) ?: text
    }
}
