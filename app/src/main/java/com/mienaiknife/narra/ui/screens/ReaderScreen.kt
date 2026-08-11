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

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.UiText
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.screens.reader.ReaderContentList
import com.mienaiknife.narra.ui.screens.reader.ReaderPlaybackControls
import com.mienaiknife.narra.ui.screens.reader.ReaderTopBar
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import com.mienaiknife.narra.ui.theme.getFontFamily
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState
import com.mienaiknife.narra.ui.viewmodels.ReaderViewModel
import kotlinx.coroutines.delay

private val ReaderTopPadding = 105.dp
private val ReaderBottomPadding = 220.dp

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeUiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ReaderViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.uiText.asString(context))
                }
            }
        }
    }

    // Automatically close the screen if the queue finishes (article becomes null)
    LaunchedEffect(uiState.article, uiState.isLoading) {
        if ((!uiState.isLoading) && (uiState.article == null)) {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else if (uiState.error != null) {
            ErrorView(
                error = uiState.error!!,
                onRetry = viewModel::retry,
                onBack = onBack,
            )
        } else {
            uiState.article?.let {
                ReaderContent(
                    uiState = uiState,
                    readerFontFamily = getFontFamily(themeUiState.readerFontFamily),
                    readerFontSize = themeUiState.readerFontSize,
                    lineSpacing = themeUiState.lineSpacing.toFloatOrNull() ?: 1.6f,
                    tapToShowControls = themeUiState.tapToShowControls,
                    autoFullscreen = themeUiState.autoFullscreen,
                    onBack = onBack,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekToWord = viewModel::seekToWord,
                    onSkipForward = viewModel::skipForward,
                    onSkipBackward = viewModel::skipBackward,
                    onSkipNext = viewModel::skipNext,
                    onCycleSpeed = viewModel::cycleSpeed,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSetSleepTimer = viewModel::setSleepTimer,
                    onSetSearchQuery = viewModel::setSearchQuery,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderContent(
    uiState: ReaderUiState,
    readerFontFamily: androidx.compose.ui.text.font.FontFamily,
    readerFontSize: Float,
    lineSpacing: Float,
    tapToShowControls: Boolean,
    autoFullscreen: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekToWord: (Int, IntRange) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onSetSearchQuery: (String) -> Unit,
) {
    val article = uiState.article ?: return
    val blocks = uiState.blocks
    val currentParagraphIndex = uiState.currentParagraphIndex

    var isControlsVisible by remember { mutableStateOf(value = true) }
    var lastInteractionTrigger by remember { mutableIntStateOf(0) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSleepTimerSheetVisible by remember { mutableStateOf(false) }
    var isSearchSheetVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val isPreview = LocalInspectionMode.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Estimate the scroll offset needed to center the paragraph on the first frame.
    // This reduces flicker before the actual word measurement is available.
    val initialScrollOffset = remember(article.id) {
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val targetViewportY = screenHeightPx * 0.5f
        val itemTopInViewport = if (currentParagraphIndex == 0) with(density) { ReaderTopPadding.toPx() } else 0f
        // itemTopInViewport - offset = targetViewportY => offset = itemTopInViewport - targetViewportY
        (itemTopInViewport - targetViewportY).toInt()
    }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentParagraphIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset,
    )

    val isAtTop by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
        }
    }

    // Keep screen on while in the reader
    if (!isPreview) {
        DisposableEffect(Unit) {
            val window = (context as? Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Handle system bars visibility
    LaunchedEffect(isControlsVisible) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)

        if (isControlsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    LaunchedEffect(uiState.isPlaying, isControlsVisible, lastInteractionTrigger, isAtTop, autoFullscreen) {
        if (isAtTop) {
            isControlsVisible = true
        } else if (uiState.isPlaying && autoFullscreen) {
            if (isControlsVisible) {
                delay(5000)
                isControlsVisible = false
            }
        } else if (!autoFullscreen) {
            isControlsVisible = true
        } else {
            isControlsVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(tapToShowControls) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        if (tapToShowControls) {
                            isControlsVisible = true
                            lastInteractionTrigger++
                        }
                    }
                }
            },
    ) {
        var isFollowing by remember(article.id) { mutableStateOf(true) }

        ReaderContentList(
            article = article,
            blocks = blocks,
            uiState = uiState,
            scrollState = scrollState,
            readerFontFamily = readerFontFamily,
            readerFontSize = readerFontSize,
            lineSpacing = lineSpacing,
            topPadding = ReaderTopPadding,
            bottomPadding = ReaderBottomPadding,
            isFollowing = isFollowing,
            onFollowingChange = { isFollowing = it },
            onControlsVisibleChange = { isControlsVisible = it },
            onInteractionTrigger = { lastInteractionTrigger++ },
            onSeekToWord = onSeekToWord,
        )

        val fabBottomPadding by animateDpAsState(
            targetValue = if (isControlsVisible) 150.dp else 40.dp,
            label = "fabBottomPadding",
        )

        ReaderFab(
            modifier = Modifier.align(Alignment.BottomEnd),
            isVisible = !isFollowing && isControlsVisible,
            onClick = { isFollowing = true },
            bottomPadding = fabBottomPadding,
        )

        ReaderTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            article = article,
            isControlsVisible = isControlsVisible,
            onBack = onBack,
            onMenuExpandChange = { isMenuExpanded = it },
            isMenuExpanded = isMenuExpanded,
            onSearchClick = { isSearchSheetVisible = true },
            onToggleFavorite = onToggleFavorite,
            onSleepTimerClick = { isSleepTimerSheetVisible = true },
            sleepTimerMillis = uiState.sleepTimerMillisLeft,
        )

        ReaderPlaybackControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            uiState = uiState,
            isControlsVisible = isControlsVisible,
            onTogglePlayPause = onTogglePlayPause,
            onSkipForward = onSkipForward,
            onSkipBackward = onSkipBackward,
            onSkipNext = onSkipNext,
            onCycleSpeed = onCycleSpeed,
        )

        NarraScrollbar(
            lazyListState = scrollState,
            verticalPadding = 120.dp,
            onInteraction = {
                isFollowing = false
                isControlsVisible = true
                lastInteractionTrigger++
            },
        )

        if (isSleepTimerSheetVisible) {
            ReaderSleepTimerSheet(
                onDismiss = { isSleepTimerSheetVisible = false },
                onSetTimer = onSetSleepTimer,
            )
        }

        if (isSearchSheetVisible) {
            ReaderSearchSheet(
                uiState = uiState,
                onDismiss = {
                    isSearchSheetVisible = false
                    onSetSearchQuery("")
                },
                onSearchQueryChange = onSetSearchQuery,
                onResultClick = { result ->
                    onSeekToWord(result.paragraphIndex, result.wordRange)
                    isFollowing = true
                },
            )
        }
    }
}

@Composable
fun ReaderFab(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onClick: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = bottomPadding, end = 24.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = stringResource(R.string.reader_scroll_to_current_position),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSleepTimerSheet(
    onDismiss: () -> Unit,
    onSetTimer: (Int?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_sleep_timer),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )

            val options = listOf(
                stringResource(R.string.action_off) to null,
                stringResource(R.string.unit_5_minutes) to 5,
                stringResource(R.string.unit_15_minutes) to 15,
                stringResource(R.string.unit_30_minutes) to 30,
                stringResource(R.string.unit_45_minutes) to 45,
                stringResource(R.string.unit_1_hour) to 60,
            )

            options.forEach { (label, minutes) ->
                ListItem(
                    headlineContent = { Text(label) },
                    modifier = Modifier.clickable {
                        onSetTimer(minutes)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSearchSheet(
    uiState: ReaderUiState,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onResultClick: (com.mienaiknife.narra.ui.viewmodels.SearchResult) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = Modifier.fillMaxHeight(0.8f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.reader_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
            )

            HorizontalDivider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.searchResults) { _, result ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = result.previewText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        overlineContent = {
                            Text(stringResource(R.string.reader_paragraph_search_desc, result.paragraphIndex + 1))
                        },
                        modifier = Modifier.clickable {
                            onResultClick(result)
                            onDismiss()
                        },
                    )
                }

                if (uiState.searchQuery.length >= 2 && uiState.searchResults.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.reader_no_results),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(
    error: UiText,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh, // Using Refresh as a placeholder for error
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.reader_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.asString(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reader_go_back))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reader_retry))
                }
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
    showSystemUi = true,
)
@Composable
fun ReaderScreenPreview() {
    val article = SampleArticles.sampleArticle1
    val blocks = remember(article.content) { HtmlParser.parse(article.content, article.url) }
    val fontFamily = getFontFamily("Roboto")
    NarraTheme(darkTheme = true, dynamicColor = false, fontFamily = fontFamily) {
        ReaderContent(
            uiState = ReaderUiState(
                article = article,
                blocks = blocks,
                isPlaying = true,
                playbackSpeed = 1.0f,
                currentPosition = 46000L,
                duration = 180000L,
                currentParagraphIndex = 1,
                currentWordRange = 330..334,
            ),
            readerFontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            readerFontSize = 20f,
            lineSpacing = 1.6f,
            tapToShowControls = true,
            autoFullscreen = true,
            onBack = {},
            onTogglePlayPause = {},
            onSeekToWord = { _, _ -> },
            onSkipForward = {},
            onSkipBackward = {},
            onSkipNext = {},
            onCycleSpeed = {},
            onToggleFavorite = {},
            onSetSleepTimer = {},
            onSetSearchQuery = {},
        )
    }
}
