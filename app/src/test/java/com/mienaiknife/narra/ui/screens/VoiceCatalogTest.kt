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
package com.mienaiknife.narra.ui.screens

import com.mienaiknife.narra.data.models.TtsModelCatalog
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCatalogTest {
    private fun model(
        id: String,
        name: String,
        language: String,
    ) = TtsModel(
        id = id,
        name = name,
        language = language,
        type = TtsModelType.VITS,
        modelUrl = "",
    )

    @Test
    fun `groups are sorted by language and voices by name`() {
        val groups = groupModelsByLanguage(
            listOf(
                model("es-2", "Zeta", "es"),
                model("en-2", "beta", "en"),
                model("en-1", "Alpha", "en"),
                model("fr-1", "Gamma", "fr"),
            ),
        ) { it }

        assertEquals(listOf("en", "es", "fr"), groups.map { it.language })
        assertEquals(listOf("Alpha", "beta"), groups.first().models.map { it.name })
    }

    @Test
    fun `sections are ordered by the display name, not the language tag`() {
        val groups = groupModelsByLanguage(
            listOf(
                model("de-1", "Thorsten", "de"),
                model("en-1", "Amy", "en"),
            ),
        ) { language -> if (language == "de") "German" else "English" }

        assertEquals(listOf("en", "de"), groups.map { it.language })
    }

    @Test
    fun `filtering keeps only the requested language`() {
        val models = listOf(
            model("en-1", "Amy", "en"),
            model("es-1", "Dave", "es"),
            model("en-2", "Ryan", "en"),
        )

        assertEquals(listOf("en-1", "en-2"), filterModelsByLanguage(models, "en").map { it.id })
        assertEquals(models, filterModelsByLanguage(models, null))
    }

    @Test
    fun `blank languages fall into the undetermined bucket`() {
        val groups = groupModelsByLanguage(listOf(model("x", "Voice", ""))) { it }

        assertEquals("und", groups.single().language)
    }

    @Test
    fun `kokoro exposes curated voice names`() {
        val voices = curatedVoicesFor("kokoro-en-v0_19")

        assertEquals(11, voices.size)
        assertTrue(voices.any { it.id == 0 && it.name == "Julia" })
        assertTrue(voices.any { it.id == 5 && it.name == "Amelie" })
    }

    @Test
    fun `kokoro speaker count matches the curated voice list`() {
        val kokoro = TtsModelCatalog.defaultModels.single { it.id == "kokoro-en-v0_19" }

        assertEquals(kokoro.voice.speakerCount, curatedVoicesFor(kokoro.id).size)
    }

    @Test
    fun `other models have no curated voice list`() {
        assertTrue(curatedVoicesFor("vits-piper-en_US-amy-low").isEmpty())
        assertTrue(curatedVoicesFor(null).isEmpty())
    }
}
