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
package com.mienaiknife.narra.ui.viewmodels

import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.SherpaVoiceParams
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.VoiceCapabilities
import com.mienaiknife.narra.domain.models.defaultVoiceParams
import com.mienaiknife.narra.domain.models.voiceCapabilities
import com.mienaiknife.narra.ui.UiText

data class VoicesSettingsUiState(
    val availableModels: List<TtsModel> = emptyList(),
    val selectedEngine: String = "android",
    val selectedModelId: String? = null,
    val voiceParams: SherpaVoiceParams = SherpaVoiceParams(),
    val engineState: TtsState = TtsState.Idle,
    val errorMessage: UiText? = null,
) {
    val selectedModel: TtsModel? get() = availableModels.find { it.id == selectedModelId }

    val selectedModelType: TtsModelType? get() = selectedModel?.type

    val voiceCapabilities: VoiceCapabilities
        get() = selectedModelType?.voiceCapabilities ?: VoiceCapabilities()

    val defaultVoiceParams: SherpaVoiceParams
        get() = selectedModelType?.defaultVoiceParams() ?: SherpaVoiceParams()

    val hasCustomVoiceParams: Boolean get() = voiceParams != defaultVoiceParams
}
