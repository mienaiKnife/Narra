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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.utils.DateUtils

@Composable
fun QueueItem(
    article: Article,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    onMarkAsPlayedClick: () -> Unit = {},
    dragModifier: Modifier? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        QueueItemRow(
            article = article,
            isPlaying = isPlaying,
            onItemClick = onItemClick,
            onLongClick = { showMenu = true },
            onPlayPauseClick = onPlayPauseClick,
            dragModifier = dragModifier
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (article.progress == 1f) "Mark as unplayed"
                        else "Mark as played"
                    )
                },
                onClick = {
                    onMarkAsPlayedClick()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                }
            )
            if (article.isInQueue) {
                DropdownMenuItem(
                    text = { Text("Remove from queue") },
                    onClick = {
                        onRemoveClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = {
                        onAddToQueueClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null
                        )
                    }
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
    onItemClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    dragModifier: Modifier? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragModifier != null && article.isInQueue) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 48.dp)
                    .then(dragModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onLongClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                article.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val sourceText = buildString {
                    val formattedDate = DateUtils.formatPublishedDate(article.publishedAt)
                    if (formattedDate != null) {
                        append(formattedDate)
                        append(" • ")
                    }
                    append(article.source)
                }
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (article.progress == 1f) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = if (article.progress == 1f) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val progress = article.progress ?: 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (article.id == "3") "0:42" else if (article.id == "2") "0:46" else "5:07",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (progress > 0f && progress < 1f) {
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
                            drawStopIndicator = {}
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (article.id == "2") "-0:46" else "-5:07",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (progress == 1f) {
                        Text(
                            text = " • Played",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            val (icon, contentDescription) = when {
                !article.isInQueue -> Icons.AutoMirrored.Outlined.PlaylistAdd to "Add to playlist"
                isPlaying -> Icons.Default.Pause to "Pause"
                else -> Icons.Default.PlayArrow to "Play"
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919
)
@Composable
fun QueueItemPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            QueueItem(
                article = SampleArticles.sampleArticle1,
                isPlaying = false,
            )
        }
    }
}
