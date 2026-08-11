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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.utils.DateUtils

@Composable
fun ReaderTopBar(
    modifier: Modifier = Modifier,
    article: Article,
    isControlsVisible: Boolean,
    onBack: () -> Unit,
    onMenuExpandChange: (Boolean) -> Unit,
    isMenuExpanded: Boolean,
    onSearchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepTimerClick: () -> Unit,
    sleepTimerMillis: Long?,
) {
    val uriHandler = LocalUriHandler.current

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                    )
                    val dateAndSource = buildString {
                        val formattedDate = DateUtils.formatPublishedDate(article.publishedAt)
                        if (formattedDate != null) {
                            append(formattedDate)
                            append(" • ")
                        }
                        append(article.source)
                    }
                    Text(
                        text = dateAndSource,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                    )
                }

                Box {
                    IconButton(onClick = { onMenuExpandChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.action_menu),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { onMenuExpandChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_search)) },
                            onClick = {
                                onMenuExpandChange(false)
                                onSearchClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (article.isFavorite) stringResource(R.string.reader_menu_unfavorite) else stringResource(R.string.reader_menu_favorite)) },
                            onClick = {
                                onMenuExpandChange(false)
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        val sleepTimerText = if (sleepTimerMillis != null && sleepTimerMillis > 0) {
                            stringResource(R.string.reader_menu_sleep_timer_active, DateUtils.formatElapsedTime(sleepTimerMillis))
                        } else {
                            stringResource(R.string.reader_sleep_timer)
                        }
                        DropdownMenuItem(
                            text = { Text(sleepTimerText) },
                            onClick = {
                                onMenuExpandChange(false)
                                onSleepTimerClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (sleepTimerMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_visit_site)) },
                            onClick = {
                                onMenuExpandChange(false)
                                article.url?.let { uriHandler.openUri(it) }
                            },
                            enabled = article.url != null,
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
