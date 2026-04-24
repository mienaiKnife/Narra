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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class DownloadsSettingsViewModel @Inject constructor(
    private val downloadSettingsManager: DownloadSettingsManager,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DownloadsSettingsUiState> = combine(
        downloadSettingsManager.downloadOverWifiOnly,
        _message
    ) { wifiOnly, message ->
        DownloadsSettingsUiState(wifiOnly, message)
    }.stateIn(
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

    fun deleteDatabase() {
        viewModelScope.launch {
            contentRepository.deleteAllMetadata()
            contentRepository.deleteAllFeeds()
        }
    }

    fun importOpml(inputStream: InputStream?) {
        if (inputStream == null) return
        viewModelScope.launch {
            inputStream.use {
                contentRepository.importOpml(it)
                    .onSuccess { count ->
                        _message.value = "Imported $count feeds"
                    }
                    .onFailure { error ->
                        _message.value = "Import failed: ${error.message}"
                    }
            }
        }
    }

    fun exportOpml(outputStream: OutputStream?) {
        if (outputStream == null) return
        viewModelScope.launch {
            outputStream.use {
                contentRepository.exportOpml(it)
                    .onSuccess {
                        _message.value = "Exported subscriptions"
                    }
                    .onFailure { error ->
                        _message.value = "Export failed: ${error.message}"
                    }
            }
        }
    }

    fun backupDatabase(outputStream: OutputStream?) {
        if (outputStream == null) return
        viewModelScope.launch {
            outputStream.use {
                contentRepository.backupDatabase(it)
                    .onSuccess {
                        _message.value = "Backup created"
                    }
                    .onFailure { error ->
                        _message.value = "Backup failed: ${error.message}"
                    }
            }
        }
    }

    fun restoreDatabase(inputStream: InputStream?) {
        if (inputStream == null) return
        viewModelScope.launch {
            inputStream.use {
                contentRepository.restoreDatabase(it)
                    .onSuccess {
                        _message.value = "Database restored. Restart app."
                    }
                    .onFailure { error ->
                        _message.value = "Restore failed: ${error.message}"
                    }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
