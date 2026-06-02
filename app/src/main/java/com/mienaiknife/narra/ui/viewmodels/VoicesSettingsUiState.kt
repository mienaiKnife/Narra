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
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.ui.UiText

data class VoicesSettingsUiState(
    val availableModels: List<TtsModel> = emptyList(),
    val selectedEngine: String = "android",
    val selectedModelId: String? = null,
    val selectedSpeakerId: Int = 0,
    val sherpaSpeed: Float = 1.0f,
    val sherpaNoiseScale: Float = 0.667f,
    val sherpaLengthScale: Float = 1.0f,
    val engineState: TtsState = TtsState.Idle,
    val errorMessage: UiText? = null
)
