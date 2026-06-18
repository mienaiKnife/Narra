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
package com.mienaiknife.narra.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.utils.DateUtils

@Composable
fun QueueItem(
    article: Article,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isDownloading: Boolean = false,
    playbackSpeed: Float = 1.0f,
    showRemainingTime: Boolean = true,
    onPlayPauseClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    onMarkAsPlayedClick: () -> Unit = {},
    dragModifier: Modifier? = null,
) {
    var showMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        QueueItemRow(
            article = article,
            isPlaying = isPlaying,
            isDownloading = isDownloading,
            playbackSpeed = playbackSpeed,
            showRemainingTime = showRemainingTime,
            onClick = { showMenu = true },
            onPlayPauseClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (article.isInQueue) onPlayPauseClick() else onAddToQueueClick()
            },
            dragModifier = dragModifier,
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (article.progress == 1f) {
                            stringResource(R.string.reader_menu_unplayed)
                        } else {
                            stringResource(R.string.reader_menu_played)
                        },
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkAsPlayedClick()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                },
            )
            if (article.isInQueue) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_menu_remove_queue)) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemoveClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_menu_add_queue)) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddToQueueClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null,
                        )
                    },
                )
            }
            if (!article.url.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_menu_visit_site)) },
                    onClick = {
                        uriHandler.openUri(article.url)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItemRow(
    article: Article,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isDownloading: Boolean = false,
    playbackSpeed: Float = 1.0f,
    showRemainingTime: Boolean = true,
    onClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    dragModifier: Modifier? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dragModifier != null && article.isInQueue) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 48.dp)
                    .then(dragModifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = stringResource(R.string.action_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        val progressPercent = ((article.progress ?: 0f) * 100).toInt()
        val itemContentDescription = if (article.isInQueue) {
            pluralStringResource(R.plurals.queue_item_semantics_queue_desc, progressPercent, article.title, article.source, progressPercent)
        } else {
            stringResource(R.string.queue_item_semantics_desc, article.title, article.source)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = itemContentDescription
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onClick,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = article.localImageUrl ?: article.imageUrl ?: article.feedImageUrl ?: article.url?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }
                var isImageLoaded by remember(imageUrl) { mutableStateOf(false) }

                if (!isImageLoaded) {
                    val placeholderIcon = when {
                        article.url?.startsWith("epub://") == true -> Icons.Default.AutoStories
                        article.isFromFeed -> Icons.Default.RssFeed
                        else -> Icons.Default.Language
                    }
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }

                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.home_cover_image_desc, article.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (article.progress == 1f) 0.6f else 1f,
                    onSuccess = { isImageLoaded = true },
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
                verticalArrangement = Arrangement.spacedBy(
                    LocalNarraSpacing.current.itemVertical,
                    Alignment.CenterVertically,
                ),
            ) {
                val sourceText = buildString {
                    val formattedDate = DateUtils.formatPublishedDate(article.publishedAt)
                    if (formattedDate != null) {
                        append(formattedDate)
                        append(" • ")
                    }
                    append(article.source)
                }

                val baseColor = if (article.progress == 1f) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                val variantColor = if (article.progress == 1f) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant

                AdaptiveText(
                    text = sourceText,
                    style = MaterialTheme.typography.bodySmall.copy(color = variantColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AdaptiveText(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium.copy(color = baseColor),
                    maxLines = if (article.isInQueue) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )

                val nominalDuration = article.duration ?: remember(article.content) { DateUtils.estimateReadingTimeMs(article.content) }
                val totalDuration = (nominalDuration / playbackSpeed).toLong()
                val progress = article.progress ?: 0f
                val currentPosition = (progress * totalDuration).toLong()
                val remainingTime = totalDuration - currentPosition

                if (totalDuration > 0 && article.isInQueue) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val inProgress = progress > 0f && progress < 1f

                        if (inProgress) {
                            Text(
                                text = DateUtils.formatElapsedTime(currentPosition, totalDuration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                gapSize = 5.dp,
                                drawStopIndicator = {},
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (showRemainingTime) {
                                    "-${DateUtils.formatElapsedTime(remainingTime, totalDuration)}"
                                } else {
                                    DateUtils.formatElapsedTime(totalDuration, totalDuration)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            if (progress >= 1f) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text(
                                text = DateUtils.formatElapsedTime(totalDuration, totalDuration),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (article.progress == 1f) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .padding(end = 16.dp)
                .semantics {
                    val context = context
                    contentDescription = when {
                        !article.isInQueue -> context.getString(R.string.action_add_to_queue_desc, article.title)
                        isPlaying -> context.getString(R.string.action_pause_desc, article.title)
                        else -> context.getString(R.string.action_play_desc, article.title)
                    }
                },
            enabled = !isDownloading,
        ) {
            val (icon, resId) = when {
                !article.isInQueue -> Icons.AutoMirrored.Outlined.PlaylistAdd to R.string.reader_menu_add_queue
                isPlaying -> Icons.Default.Pause to R.string.action_pause
                else -> Icons.Default.PlayArrow to R.string.action_play
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(resId),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
)
@Composable
fun QueueItemPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            QueueItem(
                article = SampleArticles.sampleArticle1,
                isPlaying = false,
                dragModifier = Modifier,
            )
        }
    }
}
