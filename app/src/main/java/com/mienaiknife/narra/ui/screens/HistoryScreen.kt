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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.EmptyState
import com.mienaiknife.narra.ui.components.NarraPullToRefreshBox
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.OverflowMenu
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.HistoryViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HistoryViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
            }
        }
    }

    HistoryScreenContent(
        articles = uiState.articles,
        isRefreshing = uiState.isRefreshing,
        isPlaying = uiState.isPlaying,
        playbackSpeed = uiState.playbackSpeed,
        currentArticleId = uiState.currentArticle?.id,
        downloadingArticleIds = uiState.downloadingArticleIds,
        onBackClick = onBack,
        onPlayPauseClick = { viewModel.onPlayPauseClick(it) },
        onAddToQueueClick = { viewModel.addToQueue(it) },
        onMarkAsPlayedClick = { viewModel.togglePlayedStatus(it) },
        onClearHistory = { viewModel.clearHistory() },
        onRefresh = { viewModel.refresh() },
    )
}

@Composable
fun HistoryScreenContent(
    articles: List<Article>,
    isRefreshing: Boolean = false,
    isPlaying: Boolean = false,
    playbackSpeed: Float = 1.0f,
    currentArticleId: String? = null,
    downloadingArticleIds: Set<String> = emptySet(),
    onBackClick: () -> Unit,
    onPlayPauseClick: (Article) -> Unit,
    onAddToQueueClick: (Article) -> Unit = {},
    onMarkAsPlayedClick: (Article) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ScreenHeader(
            title = stringResource(R.string.nav_history),
            onBack = onBackClick,
        ) {
            OverflowMenu(
                expanded = showMenu,
                onExpandedChange = { showMenu = it },
                contentDescription = stringResource(R.string.action_menu),
            ) {
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
                        onClearHistory()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NarraPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (articles.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.history_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            } else {
                val scrollState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(articles, key = { it.id }) { article ->
                            QueueItem(
                                article = article,
                                isPlaying = isPlaying && currentArticleId == article.id,
                                isDownloading = downloadingArticleIds.contains(article.id),
                                playbackSpeed = playbackSpeed,
                                modifier = Modifier.animateItem(),
                                onPlayPauseClick = { onPlayPauseClick(article) },
                                onAddToQueueClick = { onAddToQueueClick(article) },
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
fun HistoryScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HistoryScreenContent(
                    articles = listOf(SampleArticles.finishedArticle),
                    onBackClick = {},
                    onPlayPauseClick = {},
                )
            }
        }
    }
}
