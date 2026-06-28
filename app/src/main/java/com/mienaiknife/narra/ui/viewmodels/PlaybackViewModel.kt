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
import com.mienaiknife.narra.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel
@Inject
constructor(
    private val playbackManager: PlaybackManager,
) : ViewModel() {
    val uiState: StateFlow<PlaybackUiState> =
        combine(
            playbackManager.currentArticle,
            playbackManager.isPlaying,
            playbackManager.currentPosition,
            playbackManager.duration,
        ) { currentArticle, isPlaying, currentPosition, duration ->
            val articleToShow = if (currentArticle?.isInQueue == true || isPlaying) currentArticle else null
            PlaybackUiState(
                currentArticle = articleToShow,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackUiState(),
        )

    fun togglePlayPause() = playbackManager.togglePlayPause()
}
