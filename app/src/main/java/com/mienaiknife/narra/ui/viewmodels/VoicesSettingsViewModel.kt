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
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.repository.ModelRepository
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import com.mienaiknife.narra.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoicesSettingsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager,
    private val ttsEngine: TtsEngine,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<UiText?>(null)

    init {
        viewModelScope.launch {
            modelRepository.ensureDefaultModelsInitialized()
        }
    }

    private data class VoicesData(
        val models: List<TtsModel>,
        val engine: String,
        val modelId: String?,
        val speakerId: Int,
        val noiseScale: Float,
    )

    val uiState: StateFlow<VoicesSettingsUiState> = combine(
        combine(
            modelRepository.getAvailableModels(),
            settingsManager.ttsEngine,
            settingsManager.ttsModelId,
            settingsManager.ttsSpeakerId,
            settingsManager.sherpaNoiseScale,
            ::VoicesData,
        ),
        combine(
            settingsManager.sherpaLengthScale,
            ttsEngine.state,
            _errorMessage,
        ) { lengthScale, engineState, errorMessage ->
            Triple(lengthScale, engineState, errorMessage)
        },
    ) { data, engineDetails ->
        val (lengthScale, engineState, errorMessage) = engineDetails
        VoicesSettingsUiState(
            availableModels = data.models,
            selectedEngine = data.engine,
            selectedModelId = data.modelId,
            selectedSpeakerId = data.speakerId,
            sherpaNoiseScale = data.noiseScale,
            sherpaLengthScale = lengthScale,
            engineState = engineState,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoicesSettingsUiState(),
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

    fun setSpeakerId(speakerId: Int) {
        viewModelScope.launch {
            settingsManager.setTtsSpeakerId(speakerId)
        }
    }

    fun downloadModel(modelId: String) {
        modelRepository.enqueueDownload(modelId)
    }

    fun cancelDownload(modelId: String) {
        modelRepository.cancelDownload(modelId)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            val currentSelected = settingsManager.ttsModelId.first()
            if (currentSelected == modelId) {
                settingsManager.setTtsModelId(null)
            }
            modelRepository.deleteModel(modelId)
        }
    }

    fun setSherpaNoiseScale(noiseScale: Float) {
        viewModelScope.launch {
            settingsManager.setSherpaNoiseScale(noiseScale)
        }
    }

    fun setSherpaLengthScale(lengthScale: Float) {
        viewModelScope.launch {
            settingsManager.setSherpaLengthScale(lengthScale)
        }
    }
}
