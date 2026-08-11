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
package com.mienaiknife.narra.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState
import com.mienaiknife.narra.utils.DateUtils
import java.util.Locale

@Composable
fun ReaderPlaybackControls(
    modifier: Modifier = Modifier,
    uiState: ReaderUiState,
    isControlsVisible: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleSpeed: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 0.dp, bottom = 16.dp),
            ) {
                // Progress Bar
                val progress = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
                val playbackProgressDesc = stringResource(R.string.reader_playback_progress_desc, (progress * 100).toInt())
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = uiState.currentPosition.toFloat(),
                                range = 0f..uiState.duration.toFloat(),
                                steps = 100,
                            )
                            contentDescription = playbackProgressDesc
                        },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    gapSize = 5.dp,
                    drawStopIndicator = {},
                )

                // Time Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val nominalDuration = uiState.article?.duration ?: remember(uiState.article?.content) { DateUtils.estimateReadingTimeMs(uiState.article?.content ?: "") }
                    val scaledTotalDuration = (nominalDuration / uiState.playbackSpeed).toLong()
                    val scaledCurrentPosition = (progress * scaledTotalDuration).toLong()
                    val scaledRemainingTime = scaledTotalDuration - scaledCurrentPosition

                    val inProgress = progress > 0f && progress < 1f
                    if (inProgress) {
                        Text(
                            text = DateUtils.formatElapsedTime(scaledCurrentPosition, scaledTotalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "-${DateUtils.formatElapsedTime(scaledRemainingTime, scaledTotalDuration)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        if (progress >= 1f && scaledTotalDuration > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = DateUtils.formatElapsedTime(scaledTotalDuration, scaledTotalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Speed Button
                    val playbackSpeedDesc = stringResource(R.string.reader_playback_speed_desc, uiState.playbackSpeed)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCycleSpeed()
                            },
                            modifier = Modifier
                                .height(64.dp)
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = playbackSpeedDesc
                                },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f", uiState.playbackSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Rewind
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipBackward()
                            },
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = stringResource(R.string.reader_rewind_desc, uiState.rewindSkipTime),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = uiState.rewindSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Play/Pause
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlayPause()
                            },
                            modifier = Modifier.size(64.dp),
                        ) {
                            if (uiState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Forward
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipForward()
                            },
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.reader_fast_forward_desc, uiState.fastForwardSkipTime),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = uiState.fastForwardSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Next
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.reader_next_article_desc),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
