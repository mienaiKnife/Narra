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

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.InboxViewModel

@Composable
fun InboxScreen(
    onNavigateToFeeds: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is InboxViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            onEditFeeds = onNavigateToFeeds
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onEditFeeds: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = {
                onSortOptionSelected(it)
                showSortSheet.value = false
            },
            showPlayed = showPlayed,
            onShowPlayedChange = onShowPlayedChange,
            onDismissRequest = { showSortSheet.value = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inbox",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Search") },
                        onClick = { showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort") },
                        onClick = {
                            showMenu = false
                            showSortSheet.value = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh") },
                        onClick = {
                            showMenu = false
                            onRefresh()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Refresh, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear") },
                        onClick = {
                            showMenu = false
                            onClearInbox()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit feeds") },
                        onClick = {
                            showMenu = false
                            onEditFeeds()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.RssFeed, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (articles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No new articles from your feeds.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val scrollState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
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
                                onMarkAsPlayedClick = { onMarkAsPlayedClick(article) }
                            )
                        }
                    }

                    NarraScrollbar(
                        lazyListState = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun InboxScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                InboxScreenContent(
                    articles = SampleArticles.all.filter { it.isFromFeed },
                    onAddToQueue = {}
                )
            }
        }
    }
}
