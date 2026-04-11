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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.QueueItem
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.QueueViewModel
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(
    navController: NavController,
    onArticleClick: (String) -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsState()
    val currentArticle by viewModel.currentArticle.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            QueueScreenContent(
                articles = articles,
                currentArticle = currentArticle,
                isPlaying = isPlaying,
                onArticleClick = onArticleClick,
                onPlayPauseClick = { article -> viewModel.onPlayPauseClick(article) },
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
                onHistoryClick = { navController.navigate("history") },
                onClearQueue = { viewModel.clearQueue() },
                onReorder = { from, to -> viewModel.reorderQueue(from, to) }
            )
        }
    }
}

@Composable
fun QueueScreenContent(
    articles: List<Article>,
    currentArticle: Article? = null,
    isPlaying: Boolean = false,
    onArticleClick: (String) -> Unit = {},
    onPlayPauseClick: (Article) -> Unit = {},
    onRemoveFromQueue: (Article) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onReorder: (Int, Int) -> Unit = { _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }

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
                style = MaterialTheme.typography.headlineLarge,
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort") },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh") },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = {
                            showMenu = false
                            onHistoryClick()
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear") },
                        onClick = {
                            showMenu = false
                            onClearQueue()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }

        Text(
            text = "${articles.size} ${if (articles.size == 1) "text" else "texts"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .pointerInput(articles) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            lazyListState.layoutInfo.visibleItemsInfo
                                .firstOrNull { item ->
                                    offset.y.toInt() in item.offset..(item.offset + item.size)
                                }
                                ?.let { item ->
                                    draggedItemIndex = item.index
                                }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingOffset += dragAmount.y

                            val currentDraggedIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
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
                            draggedItemIndex = null
                            draggingOffset = 0f
                        },
                        onDragCancel = {
                            draggedItemIndex = null
                            draggingOffset = 0f
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(articles, key = { _, article -> article.id }) { index, article ->
                val isDragging = index == draggedItemIndex
                QueueItem(
                    article = article,
                    isPlaying = article.id == currentArticle?.id && isPlaying,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset {
                            if (isDragging) {
                                IntOffset(0, draggingOffset.toInt())
                            } else {
                                IntOffset.Zero
                            }
                        }
                        .animateItem(),
                    onItemClick = {
                        onArticleClick(article.id)
                    },
                    onPlayPauseClick = {
                        onPlayPauseClick(article)
                    },
                    onRemoveClick = { onRemoveFromQueue(article) },
                    onReorderClick = { /* Handled by long press on the whole item for now */ }
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
