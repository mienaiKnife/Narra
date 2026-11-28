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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.EmptyState
import com.mienaiknife.narra.ui.components.NarraPullToRefreshBox
import com.mienaiknife.narra.ui.components.OverflowMenu
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesUiState
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesViewModel

@Composable
fun FeedArticlesScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: FeedArticlesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    FeedArticlesScreenContent(
        uiState = uiState,
        onBackClick = onBack,
        onAddToQueue = { viewModel.addToQueue(it) },
        onDeleteArticle = { viewModel.deleteArticle(it) },
        onRefresh = { viewModel.refresh() },
        onSortOptionSelected = { viewModel.setSortOption(it) },
        onShowPlayedChange = { viewModel.setShowPlayed(it) },
        onMarkAllAsPlayed = { viewModel.markAllAsPlayed() },
        onMarkAllAsUnplayed = { viewModel.markAllAsUnplayed() },
    )
}

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
    onMarkAllAsUnplayed: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = uiState.sortOption,
            onOptionSelected = onSortOptionSelected,
            showPlayed = uiState.showPlayed,
            onShowPlayedChange = onShowPlayedChange,
            onDismissRequest = { showSortSheet.value = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ScreenHeader(
            title = uiState.feedTitle,
            onBack = onBackClick,
        ) {
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
                    text = { Text(stringResource(R.string.action_mark_all_played)) },
                    onClick = {
                        showMenu = false
                        onMarkAllAsPlayed()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                        )
                    },
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_mark_all_unplayed)) },
                    onClick = {
                        showMenu = false
                        onMarkAllAsUnplayed()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.RemoveDone,
                            contentDescription = null,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NarraPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (uiState.articles.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Article,
                    title = stringResource(R.string.feed_articles_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(uiState.articles, key = { it.id }) { article ->
                        QueueItem(
                            article = article,
                            isPlaying = false,
                            playbackSpeed = uiState.playbackSpeed,
                            isDownloading = article.id in uiState.downloadingArticleIds,
                            onAddToQueueClick = { onAddToQueue(article) },
                            onRemoveClick = { onDeleteArticle(article) },
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun FeedArticlesScreenPreview() {
    val navController = rememberNavController()
    val sampleArticles = listOf(
        Article(
            id = "1",
            title = "Apple announces new M4 Pro and M4 Max chips",
            source = "The Verge",
            publishedAt = "2 hours ago",
        ),
        Article(
            id = "2",
            title = "Android 15 is here: everything you need to know",
            source = "Android Central",
            publishedAt = "5 hours ago",
        ),
        Article(
            id = "3",
            title = "Google Search is getting a major AI overhaul",
            source = "9to5Google",
            publishedAt = "1 day ago",
        ),
    )
    val uiState = FeedArticlesUiState(
        articles = sampleArticles,
        feedTitle = "The Verge",
    )

    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                FeedArticlesScreenContent(
                    uiState = uiState,
                    onBackClick = {},
                    onAddToQueue = {},
                    onDeleteArticle = {},
                )
            }
        }
    }
}
