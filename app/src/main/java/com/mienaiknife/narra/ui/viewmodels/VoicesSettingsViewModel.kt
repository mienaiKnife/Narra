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
import com.mienaiknife.narra.domain.models.SherpaVoiceOverrides
import com.mienaiknife.narra.domain.models.SherpaVoiceParams
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.models.defaultVoiceParams
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
            settingsManager.migrateLegacyVoiceSettings()
        }
    }

    private data class VoicesData(
        val models: List<TtsModel>,
        val engine: String,
        val modelId: String?,
        val overrides: Map<String, SherpaVoiceOverrides>,
    )

    val uiState: StateFlow<VoicesSettingsUiState> = combine(
        combine(
            modelRepository.getAvailableModels(),
            settingsManager.ttsEngine,
            settingsManager.ttsModelId,
            settingsManager.sherpaVoiceOverrides,
            ::VoicesData,
        ),
        ttsEngine.state,
        _errorMessage,
    ) { data, engineState, errorMessage ->
        val modelType = data.models.find { it.id == data.modelId }?.type
        VoicesSettingsUiState(
            availableModels = data.models,
            selectedEngine = data.engine,
            selectedModelId = data.modelId,
            voiceParams = resolveVoiceParams(data.modelId, modelType, data.overrides),
            engineState = engineState,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoicesSettingsUiState(),
    )

    private fun resolveVoiceParams(
        modelId: String?,
        modelType: TtsModelType?,
        overrides: Map<String, SherpaVoiceOverrides>,
    ): SherpaVoiceParams {
        val defaults = modelType?.defaultVoiceParams() ?: SherpaVoiceParams()
        val modelOverrides = modelId?.let { overrides[it] } ?: SherpaVoiceOverrides()
        return modelOverrides.applyTo(defaults)
    }

    fun setEngine(engine: String) {
        viewModelScope.launch {
            settingsManager.setTtsEngine(engine)
        }
    }

    fun selectModel(modelId: String?) {
        viewModelScope.launch {
            settingsManager.setTtsModelId(modelId)
            // Covers upgrades where no model was selected on first launch: adopt the old global
            // tuning for the first model the user picks.
            if (modelId != null) settingsManager.migrateLegacyVoiceSettings()
        }
    }

    fun setSpeakerId(speakerId: Int) {
        val modelId = uiState.value.selectedModelId ?: return
        viewModelScope.launch {
            settingsManager.setVoiceSpeaker(modelId, speakerId)
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
            settingsManager.resetVoiceParams(modelId)
        }
    }

    fun setVoiceNoiseScale(value: Float) = updateVoiceParams { modelId ->
        settingsManager.setVoiceNoiseScale(modelId, value)
    }

    fun setVoiceNoiseScaleW(value: Float) = updateVoiceParams { modelId ->
        settingsManager.setVoiceNoiseScaleW(modelId, value)
    }

    fun setVoiceLengthScale(value: Float) = updateVoiceParams { modelId ->
        settingsManager.setVoiceLengthScale(modelId, value)
    }

    fun setVoicePauseScale(value: Float) = updateVoiceParams { modelId ->
        settingsManager.setVoicePauseScale(modelId, value)
    }

    fun setVoiceExtraPause(value: Int) = updateVoiceParams { modelId ->
        settingsManager.setVoiceExtraPause(modelId, value)
    }

    fun setVoiceCpuThreads(value: Int) = updateVoiceParams { modelId ->
        settingsManager.setVoiceCpuThreads(modelId, value)
    }

    fun resetVoiceParams() = updateVoiceParams { modelId ->
        settingsManager.resetVoiceParams(modelId)
    }

    private fun updateVoiceParams(action: suspend (String) -> Unit) {
        val modelId = uiState.value.selectedModelId ?: return
        viewModelScope.launch { action(modelId) }
    }
}
