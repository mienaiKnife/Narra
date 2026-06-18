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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.PlaybackViewModel

@Composable
fun MiniPlayer(
    onExpand: (String) -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MiniPlayerContent(
        article = uiState.currentArticle,
        isPlaying = uiState.isPlaying,
        currentPosition = uiState.currentPosition,
        duration = uiState.duration,
        onExpand = onExpand,
        onTogglePlayPause = { viewModel.togglePlayPause() },
    )
}

@Composable
fun MiniPlayerContent(
    article: Article?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onExpand: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    if (article == null) return

    val contentDesc = if (isPlaying) {
        stringResource(R.string.reader_playing_prefix, article.title)
    } else {
        stringResource(R.string.reader_paused_prefix, article.title)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
            }
            .clickable { onExpand(article.id) },
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val imageUrl = article.imageUrl ?: article.feedImageUrl ?: article.url?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }
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
                        contentDescription = stringResource(R.string.reader_cover_desc, article.title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageLoaded = true },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical),
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Progress Bar
            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                gapSize = 5.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
)
@Composable
fun MiniPlayerPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            MiniPlayerContent(
                article = SampleArticles.sampleArticle1,
                isPlaying = false,
                currentPosition = 45000L,
                duration = 180000L,
                onExpand = {},
                onTogglePlayPause = {},
            )
        }
    }
}
