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

import com.mienaiknife.narra.domain.models.SherpaVoiceParams
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.repository.CustomPronunciationRepository
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SherpaTtsModelConfigTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var engine: SherpaTtsEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        engine = SherpaTtsEngine(
            mock<ModelRepository>(),
            mock<PlaybackSettingsManager>(),
            mock<CustomPronunciationRepository>(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(type: TtsModelType) = TtsModel(
        id = "model",
        name = "Model",
        language = "en",
        type = type,
        modelUrl = "",
        isDownloaded = true,
    )

    private val params = SherpaVoiceParams(
        noiseScale = 0.4f,
        noiseScaleW = 0.7f,
        lengthScale = 1.3f,
        cpuThreads = 2,
    )

    @Test
    fun `vits receives its tunable values and thread count`() {
        val config = engine.createModelConfig(model(TtsModelType.VITS), "/models/voice", "/lex/lexicon.txt", params)

        assertEquals(0.4f, config.vits.noiseScale, 0.001f)
        assertEquals(0.7f, config.vits.noiseScaleW, 0.001f)
        assertEquals(1.3f, config.vits.lengthScale, 0.001f)
        assertEquals("/lex/lexicon.txt", config.vits.lexicon)
        assertEquals(2, config.numThreads)
    }

    @Test
    fun `matcha receives noise and length but has no noise W`() {
        val config = engine.createModelConfig(model(TtsModelType.MATCHA), "/models/voice", "/lex/lexicon.txt", params)

        assertEquals(0.4f, config.matcha.noiseScale, 0.001f)
        assertEquals(1.3f, config.matcha.lengthScale, 0.001f)
        assertEquals(2, config.numThreads)
    }

    @Test
    fun `kokoro only receives length scale`() {
        val config = engine.createModelConfig(model(TtsModelType.KOKORO), "/models/voice", "/lex/lexicon.txt", params)

        assertEquals(1.3f, config.kokoro.lengthScale, 0.001f)
        assertEquals(2, config.numThreads)
    }

    @Test
    fun `zipvoice and supertonic ignore knobs they do not support`() {
        val zipvoice = engine.createModelConfig(model(TtsModelType.ZIPVOICE), "/models/voice", "/lex/lexicon.txt", params)
        val supertonic = engine.createModelConfig(model(TtsModelType.SUPERTONIC), "/models/voice", "/lex/lexicon.txt", params)

        assertEquals(2, zipvoice.numThreads)
        assertEquals(2, supertonic.numThreads)
        assertEquals("/models/voice/encoder.onnx", zipvoice.zipvoice.encoder)
        assertEquals("/models/voice/tts.json", supertonic.supertonic.ttsJson)
    }
}
