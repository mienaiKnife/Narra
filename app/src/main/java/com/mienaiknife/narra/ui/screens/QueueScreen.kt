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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.mienaiknife.narra.ui.viewmodels.QueueViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun QueueScreen(
    onNavigateToHistory: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is QueueViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
            }
        }
    }

    val removedMessage = stringResource(R.string.queue_removed_message)
    val undoAction = stringResource(R.string.action_undo)

    when (val state = uiState) {
        is com.mienaiknife.narra.ui.viewmodels.QueueUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        is com.mienaiknife.narra.ui.viewmodels.QueueUiState.Success -> {
            QueueScreenContent(
                articles = state.articles,
                isRefreshing = state.isRefreshing,
                currentArticle = state.currentArticle,
                isPlaying = state.isPlaying,
                playbackSpeed = state.playbackSpeed,
                sortOption = state.sortOption,
                keepSorted = state.keepSorted,
                totalRemainingTimeMs = state.totalRemainingTimeMs,
                downloadingArticleIds = state.downloadingArticleIds,
                onPlayPauseClick = { article -> viewModel.onPlayPauseClick(article) },
                onMarkAsPlayedClick = { article -> viewModel.togglePlayedStatus(article) },
                onRemoveFromQueue = { article ->
                    viewModel.removeFromQueue(article)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = removedMessage,
                            actionLabel = undoAction,
                            duration = SnackbarDuration.Short,
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
                onReorder = { from, to -> viewModel.reorderQueue(from, to) },
            )
        }
    }
}

@Composable
fun QueueScreenContent(
    articles: List<Article>,
    isRefreshing: Boolean = false,
    currentArticle: Article? = null,
    isPlaying: Boolean = false,
    playbackSpeed: Float = 1.0f,
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
    onReorder: (Int, Int) -> Unit = { _, _ -> },
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    val totalHours = TimeUnit.MILLISECONDS.toHours(totalRemainingTimeMs)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalRemainingTimeMs) % 60

    val hoursText = if (totalHours > 0) pluralStringResource(R.plurals.unit_hours, totalHours.toInt(), totalHours) else ""
    val minutesText = pluralStringResource(R.plurals.unit_minutes, totalMinutes.toInt(), totalMinutes)
    val andText = stringResource(R.string.queue_and)
    val timeLeftPrefix = stringResource(R.string.queue_time_left_prefix)

    val timeLeftText = buildString {
        append(timeLeftPrefix)
        if (totalHours > 0) {
            append(hoursText)
            append(andText)
        }
        append(minutesText)
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
            onKeepSortedChange = onKeepSortedChange,
        )
    }

    val articlesCountText = pluralStringResource(R.plurals.unit_articles, articles.size, articles.size)
    val separator = stringResource(R.string.separator_bullet)
    val metaContent: (@Composable () -> Unit)? = if (articles.isNotEmpty()) {
        {
            Text(
                text = buildString {
                    append(articlesCountText)
                    append(separator)
                    append(timeLeftText)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    } else {
        null
    }

    TopLevelScreen(
        title = stringResource(R.string.nav_queue),
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
                    text = { Text(stringResource(R.string.nav_history)) },
                    onClick = {
                        showMenu = false
                        onHistoryClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_clear)) },
                    onClick = {
                        showMenu = false
                        onClearQueue()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )
            }
        },
        meta = metaContent,
    ) {
        NarraPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (articles.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = stringResource(R.string.queue_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        itemsIndexed(
                            items = articles,
                            key = { _, article -> article.id },
                        ) { index, article ->
                            val isDragged = index == draggedItemIndex
                            val offset =
                                if (isDragged) IntOffset(0, draggingOffset.toInt()) else IntOffset.Zero
                            val zIndex = if (isDragged) 1f else 0f
                            val currentIndex by rememberUpdatedState(index)
                            val currentArticles by rememberUpdatedState(articles)

                            QueueItem(
                                article = article,
                                isPlaying = isPlaying && currentArticle?.id == article.id,
                                isDownloading = downloadingArticleIds.contains(article.id),
                                playbackSpeed = playbackSpeed,
                                modifier = Modifier
                                    .offset { offset }
                                    .zIndex(zIndex)
                                    .animateItem()
                                    .fillMaxWidth(),
                                onPlayPauseClick = { onPlayPauseClick(article) },
                                onMarkAsPlayedClick = { onMarkAsPlayedClick(article) },
                                onRemoveClick = { onRemoveFromQueue(article) },
                                dragModifier = Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            draggedItemIndex = currentIndex
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingOffset += dragAmount.y

                                            val currentDraggedIndex = draggedItemIndex
                                            if (currentDraggedIndex == -1) return@detectDragGestures
                                            val itemHeight =
                                                lazyListState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.index == currentDraggedIndex }?.size
                                                    ?: 0

                                            if (itemHeight > 0) {
                                                val targetIndex = when {
                                                    draggingOffset > itemHeight / 2 -> currentDraggedIndex + 1
                                                    draggingOffset < -itemHeight / 2 -> currentDraggedIndex - 1
                                                    else -> currentDraggedIndex
                                                }

                                                if (targetIndex in currentArticles.indices && targetIndex != currentDraggedIndex) {
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
                                        },
                                    )
                                },
                            )
                        }
                    }

                    NarraScrollbar(
                        lazyListState = lazyListState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun QueueScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                QueueScreenContent(
                    articles = SampleArticles.all.filter { it.isInQueue },
                    onHistoryClick = {},
                )
            }
        }
    }
}
