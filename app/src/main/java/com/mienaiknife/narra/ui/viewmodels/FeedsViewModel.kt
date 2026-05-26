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
import com.mienaiknife.narra.data.local.dao.FeedDao
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.repository.ContentRepository
import com.mienaiknife.narra.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class FeedsViewModel @Inject constructor(
    feedDao: FeedDao,
    private val repository: ContentRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.TITLE_ASC)
    private val _message = MutableStateFlow<UiText?>(null)
    val message = _message.asStateFlow()

    val uiState: StateFlow<FeedsUiState> = combine(
        feedDao.getAllFeeds(),
        _isRefreshing,
        _sortOption
    ) { feeds, isRefreshing, sort ->
        val sortedFeeds = when (sort) {
            SortOption.MANUAL -> feeds
            SortOption.DATE_DESC -> feeds.sortedByDescending { it.createdAt }
            SortOption.DATE_ASC -> feeds.sortedBy { it.createdAt }
            SortOption.TITLE_ASC -> feeds.sortedBy { it.title }
            SortOption.TITLE_DESC -> feeds.sortedByDescending { it.title }
            // For feeds, source sort doesn't make as much sense, but we can reuse the enum
            else -> feeds.sortedBy { it.title }
        }
        FeedsUiState(
            feeds = sortedFeeds,
            isRefreshing = isRefreshing,
            sortOption = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedsUiState()
    )

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            // Toggle order if the same category is clicked
            val next = when (option) {
                SortOption.DATE_DESC -> SortOption.DATE_ASC
                SortOption.DATE_ASC -> SortOption.DATE_DESC
                SortOption.TITLE_ASC -> SortOption.TITLE_DESC
                SortOption.TITLE_DESC -> SortOption.TITLE_ASC
                SortOption.SOURCE_ASC -> SortOption.SOURCE_DESC
                SortOption.SOURCE_DESC -> SortOption.SOURCE_ASC
                SortOption.MANUAL -> SortOption.MANUAL
            }
            _sortOption.value = next
        } else {
            _sortOption.value = option
        }
    }

    fun deleteFeed(feed: FeedEntity) {
        viewModelScope.launch {
            repository.deleteFeed(feed.url)
        }
    }

    fun toggleNotifications(feed: FeedEntity) {
        viewModelScope.launch {
            repository.updateFeed(feed.copy(notificationsEnabled = !feed.notificationsEnabled))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshFeeds()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun importOpml(inputStream: InputStream?) {
        if (inputStream == null) return
        viewModelScope.launch {
            inputStream.use {
                repository.importOpml(it)
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
                repository.exportOpml(it)
                    .onSuccess {
                        _message.value = UiText.StringResource(R.string.message_exported_feeds)
                    }
                    .onFailure { error ->
                        _message.value = UiText.StringResource(R.string.message_export_failed, error.message ?: "")
                    }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
