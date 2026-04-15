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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.mienaiknife.narra.NavDestination
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.viewmodels.FeedsViewModel

@Composable
fun FeedsScreen(
    navController: NavController,
    viewModel: FeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeedsScreenContent(
        navController = navController,
        feeds = uiState.feeds,
        isRefreshing = uiState.isRefreshing,
        sortOption = uiState.sortOption,
        onBackClick = { navController.popBackStack() },
        onDeleteFeed = { viewModel.deleteFeed(it) },
        onRefresh = { viewModel.refresh() },
        onSortOptionSelected = { viewModel.setSortOption(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreenContent(
    navController: NavController,
    feeds: List<FeedEntity>,
    isRefreshing: Boolean = false,
    sortOption: SortOption = SortOption.TITLE_ASC,
    onBackClick: () -> Unit,
    onDeleteFeed: (FeedEntity) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = onSortOptionSelected,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "Feeds",
                    style = MaterialTheme.typography.headlineLarge,
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
                    DropdownMenuItem(
                        text = { Text("Refresh") },
                        onClick = {
                            showMenu = false
                            onRefresh()
                        },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            if (feeds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                                    navController.navigate(NavDestination.Feed(feed.title))
                                },
                                onDeleteClick = { onDeleteFeed(feed) }
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
    onDeleteClick: () -> Unit
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
                if (feed.imageUrl != null) {
                    AsyncImage(
                        model = feed.imageUrl,
                        contentDescription = "Feed icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
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
            IconButton(onClick = { /* TODO: Implement feed notifications */ }) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    navController = navController,
                    feeds = sampleFeeds,
                    onBackClick = {}
                )
            }
        }
    }
}
