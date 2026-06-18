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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.UiText
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import com.mienaiknife.narra.ui.theme.getFontFamily
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState
import com.mienaiknife.narra.ui.viewmodels.ReaderViewModel
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.delay
import java.util.Locale

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
fun ReaderTopBar(
    modifier: Modifier = Modifier,
    article: Article,
    isControlsVisible: Boolean,
    onBack: () -> Unit,
    onMenuExpandChange: (Boolean) -> Unit,
    isMenuExpanded: Boolean,
    onSearchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepTimerClick: () -> Unit,
    sleepTimerMillis: Long?,
) {
    val uriHandler = LocalUriHandler.current

    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                    )
                    val dateAndSource = buildString {
                        val formattedDate = DateUtils.formatPublishedDate(article.publishedAt)
                        if (formattedDate != null) {
                            append(formattedDate)
                            append(" • ")
                        }
                        append(article.source)
                    }
                    Text(
                        text = dateAndSource,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                    )
                }

                Box {
                    IconButton(onClick = { onMenuExpandChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.action_menu),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { onMenuExpandChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_search)) },
                            onClick = {
                                onMenuExpandChange(false)
                                onSearchClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (article.isFavorite) stringResource(R.string.reader_menu_unfavorite) else stringResource(R.string.reader_menu_favorite)) },
                            onClick = {
                                onMenuExpandChange(false)
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        val sleepTimerText = if (sleepTimerMillis != null && sleepTimerMillis > 0) {
                            stringResource(R.string.reader_menu_sleep_timer_active, DateUtils.formatElapsedTime(sleepTimerMillis))
                        } else {
                            stringResource(R.string.reader_sleep_timer)
                        }
                        DropdownMenuItem(
                            text = { Text(sleepTimerText) },
                            onClick = {
                                onMenuExpandChange(false)
                                onSleepTimerClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (sleepTimerMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_visit_site)) },
                            onClick = {
                                onMenuExpandChange(false)
                                article.url?.let { uriHandler.openUri(it) }
                            },
                            enabled = article.url != null,
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderContentList(
    article: Article,
    blocks: List<ContentBlock>,
    uiState: ReaderUiState,
    scrollState: LazyListState,
    readerFontFamily: androidx.compose.ui.text.font.FontFamily,
    readerFontSize: Float,
    lineSpacing: Float,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isFollowing: Boolean,
    onFollowingChange: (Boolean) -> Unit,
    onControlsVisibleChange: (Boolean) -> Unit,
    onInteractionTrigger: () -> Unit,
    onSeekToWord: (Int, IntRange) -> Unit,
) {
    var isInitialScroll by remember(article.id) { mutableStateOf(true) }
    var currentWordYInItem by remember(article.id) { mutableFloatStateOf(0f) }
    var currentWordYIndex by remember(article.id) { mutableIntStateOf(-1) }

    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { 4.dp.toPx() }

    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        if (isDragged) {
            onFollowingChange(false)
            onControlsVisibleChange(true)
            onInteractionTrigger()
        }
    }

    LaunchedEffect(uiState.currentParagraphIndex, currentWordYInItem, uiState.currentWordRange, isFollowing) {
        if (isFollowing) {
            val layoutInfo = scrollState.layoutInfo
            val viewportHeight = layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                val targetViewportY = viewportHeight * 0.5f
                val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == uiState.currentParagraphIndex }

                // Only use the measurement if it's for the current paragraph
                val wordY = if (currentWordYIndex == uiState.currentParagraphIndex) currentWordYInItem else 0f

                // Calculate the scroll offset to center the word.
                // For item 0, the "natural" top is at topPadding from viewport top. For others, it's at 0.
                val itemTopInViewport = if (uiState.currentParagraphIndex == 0) with(density) { topPadding.toPx() } else 0f
                // itemTopInViewport - scrollOffset + wordY = targetViewportY
                val targetScrollOffset = (itemTopInViewport + wordY - targetViewportY).toInt()

                if (visibleItem != null && currentWordYIndex == uiState.currentParagraphIndex) {
                    val currentWordViewportY = visibleItem.offset - layoutInfo.viewportStartOffset + wordY
                    val delta = currentWordViewportY - targetViewportY

                    if (kotlin.math.abs(delta) > with(density) { 2.dp.toPx() }) {
                        if (isInitialScroll) {
                            scrollState.scrollToItem(uiState.currentParagraphIndex, targetScrollOffset)
                            if (wordY > 0) isInitialScroll = false
                        } else {
                            scrollState.animateScrollBy(
                                value = delta,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                        }
                    } else if (isInitialScroll && wordY > 0) {
                        isInitialScroll = false
                    }
                } else {
                    if (isInitialScroll) {
                        scrollState.scrollToItem(uiState.currentParagraphIndex, targetScrollOffset)
                        if (wordY > 0 && currentWordYIndex == uiState.currentParagraphIndex) {
                            isInitialScroll = false
                        }
                    } else {
                        scrollState.animateScrollToItem(uiState.currentParagraphIndex, targetScrollOffset)
                    }
                }
            }
        }
    }

    val articleSemanticsDesc = pluralStringResource(R.plurals.home_article_semantics_desc, 0, article.title, article.source, 0)

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = articleSemanticsDesc
            },
        contentPadding = PaddingValues(
            top = topPadding,
            bottom = bottomPadding,
            start = 24.dp,
            end = 24.dp,
        ),
    ) {
        itemsIndexed(blocks) { index, block ->
            val isHeading = block is ContentBlock.Heading
            val isCurrentParagraph = index == uiState.currentParagraphIndex

            if (isHeading && index > 0) {
                Spacer(modifier = Modifier.height(((32 + 16 * (lineSpacing - 1)) * lineSpacing).dp))
            }

            val baseAnnotatedString = block.text
            val colorScheme = MaterialTheme.colorScheme

            val currentWordRange = uiState.currentWordRange
            val annotatedString = remember(baseAnnotatedString, colorScheme, isCurrentParagraph, currentWordRange) {
                buildAnnotatedString {
                    append(baseAnnotatedString)
                    val links = baseAnnotatedString.getStringAnnotations("link", 0, baseAnnotatedString.length)
                    links.forEach { annotation ->
                        addStyle(SpanStyle(color = colorScheme.primary), annotation.start, annotation.end)
                    }

                    // Change highlighted link text to onSurface for better contrast with the highlight background
                    if (isCurrentParagraph && currentWordRange != null) {
                        links.forEach { link ->
                            val intersectStart = maxOf(link.start, currentWordRange.first)
                            val intersectEnd = minOf(link.end, currentWordRange.last + 1)
                            if (intersectStart < intersectEnd) {
                                addStyle(SpanStyle(color = colorScheme.onSurface), intersectStart, intersectEnd)
                            }
                        }
                    }

                    val words = baseAnnotatedString.text.split(Regex("(?<=\\s)|(?=\\s)"))
                    var currentOffset = 0
                    words.forEach { word ->
                        val start = currentOffset
                        val end = currentOffset + word.length
                        if (start < end && word.trim().isNotEmpty()) {
                            addStringAnnotation("word", "$index|$start|$end", start, end)
                        }
                        currentOffset = end
                    }
                }
            }

            val baseLineHeight = readerFontSize * 1.6f
            val baseStyle = when (block) {
                is ContentBlock.Heading -> {
                    val scaleFactor = readerFontSize / 18f
                    when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(fontSize = (34 * scaleFactor).sp, lineHeight = (42 * scaleFactor).sp)
                        2 -> MaterialTheme.typography.headlineMedium.copy(fontSize = (30 * scaleFactor).sp, lineHeight = (38 * scaleFactor).sp)
                        else -> MaterialTheme.typography.headlineSmall.copy(fontSize = (26 * scaleFactor).sp, lineHeight = (34 * scaleFactor).sp)
                    }.copy(color = MaterialTheme.colorScheme.onBackground, fontFamily = readerFontFamily)
                }
                is ContentBlock.BlockQuote -> MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = (baseLineHeight * lineSpacing).sp,
                    fontSize = readerFontSize.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontFamily = readerFontFamily,
                )
                else -> MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = (baseLineHeight * lineSpacing).sp,
                    fontSize = readerFontSize.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = readerFontFamily,
                )
            }

            val paragraphContentDesc = stringResource(R.string.reader_paragraph_semantics, index + 1, blocks.size)
            val paragraphSemantics = Modifier.semantics {
                contentDescription = paragraphContentDesc
            }

            when (block) {
                is ContentBlock.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .then(paragraphSemantics)
                            .then(if (isCurrentParagraph) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else Modifier)
                            .onGloballyPositioned { coords ->
                                if (isCurrentParagraph) {
                                    currentWordYInItem = coords.size.height / 2f
                                    currentWordYIndex = index
                                }
                            },
                    ) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.altText ?: stringResource(R.string.reader_article_image_desc),
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                is ContentBlock.BlockQuote -> {
                    BlockQuoteItem(
                        annotatedString = annotatedString,
                        baseStyle = baseStyle,
                        isCurrentParagraph = isCurrentParagraph,
                        currentWordRange = uiState.currentWordRange,
                        modifier = paragraphSemantics,
                        onSeekToWord = { pIdx, range ->
                            onSeekToWord(pIdx, range)
                            onFollowingChange(true)
                        },
                        onMeasureWordY = { y ->
                            currentWordYInItem = y
                            currentWordYIndex = index
                        },
                        verticalPaddingPx = verticalPaddingPx,
                    )
                }
                else -> {
                    val headingSpacerPx = with(density) { 32.dp.toPx() }
                    ParagraphItem(
                        annotatedString = annotatedString,
                        baseStyle = baseStyle,
                        isCurrentParagraph = isCurrentParagraph,
                        currentWordRange = uiState.currentWordRange,
                        modifier = paragraphSemantics,
                        onSeekToWord = { pIdx, range ->
                            onSeekToWord(pIdx, range)
                            onFollowingChange(true)
                        },
                        onMeasureWordY = { y ->
                            val isHeadingWithSpacer = block is ContentBlock.Heading && index > 0
                            val headingSpacing = (32 + 16 * (lineSpacing - 1)) * lineSpacing
                            val headingSpacerPx = with(density) { headingSpacing.dp.toPx() }
                            val internalOffset = (if (isHeadingWithSpacer) headingSpacerPx else 0f) + verticalPaddingPx
                            currentWordYInItem = y + internalOffset
                            currentWordYIndex = index
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(((16 + 8 * (lineSpacing - 1)) * lineSpacing).dp))
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
fun BlockQuoteItem(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    baseStyle: androidx.compose.ui.text.TextStyle,
    isCurrentParagraph: Boolean,
    currentWordRange: IntRange?,
    modifier: Modifier = Modifier,
    onSeekToWord: (Int, IntRange) -> Unit,
    onMeasureWordY: (Float) -> Unit,
    verticalPaddingPx: Float,
) {
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .then(modifier)
            .then(if (isCurrentParagraph) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(16.dp))

        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var contextMenuLink by remember { mutableStateOf<String?>(null) }
        var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = annotatedString,
                style = baseStyle,
                onTextLayout = { lr ->
                    layoutResult = lr
                    if (isCurrentParagraph && currentWordRange != null) {
                        if (currentWordRange.first in 0 until lr.layoutInput.text.length) {
                            val boundingBox = lr.getBoundingBox(currentWordRange.first)
                            onMeasureWordY(boundingBox.center.y + verticalPaddingPx)
                        }
                    }
                },
                modifier = Modifier
                    .wordHighlight(
                        isCurrentParagraph = isCurrentParagraph,
                        currentWordRange = currentWordRange,
                        layoutResult = layoutResult,
                        highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        fontSizePx = with(LocalDensity.current) { baseStyle.fontSize.toPx() },
                    )
                    .pointerInput(annotatedString) {
                        detectTapGestures(
                            onTap = { pos ->
                                layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                    annotatedString.getStringAnnotations("word", offset, offset).firstOrNull()?.let { annotation ->
                                        val parts = annotation.item.split("|")
                                        onSeekToWord(parts[0].toInt(), parts[1].toInt() until parts[2].toInt())
                                    }
                                }
                            },
                            onLongPress = { pos ->
                                layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                    annotatedString.getStringAnnotations("link", offset, offset).firstOrNull()?.let { annotation ->
                                        contextMenuLink = annotation.item
                                        contextMenuOffset = DpOffset(pos.x.toDp(), pos.y.toDp())
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            },
                        )
                    },
            )

            DropdownMenu(expanded = contextMenuLink != null, onDismissRequest = { contextMenuLink = null }, offset = contextMenuOffset) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_open_link)) },
                    onClick = {
                        contextMenuLink?.let { uriHandler.openUri(it) }
                        contextMenuLink = null
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
fun ParagraphItem(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    baseStyle: androidx.compose.ui.text.TextStyle,
    isCurrentParagraph: Boolean,
    currentWordRange: IntRange?,
    modifier: Modifier = Modifier,
    onSeekToWord: (Int, IntRange) -> Unit,
    onMeasureWordY: (Float) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var contextMenuLink by remember { mutableStateOf<String?>(null) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(modifier)
            .then(if (isCurrentParagraph) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = annotatedString,
            style = baseStyle,
            onTextLayout = { lr ->
                layoutResult = lr
                if (isCurrentParagraph && currentWordRange != null) {
                    if (currentWordRange.first in 0 until lr.layoutInput.text.length) {
                        onMeasureWordY(lr.getBoundingBox(currentWordRange.first).center.y)
                    }
                }
            },
            modifier = Modifier
                .wordHighlight(
                    isCurrentParagraph = isCurrentParagraph,
                    currentWordRange = currentWordRange,
                    layoutResult = layoutResult,
                    highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    fontSizePx = with(LocalDensity.current) { baseStyle.fontSize.toPx() },
                )
                .pointerInput(annotatedString) {
                    detectTapGestures(
                        onTap = { pos ->
                            layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                annotatedString.getStringAnnotations("word", offset, offset).firstOrNull()?.let { annotation ->
                                    val parts = annotation.item.split("|")
                                    onSeekToWord(parts[0].toInt(), parts[1].toInt() until parts[2].toInt())
                                }
                            }
                        },
                        onLongPress = { pos ->
                            layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                annotatedString.getStringAnnotations("link", offset, offset).firstOrNull()?.let { annotation ->
                                    contextMenuLink = annotation.item
                                    contextMenuOffset = DpOffset(pos.x.toDp(), pos.y.toDp())
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                    )
                },
        )

        DropdownMenu(expanded = contextMenuLink != null, onDismissRequest = { contextMenuLink = null }, offset = contextMenuOffset) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_open_link)) },
                onClick = {
                    contextMenuLink?.let { uriHandler.openUri(it) }
                    contextMenuLink = null
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
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

@Composable
fun ReaderPlaybackControls(
    modifier: Modifier = Modifier,
    uiState: ReaderUiState,
    isControlsVisible: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleSpeed: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 0.dp, bottom = 16.dp),
            ) {
                // Progress Bar
                val progress = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
                val playbackProgressDesc = stringResource(R.string.reader_playback_progress_desc, (progress * 100).toInt())
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = uiState.currentPosition.toFloat(),
                                range = 0f..uiState.duration.toFloat(),
                                steps = 100,
                            )
                            contentDescription = playbackProgressDesc
                        },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    gapSize = 5.dp,
                    drawStopIndicator = {},
                )

                // Time Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val nominalDuration = uiState.article?.duration ?: remember(uiState.article?.content) { DateUtils.estimateReadingTimeMs(uiState.article?.content ?: "") }
                    val scaledTotalDuration = (nominalDuration / uiState.playbackSpeed).toLong()
                    val scaledCurrentPosition = (progress * scaledTotalDuration).toLong()
                    val scaledRemainingTime = scaledTotalDuration - scaledCurrentPosition

                    val inProgress = progress > 0f && progress < 1f
                    if (inProgress) {
                        Text(
                            text = DateUtils.formatElapsedTime(scaledCurrentPosition, scaledTotalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "-${DateUtils.formatElapsedTime(scaledRemainingTime, scaledTotalDuration)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        if (progress >= 1f && scaledTotalDuration > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = DateUtils.formatElapsedTime(scaledTotalDuration, scaledTotalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Speed Button
                    val playbackSpeedDesc = stringResource(R.string.reader_playback_speed_desc, uiState.playbackSpeed)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCycleSpeed()
                            },
                            modifier = Modifier
                                .height(64.dp)
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = playbackSpeedDesc
                                },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f", uiState.playbackSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Rewind
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipBackward()
                            },
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = stringResource(R.string.reader_rewind_desc, uiState.rewindSkipTime),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = uiState.rewindSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Play/Pause
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlayPause()
                            },
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Forward
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipForward()
                            },
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.reader_fast_forward_desc, uiState.fastForwardSkipTime),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = uiState.fastForwardSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp),
                        )
                    }

                    // Next
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.reader_next_article_desc),
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
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

@Composable
private fun Modifier.wordHighlight(
    isCurrentParagraph: Boolean,
    currentWordRange: IntRange?,
    layoutResult: TextLayoutResult?,
    highlightColor: Color,
    fontSizePx: Float,
): Modifier {
    if (!isCurrentParagraph || currentWordRange == null || layoutResult == null) return this

    val density = LocalDensity.current
    val paddingPx = with(density) { 2.dp.toPx() }

    val wordInfo = remember(currentWordRange, layoutResult, fontSizePx, paddingPx) {
        val start = currentWordRange.first
        val end = currentWordRange.last + 1
        if (start < 0 || end > layoutResult.layoutInput.text.length) return@remember null

        // Trim punctuation from highlight for a cleaner look
        var trimmedEnd = end
        val text = layoutResult.layoutInput.text
        while (trimmedEnd > start && text[trimmedEnd - 1].isPunctuationOrWhitespace()) {
            trimmedEnd--
        }

        if (trimmedEnd <= start) return@remember null

        val startRect = layoutResult.getBoundingBox(start)
        val endRect = layoutResult.getBoundingBox(trimmedEnd - 1)

        // If the word wraps across lines, we only highlight the first part
        val line = layoutResult.getLineForOffset(start)
        val isSameLine = line == layoutResult.getLineForOffset(trimmedEnd - 1)

        // Use the baseline as the stable reference point for vertical alignment
        val baseline = layoutResult.getLineBaseline(line)

        val rectHeight = (fontSizePx * 1.2f) + (paddingPx * 2)
        // Center the highlight vertically relative to the baseline.
        // A visual center for a line of text is typically about 0.3em above the baseline.
        val highlightCenterY = baseline - (fontSizePx * 0.3f)
        val highlightTop = highlightCenterY - (rectHeight / 2)

        val offset = Offset(startRect.left - paddingPx, highlightTop)
        val size = if (isSameLine) {
            Size((endRect.right - startRect.left) + (paddingPx * 2), rectHeight)
        } else {
            Size(startRect.width + (paddingPx * 2), rectHeight)
        }

        Pair(offset, size)
    } ?: return this

    return this.drawBehind {
        drawRoundRect(
            color = highlightColor,
            topLeft = wordInfo.first,
            size = wordInfo.second,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
    }
}

private fun Char.isPunctuationOrWhitespace(): Boolean = isWhitespace() || !isLetterOrDigit()

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
    showSystemUi = true,
)
@Composable
fun ReaderScreenPreview() {
    val article = SampleArticles.sampleArticle1
    val blocks = remember(article.content) { HtmlParser.parse(article.content) }
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
