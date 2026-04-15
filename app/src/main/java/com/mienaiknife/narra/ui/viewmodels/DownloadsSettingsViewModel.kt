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
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.map

data class DownloadsSettingsUiState(
    val downloadOverWifiOnly: Boolean = true
)

@HiltViewModel
class DownloadsSettingsViewModel @Inject constructor(
    private val downloadSettingsManager: DownloadSettingsManager,
    private val contentRepository: ContentRepository
) : ViewModel() {

    val uiState: StateFlow<DownloadsSettingsUiState> = downloadSettingsManager.downloadOverWifiOnly
        .map { DownloadsSettingsUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadsSettingsUiState()
        )

    fun setDownloadOverWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            downloadSettingsManager.setDownloadOverWifiOnly(enabled)
        }
    }

    fun deleteAllMetadata() {
        viewModelScope.launch {
            contentRepository.deleteAllMetadata()
        }
    }

    fun deleteAllFeeds() {
        viewModelScope.launch {
            contentRepository.deleteAllFeeds()
        }
    }
}
