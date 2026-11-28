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
package com.mienaiknife.narra.data.models

import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TtsModelCatalogTest {
    @Test
    fun `every shipped model has a complete voice profile`() {
        TtsModelCatalog.defaultModels.forEach { model ->
            assertNotEquals(model.id, VoiceQuality.UNKNOWN, model.voice.quality)
            assertNotEquals(model.id, VoiceGender.UNSPECIFIED, model.voice.gender)
            assertTrue(model.id, model.voice.locale.isNotBlank())
            assertTrue(model.id, model.voice.speakerCount >= 1)
        }
    }

    @Test
    fun `the display locale matches the lexicon language`() {
        TtsModelCatalog.defaultModels.forEach { model ->
            val locale = Locale.forLanguageTag(model.voice.locale)
            assertEquals(model.id, model.language, locale.language)
        }
    }

    @Test
    fun `model ids are unique`() {
        val ids = TtsModelCatalog.defaultModels.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
