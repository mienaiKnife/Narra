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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.viewmodels.FeedArticlesViewModel

@Composable
fun FeedArticlesScreen(
    onBack: () -> Unit,
    viewModel: FeedArticlesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is FeedArticlesViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
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
            onSortOptionSelected = { viewModel.setSortOption(it) },
            onShowPlayedChange = { viewModel.setShowPlayed(it) }
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
    uiState: com.mienaiknife.narra.ui.viewmodels.FeedArticlesUiState,
    onBackClick: () -> Unit,
    onAddToQueue: (Article) -> Unit,
    onDeleteArticle: (Article) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit = {},
    onShowPlayedChange: (Boolean) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        SortBottomSheet(
            selectedOption = uiState.sortOption,
            onOptionSelected = onSortOptionSelected,
            showPlayed = uiState.showPlayed,
            onShowPlayedChange = onShowPlayedChange,
            onDismissRequest = { showSortSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

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
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = uiState.feedTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }

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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort") },
                        onClick = {
                            showMenu = false
                            showSortSheet = true
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.articles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No articles found in this feed.",
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
                        isDownloading = article.id in uiState.downloadingArticleIds,
                        onAddToQueueClick = { onAddToQueue(article) },
                        onRemoveClick = { onDeleteArticle(article) }
                    )
                }
            }
        }
    }
}

