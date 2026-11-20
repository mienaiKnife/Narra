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
import org.junit.Assert.assertNull
import org.junit.Test

class ModelLexiconsTest {
    @Test
    fun `english maps to the en-us lexicon`() {
        assertEquals("lexicon-en-us.txt", ModelLexicons.fileNameFor("en"))
    }

    @Test
    fun `language tags are reduced to their primary subtag`() {
        listOf("en-US", "en_us", "EN", " en ").forEach { language ->
            assertEquals(
                "language '$language' should use the English lexicon",
                "lexicon-en-us.txt",
                ModelLexicons.fileNameFor(language),
            )
        }
    }

    @Test
    fun `languages without a sourced lexicon return null`() {
        assertNull(ModelLexicons.fileNameFor("fr"))
        assertNull(ModelLexicons.fileNameFor(""))
        assertNull(ModelLexicons.urlFor("de"))
    }

    @Test
    fun `lexicon url points at the sherpa tts-models release`() {
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/lexicon-en-us.txt",
            ModelLexicons.urlFor("en-GB"),
        )
    }
}
