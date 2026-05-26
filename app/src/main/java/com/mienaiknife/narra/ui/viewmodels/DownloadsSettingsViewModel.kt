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
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.settings.DownloadSettingsManager
import com.mienaiknife.narra.data.settings.SyncSettingsManager
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.ui.UiText
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
    private val syncSettingsManager: SyncSettingsManager,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _message = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<DownloadsSettingsUiState> = combine(
        downloadSettingsManager.downloadOverWifiOnly,
        downloadSettingsManager.refreshInterval,
        syncSettingsManager.autoExportEnabled,
        syncSettingsManager.autoImportEnabled,
        syncSettingsManager.autoExportUri,
        syncSettingsManager.lastExportTimestamp,
        syncSettingsManager.pendingImport,
        _message
    ) { args: Array<Any?> ->
        DownloadsSettingsUiState(
            downloadOverWifiOnly = args[0] as Boolean,
            refreshInterval = args[1] as String,
            autoExportEnabled = args[2] as Boolean,
            autoImportEnabled = args[3] as Boolean,
            autoExportUri = args[4] as String?,
            lastExportTimestamp = args[5] as Long,
            pendingImport = args[6] as Boolean,
            message = args[7] as UiText?
        )
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

    fun setRefreshInterval(interval: String) {
        viewModelScope.launch {
            downloadSettingsManager.setRefreshInterval(interval)
        }
    }

    fun setAutoExportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            syncSettingsManager.setAutoExportEnabled(enabled)
        }
    }

    fun setAutoExportUri(uri: String?) {
        viewModelScope.launch {
            syncSettingsManager.setAutoExportUri(uri)
        }
    }

    fun setAutoImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            syncSettingsManager.setAutoImportEnabled(enabled)
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
                        _message.value = UiText.StringResource(R.string.message_imported_feeds, count)
                    }
                    .onFailure { error ->
                        _message.value = UiText.StringResource(R.string.message_import_failed, error.message ?: "")
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
                        _message.value = UiText.StringResource(R.string.message_exported_feeds)
                    }
                    .onFailure { error ->
                        _message.value = UiText.StringResource(R.string.message_export_failed, error.message ?: "")
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
                        _message.value = UiText.StringResource(R.string.message_backup_created)
                    }
                    .onFailure { error ->
                        _message.value = UiText.StringResource(R.string.message_backup_failed, error.message ?: "")
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
                        _message.value = UiText.StringResource(R.string.message_db_restored)
                    }
                    .onFailure { error ->
                        _message.value = UiText.StringResource(R.string.message_restore_failed, error.message ?: "")
                    }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
