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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextPreparerTest {
    /**
     * A small stand-in for the merged English lexicon. It contains the supplement words, the
     * single letters the native engine can say, and the pieces the decomposition tests rely on.
     */
    private val vocabulary = LexiconIndex.fromWords(
        singleWords = listOf(
            "a", "an", "and", "answered", "busy", "car", "controllers", "controller",
            "companies", "company", "disagree", "download", "fact", "glasses", "her",
            "here", "is", "it", "it's", "known", "leave", "let's", "like", "met", "modal",
            "multi", "ness", "notes", "or", "people", "per", "purple", "seem", "smart",
            "take", "technician", "the", "to", "told", "two", "use", "we", "well", "woke",
            "able", "bot", "chat", "john", "seems",
        ),
    )

    private fun prepared(input: String): String = SpeechTextPreparer.prepare(input, vocabulary).text

    @Test
    fun `known text is unchanged`() {
        val text = "The technician seems busy."
        assertEquals(text, prepared(text))
    }

    @Test
    fun `supplement words are left for the native engine instead of being spelled`() {
        assertEquals("her or per seem technician", prepared("her or per seem technician"))
    }

    @Test
    fun `contractions are kept intact`() {
        assertEquals("It's here.", prepared("It's here."))
        assertEquals("Let's use it.", prepared("Let's use it."))
    }

    @Test
    fun `possessive uses a known plural`() {
        assertEquals("The companies car.", prepared("The company's car."))
    }

    @Test
    fun `plural possessive drops the apostrophe`() {
        assertEquals("The controllers car.", prepared("The controllers' car."))
    }

    @Test
    fun `possessive falls back to the known stem`() {
        // "John" is known but "Johns" is not, so the possessive is dropped rather than read "ess".
        assertEquals("john car.", prepared("John's car."))
    }

    @Test
    fun `unknown possessive spells the stem and never emits a separate s`() {
        assertEquals("b a l a j i car.", prepared("Balaji's car."))
        assertFalse(prepared("Balaji's car.").split(' ').contains("s"))
    }

    @Test
    fun `unknown names are spelled`() {
        assertEquals(
            "b a l a j i and a n d r e e s s e n disagree.",
            prepared("Balaji and Andreessen disagree."),
        )
    }

    @Test
    fun `short all caps acronyms are spelled`() {
        assertEquals("An L L M is here.", prepared("An LLM is here."))
    }

    @Test
    fun `unknown compounds are decomposed into known words`() {
        assertEquals("multi modal", prepared("multimodal"))
        assertEquals("woke ness", prepared("wokeness"))
        assertEquals("download able", prepared("downloadable"))
        assertEquals("smart glasses", prepared("smartglasses"))
        assertEquals("chat bot", prepared("chatbot"))
    }

    @Test
    fun `hyphenated words with known pieces are left alone`() {
        assertEquals("a well-known fact", prepared("a well-known fact"))
    }

    @Test
    fun `hyphenated words with an unknown piece are spoken separately`() {
        // The native frontend joins hyphen pieces, which would hide the spelled word.
        assertEquals("a b a l a j i like fact", prepared("a Balaji-like fact"))
    }

    @Test
    fun `components of multi word lexicon entries are not rewritten`() {
        val index = LexiconIndex.fromWords(
            singleWords = listOf("i", "am"),
            phraseWords = listOf("baton", "rouge"),
        )

        assertEquals("I am Baton rouge.", SpeechTextPreparer.prepare("I am Baton rouge.", index).text)
    }

    @Test
    fun `rewrites map back into the original word`() {
        val original = "Balaji's notes."
        val speech = SpeechTextPreparer.prepare(original, vocabulary)

        assertEquals("b a l a j i notes.", speech.text)
        val wordStart = original.indexOf("Balaji")
        val wordEnd = wordStart + "Balaji's".length
        for (index in 0 until "b a l a j i".length) {
            val range = speech.sourceRange(index, index + 1)
            assertNotNull("missing source mapping for rewritten character $index", range)
            assertTrue("range ${range!!} must stay inside the word", range.first >= wordStart)
            assertTrue("range $range must stay inside the word", range.last < wordEnd)
        }

        val notesStart = speech.text.indexOf("notes")
        val notesRange = speech.sourceRange(notesStart, notesStart + "notes".length)!!
        assertEquals("notes", original.substring(notesRange.first, notesRange.last + 1))
    }

    @Test
    fun `numbers are still expanded before the lexicon pass`() {
        assertEquals("met two people", prepared("met 2 people"))
    }

    @Test
    fun `no index returns the normalized text unchanged`() {
        val speech = SpeechTextPreparer.prepare("Balaji met 2 people.", index = null)

        assertEquals("Balaji met two people.", speech.text)
    }

    @Test
    fun `empty and whitespace input does not crash`() {
        assertEquals("", SpeechTextPreparer.prepare("", vocabulary).text)
        assertEquals("   ", SpeechTextPreparer.prepare("   ", vocabulary).text)
        assertEquals("", SpeechTextPreparer.prepare("", null).text)
    }

    @Test
    fun `very long words and un foldable letters are left alone`() {
        val longWord = "a".repeat(41)
        assertEquals(longWord, prepared(longWord))

        // Greek letters cannot be folded to ASCII and are left for the native frontend.
        assertEquals("Ελλάδα", prepared("Ελλάδα"))
    }
}
