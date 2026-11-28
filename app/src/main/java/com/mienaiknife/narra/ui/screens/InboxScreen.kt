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
package com.mienaiknife.narra.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.EmptyState
import com.mienaiknife.narra.ui.components.NarraPullToRefreshBox
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.OverflowMenu
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.components.TopLevelScreen
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.InboxViewModel

@Composable
fun InboxScreen(
    onNavigateToFeeds: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is InboxViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
            }
        }
    }

    InboxScreenContent(
        articles = uiState.articles,
        isRefreshing = uiState.isRefreshing,
        sortOption = uiState.sortOption,
        showPlayed = uiState.showPlayed,
        playbackSpeed = uiState.playbackSpeed,
        downloadingArticleIds = uiState.downloadingArticleIds,
        onAddToQueue = { viewModel.addToQueue(it) },
        onMarkAsPlayedClick = { viewModel.togglePlayedStatus(it) },
        onClearInbox = { viewModel.clearInbox() },
        onRefresh = { viewModel.refresh() },
        onSortOptionSelected = { viewModel.setSortOption(it) },
        onShowPlayedChange = { viewModel.setShowPlayed(it) },
        onEditFeeds = onNavigateToFeeds,
    )
}

@Composable
fun InboxScreenContent(
    articles: List<Article>,
    isRefreshing: Boolean = false,
    sortOption: SortOption = SortOption.DATE_DESC,
    showPlayed: Boolean = false,
    playbackSpeed: Float = 1.0f,
    downloadingArticleIds: Set<String> = emptySet(),
    onAddToQueue: (Article) -> Unit,
    onMarkAsPlayedClick: (Article) -> Unit = {},
    onClearInbox: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {},
    onShowPlayedChange: (Boolean) -> Unit = {},
    onEditFeeds: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = {
                onSortOptionSelected(it)
                showSortSheet.value = false
            },
            showPlayed = showPlayed,
            onShowPlayedChange = onShowPlayedChange,
            onDismissRequest = { showSortSheet.value = false },
        )
    }

    TopLevelScreen(
        title = stringResource(R.string.nav_inbox),
        headerActions = {
            OverflowMenu(
                expanded = showMenu,
                onExpandedChange = { showMenu = it },
                contentDescription = stringResource(R.string.action_menu),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_sort)) },
                    onClick = {
                        showMenu = false
                        showSortSheet.value = true
                    },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_refresh)) },
                    onClick = {
                        showMenu = false
                        onRefresh()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_clear)) },
                    onClick = {
                        showMenu = false
                        onClearInbox()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.inbox_menu_edit_feeds)) },
                    onClick = {
                        showMenu = false
                        onEditFeeds()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.RssFeed,
                            contentDescription = null,
                        )
                    },
                )
            }
        },
    ) {
        NarraPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (articles.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Inbox,
                    title = stringResource(R.string.inbox_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            } else {
                val scrollState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(articles, key = { it.id }) { article ->
                            QueueItem(
                                article = article,
                                isPlaying = false,
                                playbackSpeed = playbackSpeed,
                                isDownloading = downloadingArticleIds.contains(article.id),
                                modifier = Modifier.animateItem(),
                                onPlayPauseClick = { onAddToQueue(article) },
                                onAddToQueueClick = { onAddToQueue(article) },
                                onMarkAsPlayedClick = { onMarkAsPlayedClick(article) },
                            )
                        }
                    }

                    NarraScrollbar(
                        lazyListState = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun InboxScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                InboxScreenContent(
                    articles = SampleArticles.all.filter { it.isFromFeed },
                    onAddToQueue = {},
                )
            }
        }
    }
}
