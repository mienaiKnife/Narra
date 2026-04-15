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
import com.mienaiknife.narra.playback.PlaybackSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.combine

data class PlaybackSettingsUiState(
    val fastForwardSkipTime: String = "30s",
    val rewindSkipTime: String = "10s",
    val pauseOnDisconnect: Boolean = true,
    val pauseForInterruptions: Boolean = true,
    val autoPlayNext: Boolean = true
)

@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor(
    private val settingsManager: PlaybackSettingsManager
) : ViewModel() {

    val uiState: StateFlow<PlaybackSettingsUiState> = combine(
        settingsManager.fastForwardSkipTime,
        settingsManager.rewindSkipTime,
        settingsManager.pauseOnDisconnect,
        settingsManager.pauseForInterruptions,
        settingsManager.autoPlayNext
    ) { ff, rw, pod, pfi, apn ->
        PlaybackSettingsUiState(ff, rw, pod, pfi, apn)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaybackSettingsUiState()
    )

    fun setFastForwardSkipTime(time: String) {
        viewModelScope.launch {
            settingsManager.setFastForwardSkipTime(time)
        }
    }

    fun setRewindSkipTime(time: String) {
        viewModelScope.launch {
            settingsManager.setRewindSkipTime(time)
        }
    }

    fun setPauseOnDisconnect(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setPauseOnDisconnect(enabled)
        }
    }

    fun setPauseForInterruptions(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setPauseForInterruptions(enabled)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoPlayNext(enabled)
        }
    }
}
