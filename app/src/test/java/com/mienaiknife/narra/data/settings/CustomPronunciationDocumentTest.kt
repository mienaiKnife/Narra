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
package com.mienaiknife.narra.data.settings

import com.mienaiknife.narra.domain.models.CustomPronunciation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPronunciationDocumentTest {
    @Test
    fun `document round trips entries`() {
        val document =
            CustomPronunciationDocument(
                entries =
                listOf(
                    CustomPronunciationEntry(word = "Narra", arpabet = "N AA R AH"),
                    CustomPronunciationEntry(
                        word = "tomato",
                        arpabet = "T AH M EY T OW",
                        language = "en-US",
                    ),
                ),
            )

        val decoded = CustomPronunciationDocument.decode(document.encode())

        assertEquals(document, decoded)
    }

    @Test
    fun `default document is version one with no entries`() {
        val document = CustomPronunciationDocument()

        assertEquals(1, document.version)
        assertEquals(CustomPronunciationDocument.CURRENT_VERSION, document.version)
        assertTrue(document.entries.isEmpty())
        assertEquals(document, CustomPronunciationDocument.decode(null))
        assertEquals(document, CustomPronunciationDocument.decode(""))
        assertEquals(document, CustomPronunciationDocument.decode("   "))
    }

    @Test
    fun `decode ignores unknown fields for forward compatibility`() {
        val json =
            """
            {
              "version": 1,
              "entries": [
                {
                  "word": "Narra",
                  "arpabet": "N AA R AH",
                  "language": "en",
                  "futureEntryField": true
                }
              ],
              "futureDocumentField": "ignored"
            }
            """.trimIndent()

        val document = CustomPronunciationDocument.decode(json)

        assertEquals(1, document.version)
        assertEquals(1, document.entries.size)
        assertEquals("Narra", document.entries.first().word)
    }

    @Test
    fun `corrupt json decodes to an empty document`() {
        assertEquals(
            CustomPronunciationDocument(),
            CustomPronunciationDocument.decode("{ this is not json"),
        )
        assertEquals(
            CustomPronunciationDocument(),
            CustomPronunciationDocument.decode("""{"version":1,"entries":[{"word":"Narra"}]}"""),
        )
    }

    @Test
    fun `entries map to and from the domain model`() {
        val domain =
            CustomPronunciation(
                word = "Custom Word",
                arpabet = "K AH S T AH M W ER D",
                language = "en",
            )

        val entry = CustomPronunciationEntry.fromDomain(domain)

        assertEquals("Custom Word", entry.word)
        assertEquals("K AH S T AH M W ER D", entry.arpabet)
        assertEquals("en", entry.language)
        assertEquals(domain, entry.toDomain())

        val entries = listOf(domain)
        assertEquals(entries, CustomPronunciationDocument.fromDomain(entries).toDomain())
    }

    @Test
    fun `entry language defaults to english`() {
        assertEquals(
            CustomPronunciation.DEFAULT_LANGUAGE,
            CustomPronunciationEntry(word = "Narra", arpabet = "N AA R AH").language,
        )
    }
}
