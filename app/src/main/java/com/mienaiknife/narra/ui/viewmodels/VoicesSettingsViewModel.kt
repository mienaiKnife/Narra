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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoicesSettingsUiState(
    val availableModels: List<TtsModel> = emptyList(),
    val selectedEngine: String = "android",
    val selectedModelId: String? = null
)

@HiltViewModel
class VoicesSettingsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager
) : ViewModel() {

    val uiState: StateFlow<VoicesSettingsUiState> = combine(
        modelRepository.getAvailableModels(),
        settingsManager.ttsEngine,
        settingsManager.ttsModelId
    ) { models, engine, modelId ->
        VoicesSettingsUiState(models, engine, modelId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoicesSettingsUiState()
    )

    fun setEngine(engine: String) {
        viewModelScope.launch {
            settingsManager.setTtsEngine(engine)
        }
    }

    fun selectModel(modelId: String?) {
        viewModelScope.launch {
            settingsManager.setTtsModelId(modelId)
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.downloadModel(modelId)
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.deleteModel(modelId)
        }
    }
}
