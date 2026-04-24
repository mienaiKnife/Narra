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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.mienaiknife.narra.ui.viewmodels.QueueViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun QueueScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is QueueViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        QueueScreenContent(
            articles = uiState.articles,
            isRefreshing = uiState.isRefreshing,
            currentArticle = uiState.currentArticle,
            isPlaying = uiState.isPlaying,
            sortOption = uiState.sortOption,
            keepSorted = uiState.keepSorted,
            totalRemainingTimeMs = uiState.totalRemainingTimeMs,
            downloadingArticleIds = uiState.downloadingArticleIds,
            onPlayPauseClick = { article -> viewModel.onPlayPauseClick(article) },
            onMarkAsPlayedClick = { article -> viewModel.togglePlayedStatus(article) },
            onRemoveFromQueue = { article ->
                viewModel.removeFromQueue(article)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Removed from queue",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.addToQueue(article.id)
                    }
                }
            },
            onHistoryClick = onNavigateToHistory,
            onClearQueue = { viewModel.clearQueue() },
            onRefresh = { viewModel.refresh() },
            onSortOptionSelected = { viewModel.setSortOption(it) },
            onKeepSortedChange = { viewModel.setKeepSorted(it) },
            onReorder = { from, to -> viewModel.reorderQueue(from, to) }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreenContent(
    articles: List<Article>,
    isRefreshing: Boolean = false,
    currentArticle: Article? = null,
    isPlaying: Boolean = false,
    sortOption: SortOption = SortOption.DATE_DESC,
    keepSorted: Boolean = false,
    totalRemainingTimeMs: Long = 0L,
    downloadingArticleIds: Set<String> = emptySet(),
    onPlayPauseClick: (Article) -> Unit = {},
    onMarkAsPlayedClick: (Article) -> Unit = {},
    onRemoveFromQueue: (Article) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {},
    onKeepSortedChange: (Boolean) -> Unit = {},
    onReorder: (Int, Int) -> Unit = { _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    val totalHours = TimeUnit.MILLISECONDS.toHours(totalRemainingTimeMs)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalRemainingTimeMs) % 60

    val timeLeftText = buildString {
        append("Time left: ")
        if (totalHours > 0) {
            append(totalHours)
            append(" ")
            append(if (totalHours == 1L) "hour" else "hours")
            append(" and ")
        }
        append(totalMinutes)
        append(" ")
        append(if (totalMinutes == 1L) "minute" else "minutes")
    }

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = {
                onSortOptionSelected(it)
                showSortSheet.value = false
            },
            onDismissRequest = { showSortSheet.value = false },
            isQueue = true,
            keepSorted = keepSorted,
            onKeepSortedChange = onKeepSortedChange
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
                text = "Queue",
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
                        text = { Text("History") },
                        onClick = {
                            showMenu = false
                            onHistoryClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.History, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear") },
                        onClick = {
                            showMenu = false
                            onClearQueue()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                }
            }
        }

        Text(
            text = buildString {
                append(articles.size)
                append(" ")
                append(if (articles.size == 1) "text" else "texts")
                append(" • ")
                append(timeLeftText)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = articles,
                        key = { _, article -> article.id }
                    ) { index, article ->
                        val isDragged = index == draggedItemIndex
                        val offset = if (isDragged) IntOffset(0, draggingOffset.toInt()) else IntOffset.Zero
                        val zIndex = if (isDragged) 1f else 0f

                        QueueItem(
                            article = article,
                            isPlaying = isPlaying && currentArticle?.id == article.id,
                            isDownloading = downloadingArticleIds.contains(article.id),
                            modifier = Modifier
                                .offset { offset }
                                .zIndex(zIndex)
                                .animateItem()
                                .fillMaxWidth(),
                            onPlayPauseClick = { onPlayPauseClick(article) },
                            onMarkAsPlayedClick = { onMarkAsPlayedClick(article) },
                            onRemoveClick = { onRemoveFromQueue(article) },
                            dragModifier = Modifier.pointerInput(articles) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        draggedItemIndex = index
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount.y

                                        val currentDraggedIndex = draggedItemIndex
                                        if (currentDraggedIndex == -1) return@detectDragGestures
                                        val itemHeight = lazyListState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == currentDraggedIndex }?.size ?: 0

                                        if (itemHeight > 0) {
                                            val targetIndex = when {
                                                draggingOffset > itemHeight / 2 -> currentDraggedIndex + 1
                                                draggingOffset < -itemHeight / 2 -> currentDraggedIndex - 1
                                                else -> currentDraggedIndex
                                            }

                                            if (targetIndex in articles.indices && targetIndex != currentDraggedIndex) {
                                                onReorder(currentDraggedIndex, targetIndex)
                                                draggedItemIndex = targetIndex
                                                draggingOffset = 0f
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = -1
                                        draggingOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = -1
                                        draggingOffset = 0f
                                    }
                                )
                            }
                        )
                    }
                }

                NarraScrollbar(
                    lazyListState = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun QueueScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                QueueScreenContent(
                    articles = SampleArticles.all.filter { it.isInQueue },
                    onHistoryClick = {}
                )
            }
        }
    }
}
