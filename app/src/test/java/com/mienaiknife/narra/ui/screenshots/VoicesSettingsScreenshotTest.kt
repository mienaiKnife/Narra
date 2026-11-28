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
package com.mienaiknife.narra.ui.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import com.mienaiknife.narra.ui.screens.VoicesSettingsContent
import com.mienaiknife.narra.ui.viewmodels.VoicesSettingsUiState
import org.junit.Rule
import org.junit.Test

class VoicesSettingsScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
        )

    private fun model(
        id: String,
        name: String,
        language: String,
        type: TtsModelType,
        voice: VoiceProfile = VoiceProfile(
            quality = VoiceQuality.MEDIUM,
            gender = VoiceGender.FEMALE,
            locale = "en-US",
        ),
    ) = TtsModel(
        id = id,
        name = name,
        language = language,
        voice = voice,
        type = type,
        modelUrl = "",
        isDownloaded = true,
    )

    private val vits = model(
        id = "vits-piper-en_US-amy-low",
        name = "Piper Amy",
        language = "en",
        type = TtsModelType.VITS,
        voice = VoiceProfile(
            quality = VoiceQuality.LOW,
            gender = VoiceGender.FEMALE,
            locale = "en-US",
        ),
    )

    private val kokoro = model(
        id = "kokoro-en-v0_19",
        name = "Kokoro",
        language = "en",
        type = TtsModelType.KOKORO,
        voice = VoiceProfile(
            quality = VoiceQuality.HIGH,
            gender = VoiceGender.MIXED,
            locale = "en-US",
            speakerCount = 11,
        ),
    )

    @Test
    fun voicesSettings_Vits() {
        paparazzi.snapshot {
            SnapshotSurface {
                VoicesSettingsContent(
                    uiState = VoicesSettingsUiState(
                        availableModels = listOf(vits),
                        selectedEngine = "ondevice",
                        selectedModelId = vits.id,
                        engineState = TtsState.Ready,
                    ),
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onCancelDownload = {},
                    onDeleteModel = {},
                    onSetVoiceNoiseScale = {},
                    onSetVoiceNoiseScaleW = {},
                    onSetVoiceLengthScale = {},
                    onSetVoicePauseScale = {},
                    onSetVoiceExtraPause = {},
                    onSetVoiceCpuThreads = {},
                    onResetVoiceParams = {},
                    onNavigateToPronunciations = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun voicesSettings_KokoroDark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                VoicesSettingsContent(
                    uiState = VoicesSettingsUiState(
                        availableModels = listOf(kokoro),
                        selectedEngine = "ondevice",
                        selectedModelId = kokoro.id,
                        engineState = TtsState.Ready,
                    ),
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onCancelDownload = {},
                    onDeleteModel = {},
                    onSetVoiceNoiseScale = {},
                    onSetVoiceNoiseScaleW = {},
                    onSetVoiceLengthScale = {},
                    onSetVoicePauseScale = {},
                    onSetVoiceExtraPause = {},
                    onSetVoiceCpuThreads = {},
                    onResetVoiceParams = {},
                    onNavigateToPronunciations = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun voicesSettings_MultiLanguageCatalog() {
        val models = listOf(
            vits,
            model("vits-piper-en_US-ryan-medium", "Piper Ryan", "en", TtsModelType.VITS),
            model("matcha-en-ljspeech", "Matcha Icefall", "en", TtsModelType.MATCHA),
            kokoro,
            model("vits-es_ES-davefx-medium", "Piper Dave", "es", TtsModelType.VITS),
            model("vits-es_ES-sharvard-medium", "Piper Sharvard", "es", TtsModelType.VITS),
            model("vits-fr_FR-siwis-medium", "Piper Siwis", "fr", TtsModelType.VITS),
            model("vits-de_DE-thorsten-medium", "Piper Thorsten", "de", TtsModelType.VITS),
            model("vits-it_IT-paola-medium", "Piper Paola", "it", TtsModelType.VITS),
            model("vits-pt_BR-faber-medium", "Piper Faber", "pt", TtsModelType.VITS),
            model("vits-zh_CN-huayan-medium", "Piper Huayan", "zh", TtsModelType.VITS),
            model("vits-nl_NL-mls-medium", "Piper MLS", "nl", TtsModelType.VITS),
        )
        paparazzi.snapshot {
            SnapshotSurface {
                VoicesSettingsContent(
                    uiState = VoicesSettingsUiState(
                        availableModels = models,
                        selectedEngine = "ondevice",
                        selectedModelId = vits.id,
                        engineState = TtsState.Ready,
                    ),
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onCancelDownload = {},
                    onDeleteModel = {},
                    onSetVoiceNoiseScale = {},
                    onSetVoiceNoiseScaleW = {},
                    onSetVoiceLengthScale = {},
                    onSetVoicePauseScale = {},
                    onSetVoiceExtraPause = {},
                    onSetVoiceCpuThreads = {},
                    onResetVoiceParams = {},
                    onNavigateToPronunciations = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun voicesSettings_NoModelSelected() {
        paparazzi.snapshot {
            SnapshotSurface {
                VoicesSettingsContent(
                    uiState = VoicesSettingsUiState(
                        availableModels = emptyList(),
                        selectedEngine = "ondevice",
                        selectedModelId = null,
                        engineState = TtsState.Idle,
                    ),
                    onSetEngine = {},
                    onSelectModel = {},
                    onSetSpeakerId = {},
                    onDownloadModel = {},
                    onCancelDownload = {},
                    onDeleteModel = {},
                    onSetVoiceNoiseScale = {},
                    onSetVoiceNoiseScaleW = {},
                    onSetVoiceLengthScale = {},
                    onSetVoicePauseScale = {},
                    onSetVoiceExtraPause = {},
                    onSetVoiceCpuThreads = {},
                    onResetVoiceParams = {},
                    onNavigateToPronunciations = {},
                    onBack = {},
                )
            }
        }
    }
}
