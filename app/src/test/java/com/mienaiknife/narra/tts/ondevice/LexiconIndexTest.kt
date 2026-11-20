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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.text.Normalizer

class LexiconIndexTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `known and unknown single words`() {
        val index = LexiconIndex.fromWords(listOf("her", "technician", "it's"))

        assertTrue(index.isKnown("her"))
        assertTrue(index.isKnown("technician"))
        assertTrue(index.isKnown("it's"))
        assertFalse(index.isKnown("balaji"))
        assertFalse(index.knows("andreessen"))
    }

    @Test
    fun `lookup is case insensitive and NFC normalized`() {
        val index = LexiconIndex.fromWords(listOf("café", "don't"))

        assertTrue(index.isKnown("CAFÉ"))
        assertTrue(index.isKnown("Café"))
        assertTrue(index.isKnown(Normalizer.normalize("café", Normalizer.Form.NFD)))
        assertTrue(index.isKnown("Don't"))
    }

    @Test
    fun `surrounding punctuation is stripped like the native lookup`() {
        val index = LexiconIndex.fromWords(listOf("hello", "don't"))

        assertTrue(index.isKnown("(hello)"))
        assertTrue(index.isKnown("\"hello,\""))
        assertTrue(index.isKnown("don't."))
    }

    @Test
    fun `phrase components are known as phrase words`() {
        val index = LexiconIndex.fromWords(
            singleWords = listOf("hello"),
            phraseWords = listOf("baton", "rouge"),
        )

        assertTrue(index.isKnown("hello"))
        assertFalse(index.isPhraseWord("hello"))

        assertTrue(index.isPhraseWord("Baton"))
        assertTrue(index.isPhraseWord("rouge"))
        assertFalse(index.isKnown("baton"))
        assertTrue(index.knows("baton"))
        assertTrue(index.knows("rouge"))
    }

    @Test
    fun `loading a lexicon file skips comments and splits multi word keys`() {
        val file = tempFolder.newFile("lexicon.txt")
        file.writeText(
            """
            # Narra lexicon supplement v1
            hello || h ə lˈ o ʊ
            Baton rouge || bˌ æ t ə n ɹˈ uː ʒ

            """.trimIndent(),
        )

        val index = LexiconIndex.load(file)

        assertTrue(index.isKnown("hello"))
        assertFalse(index.isKnown("baton"))
        assertTrue(index.isPhraseWord("baton"))
        assertTrue(index.isPhraseWord("rouge"))
        assertTrue(index.knows("Baton"))
        assertFalse(index.isKnown("baton rouge"))
        assertFalse(index.isKnown("#"))
    }

    @Test
    fun `an empty index knows nothing`() {
        val index = LexiconIndex.fromWords(emptyList())

        assertFalse(index.isKnown("hello"))
        assertFalse(index.isKnown(""))
        assertFalse(index.knows(" "))
    }

    @Test
    fun `loading a missing lexicon fails`() {
        assertThrows(IOException::class.java) {
            LexiconIndex.load(File(tempFolder.root, "missing.txt"))
        }
    }
}
