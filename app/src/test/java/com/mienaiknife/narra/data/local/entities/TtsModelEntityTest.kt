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

package com.mienaiknife.narra.data.local.entities

import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsModelEntityTest {

    @Test
    fun `extraUrls roundtrip handles colons in URLs`() {
        val originalExtraUrls = mapOf(
            "voices.bin" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/voices.bin",
            "config.json" to "http://example.com/config"
        )
        
        val model = TtsModel(
            id = "test-model",
            name = "Test Model",
            language = "en",
            description = "Test Description",
            type = TtsModelType.KOKORO,
            modelUrl = "https://example.com/model.tar.bz2",
            extraUrls = originalExtraUrls
        )
        
        val entity = TtsModelEntity.fromDomain(model)
        println("Serialized extraUrls: ${entity.extraUrls}")
        val domain = entity.toDomain()
        
        assertEquals(originalExtraUrls, domain.extraUrls)
    }
}
