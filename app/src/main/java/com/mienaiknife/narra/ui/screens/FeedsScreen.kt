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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.FeedsViewModel

@Composable
fun FeedsScreen(
    onNavigateToFeed: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: FeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeedsScreenContent(
        onFeedClick = onNavigateToFeed,
        feeds = uiState.feeds,
        isRefreshing = uiState.isRefreshing,
        sortOption = uiState.sortOption,
        onBackClick = onBack,
        onDeleteFeed = { viewModel.deleteFeed(it) },
        onToggleNotifications = { viewModel.toggleNotifications(it) },
        onRefresh = { viewModel.refresh() },
        onSortOptionSelected = { viewModel.setSortOption(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreenContent(
    onFeedClick: (String, String) -> Unit,
    feeds: List<FeedEntity>,
    isRefreshing: Boolean = false,
    sortOption: SortOption = SortOption.TITLE_ASC,
    onBackClick: () -> Unit,
    onDeleteFeed: (FeedEntity) -> Unit = {},
    onToggleNotifications: (FeedEntity) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = { option ->
                onSortOptionSelected(option)
                showSortSheet.value = false
            },
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
                Text(
                    text = "Feeds",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
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
            if (feeds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No feeds yet. Add one to get started!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val scrollState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(feeds) { feed ->
                            FeedItem(
                                feed = feed,
                                onClick = {
                                    onFeedClick(feed.url, feed.title)
                                },
                                onDeleteClick = { onDeleteFeed(feed) },
                                onToggleNotifications = { onToggleNotifications(feed) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItem(
    feed: FeedEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleNotifications: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square favicon or placeholder image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = feed.imageUrl ?: "https://www.google.com/s2/favicons?domain=${feed.url}&sz=128"
                var isImageLoaded by remember(imageUrl) { mutableStateOf(false) }

                if (!isImageLoaded) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }

                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Feed icon",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { isImageLoaded = true }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Feed name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (feed.description != null) {
                    Text(
                        text = feed.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            // Notification icon on the right
            IconButton(onClick = onToggleNotifications) {
                Icon(
                    imageVector = if (feed.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    tint = if (feed.notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete feed") },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun FeedsScreenPreview() {
    val navController = rememberNavController()
    val sampleFeeds = listOf(
        FeedEntity(url = "1", title = "The Verge", description = "Tech news and more"),
        FeedEntity(url = "2", title = "Android Central", description = "Android news and reviews"),
        FeedEntity(url = "3", title = "9to5Google", description = "Google and Android news")
    )
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                FeedsScreenContent(
                    onFeedClick = { _, _ -> },
                    feeds = sampleFeeds,
                    onBackClick = {}
                )
            }
        }
    }
}
