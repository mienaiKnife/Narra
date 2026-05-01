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
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoicesSettingsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsManager: PlaybackSettingsManager,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            modelRepository.ensureDefaultModelsInitialized()
        }
    }

    val uiState: StateFlow<VoicesSettingsUiState> = combine(
        modelRepository.getAvailableModels(),
        settingsManager.ttsEngine,
        settingsManager.ttsModelId,
        settingsManager.ttsSpeakerId,
        settingsManager.sherpaSpeed,
        settingsManager.sherpaNoiseScale,
        settingsManager.sherpaLengthScale,
        ttsEngine.state,
        _errorMessage
    ) { args ->
        val models = args[0] as List<TtsModel>
        val engine = args[1] as String
        val modelId = args[2] as String?
        val speakerId = args[3] as Int
        val speed = args[4] as Float
        val noiseScale = args[5] as Float
        val lengthScale = args[6] as Float
        val engineState = args[7] as TtsState
        val errorMessage = args[8] as String?
        VoicesSettingsUiState(models, engine, modelId, speakerId, speed, noiseScale, lengthScale, engineState, errorMessage)
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
            modelRepository.deleteModel(modelId)
        }
    }

    fun setSherpaSpeed(speed: Float) {
        viewModelScope.launch {
            settingsManager.setSherpaSpeed(speed)
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
