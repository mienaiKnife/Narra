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
package com.mienaiknife.narra.data.repositories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LexiconSupplementTest {
    private val entries = requireNotNull(LexiconSupplement.forLanguage("en"))

    @Test
    fun `only english has a supplement for now`() {
        assertNotNull(LexiconSupplement.forLanguage("en-US"))
        assertNotNull(LexiconSupplement.forLanguage("EN_gb"))
        assertNull(LexiconSupplement.forLanguage("fr"))
        assertNull(LexiconSupplement.forLanguage(""))
    }

    @Test
    fun `the confirmed lexicon holes are covered`() {
        listOf(
            "her",
            "or",
            "per",
            "seem",
            "technician",
            "organise",
            "organised",
            "organises",
            "organising",
            "customise",
            "customised",
            "customises",
            "customising",
            "enquiries",
        ).forEach { word ->
            assertTrue("supplement is missing '$word'", entries.containsKey(word))
        }
    }

    @Test
    fun `entries use lowercase keys and lexicon line syntax`() {
        entries.forEach { (word, phones) ->
            assertEquals("key must be lowercase: $word", word.lowercase(Locale.ROOT), word)
            assertTrue("phones must not be empty: $word", phones.isNotBlank())
            assertFalse("phones must not contain the key separator: $word", phones.contains("||"))
            assertFalse("key must be a single word: $word", word.contains(' '))
        }
    }

    @Test
    fun `pronunciations are copied from related lexicon entries`() {
        // Values copied from the live lexicon: ore || ˈ oː ɹ, purr || pˈ ɜː, seam || sˈ iː m,
        // herd || hˈ ɜː d, technicians || t ɛ k nˈ ɪ ʃ ə n z,
        // organize || ˈ ɔː ɹ ɡ ə nˌ a ɪ z, customizes || kˈ ʌ s t ə mˌ a ɪ z ᵻ z,
        // enquiry || ɛ ŋ k wˈ a ɪ ɚ ɹ i.
        assertEquals("ˈ oː ɹ", entries.getValue("or"))
        assertEquals("pˈ ɜː", entries.getValue("per"))
        assertEquals("sˈ iː m", entries.getValue("seem"))
        assertEquals("hˈ ɜː", entries.getValue("her"))
        assertEquals("t ɛ k nˈ ɪ ʃ ə n", entries.getValue("technician"))
        assertEquals("ˈ ɔː ɹ ɡ ə nˌ a ɪ z", entries.getValue("organise"))
        assertEquals("ˈ ɔː ɹ ɡ ə nˌ a ɪ z d", entries.getValue("organised"))
        assertEquals("ˈ ɔː ɹ ɡ ə nˌ a ɪ z ɪ ŋ", entries.getValue("organising"))
        assertEquals("kˈ ʌ s t ə mˌ a ɪ z", entries.getValue("customise"))
        assertEquals("kˈ ʌ s t ə mˌ a ɪ z ᵻ z", entries.getValue("customises"))
        assertEquals("ɛ ŋ k wˈ a ɪ ɚ ɹ i z", entries.getValue("enquiries"))
    }
}
