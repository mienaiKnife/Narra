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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.local.entities.FeedEntity
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.components.AdaptiveText
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.EmptyState
import com.mienaiknife.narra.ui.components.NarraPullToRefreshBox
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.components.OverflowMenu
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.components.SortBottomSheet
import com.mienaiknife.narra.ui.components.narraListItemColors
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.ui.viewmodels.FeedsViewModel

@Composable
fun FeedsScreen(
    onNavigateToFeed: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: FeedsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            viewModel.importOpml(inputStream)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml"),
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            viewModel.exportOpml(outputStream)
        }
    }

    FeedsScreenContent(
        onFeedClick = onNavigateToFeed,
        feeds = uiState.feeds,
        isRefreshing = uiState.isRefreshing,
        sortOption = uiState.sortOption,
        onBackClick = onBack,
        onDeleteFeed = { viewModel.deleteFeed(it) },
        onToggleNotifications = { viewModel.toggleNotifications(it) },
        onRefresh = { viewModel.refresh() },
        onSortOptionSelected = { viewModel.setSortOption(it) },
        onImportOpml = { importLauncher.launch(arrayOf("application/xml", "text/xml", "application/octet-stream", "*/*")) },
        onExportOpml = { exportLauncher.launch("narra-subscriptions.opml") },
    )
}

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
    onSortOptionSelected: (SortOption) -> Unit = {},
    onImportOpml: () -> Unit = {},
    onExportOpml: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val showSortSheet = remember { mutableStateOf(false) }

    if (showSortSheet.value) {
        SortBottomSheet(
            selectedOption = sortOption,
            onOptionSelected = { option ->
                onSortOptionSelected(option)
                showSortSheet.value = false
            },
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
            title = stringResource(R.string.nav_feeds),
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
                    text = { Text(stringResource(R.string.settings_downloads_import_feeds)) },
                    onClick = {
                        showMenu = false
                        onImportOpml()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_downloads_export_feeds)) },
                    onClick = {
                        showMenu = false
                        onExportOpml()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Download,
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
            if (feeds.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.RssFeed,
                    title = stringResource(R.string.feeds_empty_message),
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
                        items(feeds, key = { it.url }) { feed ->
                            FeedItem(
                                feed = feed,
                                onClick = {
                                    onFeedClick(feed.url, feed.title)
                                },
                                onDeleteClick = { onDeleteFeed(feed) },
                                onToggleNotifications = { onToggleNotifications(feed) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItem(
    feed: FeedEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleNotifications: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        ListItem(
            modifier = Modifier.combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
            headlineContent = {
                AdaptiveText(
                    text = feed.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = feed.description?.let { description ->
                {
                    AdaptiveText(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                // Square favicon or placeholder image
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val imageUrl = feed.imageUrl ?: UrlUtils.faviconUrl(feed.url)
                    var isImageLoaded by remember(imageUrl) { mutableStateOf(false) }

                    if (!isImageLoaded) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.feeds_icon_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageLoaded = true },
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (feed.lastRefreshErrorAt != null) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.feeds_refresh_error_desc),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }

                    // Notification icon on the right
                    IconToggleButton(
                        checked = feed.notificationsEnabled,
                        onCheckedChange = { onToggleNotifications() },
                    ) {
                        Icon(
                            imageVector = if (feed.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                            contentDescription = stringResource(R.string.feeds_notifications_desc),
                            modifier = Modifier.size(24.dp),
                            tint = if (feed.notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            colors = narraListItemColors(),
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feeds_delete_feed)) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            )
        }
    }
}

@PreviewLightDark
@Composable
fun FeedsScreenPreview() {
    val navController = rememberNavController()
    val sampleFeeds = listOf(
        FeedEntity(url = "1", title = "The Verge", description = "Tech news and more"),
        FeedEntity(url = "2", title = "Android Central", description = "Android news and reviews", lastRefreshErrorAt = 1L),
        FeedEntity(url = "3", title = "9to5Google", description = "Google and Android news"),
    )
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                FeedsScreenContent(
                    onFeedClick = { _, _ -> },
                    feeds = sampleFeeds,
                    onBackClick = {},
                )
            }
        }
    }
}
