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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesUiState
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesViewModel

@Composable
fun FeedArticlesScreen(
    onBack: () -> Unit,
    viewModel: FeedArticlesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is FeedArticlesViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FeedArticlesScreenContent(
            uiState = uiState,
            onBackClick = onBack,
            onAddToQueue = { viewModel.addToQueue(it) },
            onDeleteArticle = { viewModel.deleteArticle(it) },
            onRefresh = { viewModel.refresh() },
            onSortOptionSelected = { viewModel.setSortOption(it) },
            onShowPlayedChange = { viewModel.setShowPlayed(it) },
            onMarkAllAsPlayed = { viewModel.markAllAsPlayed() },
            onMarkAllAsUnplayed = { viewModel.markAllAsUnplayed() }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedArticlesScreenContent(
    uiState: FeedArticlesUiState,
    onBackClick: () -> Unit,
    onAddToQueue: (Article) -> Unit,
    onDeleteArticle: (Article) -> Unit,
    onRefresh: () -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {},
    onShowPlayedChange: (Boolean) -> Unit = {},
    onMarkAllAsPlayed: () -> Unit = {},
    onMarkAllAsUnplayed: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = uiState.sortOption,
            onOptionSelected = onSortOptionSelected,
            showPlayed = uiState.showPlayed,
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = uiState.feedTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.action_menu),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_search)) },
                        onClick = { showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_sort)) },
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
                        text = { Text(stringResource(R.string.action_refresh)) },
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
                        text = { Text(stringResource(R.string.action_mark_all_played)) },
                        onClick = {
                            showMenu = false
                            onMarkAllAsPlayed()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DoneAll, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_mark_all_unplayed)) },
                        onClick = {
                            showMenu = false
                            onMarkAllAsUnplayed()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.RemoveDone, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (uiState.articles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.feed_articles_empty_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.articles) { article ->
                        QueueItem(
                            article = article,
                            isPlaying = false,
                            playbackSpeed = uiState.playbackSpeed,
                            isDownloading = article.id in uiState.downloadingArticleIds,
                            onAddToQueueClick = { onAddToQueue(article) },
                            onRemoveClick = { onDeleteArticle(article) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun FeedArticlesScreenPreview() {
    val navController = rememberNavController()
    val sampleArticles = listOf(
        Article(
            id = "1",
            title = "Apple announces new M4 Pro and M4 Max chips",
            source = "The Verge",
            publishedAt = "2 hours ago"
        ),
        Article(
            id = "2",
            title = "Android 15 is here: everything you need to know",
            source = "Android Central",
            publishedAt = "5 hours ago"
        ),
        Article(
            id = "3",
            title = "Google Search is getting a major AI overhaul",
            source = "9to5Google",
            publishedAt = "1 day ago"
        )
    )
    val uiState = FeedArticlesUiState(
        articles = sampleArticles,
        feedTitle = "The Verge"
    )

    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                FeedArticlesScreenContent(
                    uiState = uiState,
                    onBackClick = {},
                    onAddToQueue = {},
                    onDeleteArticle = {}
                )
            }
        }
    }
}

