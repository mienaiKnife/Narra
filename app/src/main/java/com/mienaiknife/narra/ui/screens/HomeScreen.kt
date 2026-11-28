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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.components.AdaptiveText
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.EmptyState
import com.mienaiknife.narra.ui.components.NarraProgressBar
import com.mienaiknife.narra.ui.components.NarraPullToRefreshBox
import com.mienaiknife.narra.ui.components.TopLevelScreen
import com.mienaiknife.narra.ui.theme.LocalNarraSpacing
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.utils.UrlUtils
import com.mienaiknife.narra.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    onArticleClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
                else -> {}
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onArticleClick = onArticleClick,
        onAddClick = onAddClick,
        onRefresh = { viewModel.refresh() },
    )
}

@Composable
fun HomeScreenContent(
    uiState: com.mienaiknife.narra.ui.viewmodels.HomeUiState,
    snackbarHostState: SnackbarHostState,
    onArticleClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val isRefreshing = (uiState as? com.mienaiknife.narra.ui.viewmodels.HomeUiState.Success)?.isRefreshing ?: false

    TopLevelScreen(
        title = stringResource(R.string.nav_home),
        modifier = modifier,
    ) {
        NarraPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            when (uiState) {
                is com.mienaiknife.narra.ui.viewmodels.HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                is com.mienaiknife.narra.ui.viewmodels.HomeUiState.Success -> {
                    if (uiState.continueListening.isNotEmpty() || uiState.newFromFeeds.isNotEmpty() || uiState.favoriteArticles.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                        ) {
                            if (uiState.continueListening.isNotEmpty()) {
                                ArticleCarousel(
                                    title = stringResource(R.string.home_continue_listening),
                                    articles = uiState.continueListening,
                                    onArticleClick = onArticleClick,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            if (uiState.newFromFeeds.isNotEmpty()) {
                                ArticleCarousel(
                                    title = stringResource(R.string.home_new_from_feeds),
                                    articles = uiState.newFromFeeds,
                                    onArticleClick = onArticleClick,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            if (uiState.favoriteArticles.isNotEmpty()) {
                                ArticleCarousel(
                                    title = stringResource(R.string.home_favorites),
                                    articles = uiState.favoriteArticles,
                                    onArticleClick = onArticleClick,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Default.AutoStories,
                            title = stringResource(R.string.home_empty_title),
                            description = stringResource(R.string.home_empty_message),
                            actionLabel = stringResource(R.string.home_add_content),
                            actionIcon = Icons.Default.Add,
                            onActionClick = onAddClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleCarousel(
    title: String,
    articles: List<Article>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(articles, key = { it.id }) { article ->
                ArticleCard(
                    article = article,
                    onClick = { onArticleClick(article.id) },
                )
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressPercent = ((article.progress ?: 0f) * 100).toInt()
    val contentDescriptionText = pluralStringResource(R.plurals.home_article_semantics_desc, progressPercent, article.title, article.source, progressPercent)

    Card(
        modifier = modifier
            .width(140.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDescriptionText
            },
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            // Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = article.imageUrl ?: article.feedImageUrl ?: UrlUtils.faviconUrl(article.url)
                var isImageLoaded by remember(imageUrl) { mutableStateOf(false) }

                if (!isImageLoaded) {
                    val placeholderIcon = when {
                        article.url?.startsWith("epub://") == true -> Icons.Default.AutoStories
                        article.isFromFeed -> Icons.Default.RssFeed
                        else -> Icons.Default.Language
                    }
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }

                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.home_cover_image_desc, article.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (article.progress == 1f) 0.6f else 1f,
                    onSuccess = { isImageLoaded = true },
                )
            }

            // Progress Bar underneath the image
            article.progress?.let { progress ->
                if (progress > 0f && progress < 1f) {
                    NarraProgressBar(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LocalNarraSpacing.current.itemVertical),
            ) {
                val baseColor = if (article.progress == 1f) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                val variantColor = if (article.progress == 1f) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant

                AdaptiveText(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium.copy(color = baseColor),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AdaptiveText(
                    text = article.source,
                    style = MaterialTheme.typography.bodySmall.copy(color = variantColor),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val mockArticles = SampleArticles.all
    val snackbarHostState = remember { SnackbarHostState() }
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HomeScreenContent(
                    uiState = com.mienaiknife.narra.ui.viewmodels.HomeUiState.Success(
                        continueListening = mockArticles.take(5),
                        newFromFeeds = mockArticles.filter { it.isFromFeed }.take(5),
                        favoriteArticles = mockArticles.filter { it.isFavorite }.take(5),
                    ),
                    snackbarHostState = snackbarHostState,
                    onArticleClick = {},
                    onAddClick = {},
                )
            }
        }
    }
}
