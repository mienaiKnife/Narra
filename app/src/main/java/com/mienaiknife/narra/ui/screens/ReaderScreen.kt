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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SampleArticles
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

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeUiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ReaderViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else if (uiState.error != null) {
            ErrorView(
                error = uiState.error!!,
                onRetry = viewModel::retry,
                onBack = onBack
            )
        } else {
            uiState.article?.let {
                ReaderContent(
                    uiState = uiState,
                    readerFontFamily = getFontFamily(themeUiState.readerFontFamily),
                    readerFontSize = themeUiState.readerFontSize,
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
                    onSetSearchQuery = viewModel::setSearchQuery
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderContent(
    uiState: ReaderUiState,
    readerFontFamily: androidx.compose.ui.text.font.FontFamily,
    readerFontSize: Float,
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
    onSetSearchQuery: (String) -> Unit
) {
    val article = uiState.article ?: return
    val blocks = uiState.blocks
    val currentParagraphIndex = uiState.currentParagraphIndex

    var isControlsVisible by remember { mutableStateOf(value = true) }
    var lastInteractionTrigger by remember { mutableIntStateOf(0) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSleepTimerSheetVisible by remember { mutableStateOf(false) }
    var isSearchSheetVisible by remember { mutableStateOf(false) }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentParagraphIndex
    )

    val isAtTop by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
        }
    }

    val context = LocalContext.current
    val view = LocalView.current
    val isPreview = LocalInspectionMode.current
    
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
            }
    ) {
        var isFollowing by remember(article.id) { mutableStateOf(true) }

        ReaderContentList(
            article = article,
            blocks = blocks,
            uiState = uiState,
            scrollState = scrollState,
            readerFontFamily = readerFontFamily,
            readerFontSize = readerFontSize,
            isFollowing = isFollowing,
            onFollowingChange = { isFollowing = it },
            onControlsVisibleChange = { isControlsVisible = it },
            onInteractionTrigger = { lastInteractionTrigger++ },
            onSeekToWord = onSeekToWord
        )

        val fabBottomPadding by animateDpAsState(
            targetValue = if (isControlsVisible) 150.dp else 40.dp,
            label = "fabBottomPadding"
        )

        ReaderFab(
            modifier = Modifier.align(Alignment.BottomEnd),
            isVisible = !isFollowing && isControlsVisible,
            onClick = { isFollowing = true },
            bottomPadding = fabBottomPadding
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
            sleepTimerMillis = uiState.sleepTimerMillisLeft
        )

        ReaderPlaybackControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            uiState = uiState,
            isControlsVisible = isControlsVisible,
            onTogglePlayPause = onTogglePlayPause,
            onSkipForward = onSkipForward,
            onSkipBackward = onSkipBackward,
            onSkipNext = onSkipNext,
            onCycleSpeed = onCycleSpeed
        )

        NarraScrollbar(
            lazyListState = scrollState,
            verticalPadding = 120.dp,
            onInteraction = {
                isFollowing = false
                isControlsVisible = true
                lastInteractionTrigger++
            }
        )

        if (isSleepTimerSheetVisible) {
            ReaderSleepTimerSheet(
                onDismiss = { isSleepTimerSheetVisible = false },
                onSetTimer = onSetSleepTimer
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
                }
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
    sleepTimerMillis: Long?
) {
    val uriHandler = LocalUriHandler.current

    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
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
                        textAlign = TextAlign.Start
                    )
                }

                Box {
                    IconButton(onClick = { onMenuExpandChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { onMenuExpandChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search") },
                            onClick = {
                                onMenuExpandChange(false)
                                onSearchClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (article.isFavorite) "Unfavorite" else "Favorite") },
                            onClick = {
                                onMenuExpandChange(false)
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        )
                        val sleepTimerText = if (sleepTimerMillis != null && sleepTimerMillis > 0) {
                            "Sleep timer (${DateUtils.formatElapsedTime(sleepTimerMillis)})"
                        } else {
                            "Sleep timer"
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
                                    tint = if (sleepTimerMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Visit site") },
                            onClick = {
                                onMenuExpandChange(false)
                                article.url?.let { uriHandler.openUri(it) }
                            },
                            enabled = article.url != null,
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
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
    isFollowing: Boolean,
    onFollowingChange: (Boolean) -> Unit,
    onControlsVisibleChange: (Boolean) -> Unit,
    onInteractionTrigger: () -> Unit,
    onSeekToWord: (Int, IntRange) -> Unit
) {
    var isInitialScroll by remember(article.id) { mutableStateOf(true) }
    var currentWordYInItem by remember(article.id) { mutableFloatStateOf(0f) }
    var currentWordYIndex by remember(article.id) { mutableIntStateOf(-1) }
    
    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { 4.dp.toPx() }

    val topPadding = 105.dp
    val bottomPadding = 220.dp

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
                val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == uiState.currentParagraphIndex }
                val targetViewportY = viewportHeight * 0.5f

                if (visibleItem != null && currentWordYIndex == uiState.currentParagraphIndex) {
                    val currentWordViewportY = visibleItem.offset - layoutInfo.viewportStartOffset + currentWordYInItem
                    val delta = currentWordViewportY - targetViewportY
                    
                    if (kotlin.math.abs(delta) > with(density) { 5.dp.toPx() }) {
                        if (isInitialScroll) {
                            scrollState.scrollToItem(uiState.currentParagraphIndex, (currentWordYInItem - targetViewportY).toInt())
                            isInitialScroll = false
                        } else {
                            scrollState.animateScrollBy(
                                value = delta,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    } else if (isInitialScroll) {
                        isInitialScroll = false
                    }
                } else {
                    val scrollOffset = (currentWordYInItem - targetViewportY).toInt()
                    if (isInitialScroll) {
                        scrollState.scrollToItem(uiState.currentParagraphIndex, scrollOffset)
                        if (currentWordYInItem > 0 && currentWordYIndex == uiState.currentParagraphIndex) {
                            isInitialScroll = false
                        }
                    } else {
                        scrollState.animateScrollToItem(uiState.currentParagraphIndex, scrollOffset)
                    }
                }
            }
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Article: ${article.title}"
            },
        contentPadding = PaddingValues(
            top = topPadding,
            bottom = bottomPadding,
            start = 24.dp,
            end = 24.dp
        )
    ) {
        itemsIndexed(blocks) { index, block ->
            val isHeading = block is ContentBlock.Heading
            val isCurrentParagraph = index == uiState.currentParagraphIndex
            
            if (isHeading && index > 0) {
                Spacer(modifier = Modifier.height(32.dp))
            }

            val baseAnnotatedString = block.text
            val colorScheme = MaterialTheme.colorScheme

            val annotatedString = remember(baseAnnotatedString, colorScheme) {
                buildAnnotatedString {
                    append(baseAnnotatedString)
                    baseAnnotatedString.getStringAnnotations("link", 0, baseAnnotatedString.length)
                        .forEach { annotation ->
                            addStyle(SpanStyle(color = colorScheme.primary), annotation.start, annotation.end)
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
                    lineHeight = (32 * (readerFontSize / 20f)).sp,
                    fontSize = readerFontSize.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontFamily = readerFontFamily
                )
                else -> MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = (32 * (readerFontSize / 20f)).sp,
                    fontSize = readerFontSize.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = readerFontFamily
                )
            }

            val paragraphSemantics = Modifier.semantics {
                contentDescription = "Paragraph ${index + 1} of ${blocks.size}"
            }

            when (block) {
                is ContentBlock.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .then(paragraphSemantics)
                            .then(if (isCurrentParagraph) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else Modifier)
                    ) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.altText ?: "Article image",
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit
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
                        verticalPaddingPx = verticalPaddingPx
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
                            val internalOffset = (if (isHeadingWithSpacer) headingSpacerPx else 0f) + verticalPaddingPx
                            currentWordYInItem = y + internalOffset
                            currentWordYIndex = index
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
    verticalPaddingPx: Float
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
            .padding(vertical = 4.dp)
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
                    .wordHighlight(isCurrentParagraph, currentWordRange, layoutResult, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                            }
                        )
                    }
            )

            DropdownMenu(expanded = contextMenuLink != null, onDismissRequest = { contextMenuLink = null }, offset = contextMenuOffset) {
                DropdownMenuItem(
                    text = { Text("Open link") },
                    onClick = { contextMenuLink?.let { uriHandler.openUri(it) }; contextMenuLink = null },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
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
    onMeasureWordY: (Float) -> Unit
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                .wordHighlight(isCurrentParagraph, currentWordRange, layoutResult, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                        }
                    )
                }
        )

        DropdownMenu(expanded = contextMenuLink != null, onDismissRequest = { contextMenuLink = null }, offset = contextMenuOffset) {
            DropdownMenuItem(
                text = { Text("Open link") },
                onClick = { contextMenuLink?.let { uriHandler.openUri(it) }; contextMenuLink = null },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
            )
        }
    }
}

@Composable
fun ReaderFab(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onClick: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = bottomPadding, end = 24.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Scroll to current position",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
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
    onCycleSpeed: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 0.dp, bottom = 16.dp)
            ) {
                // Progress Bar
                val progress = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f
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
                                steps = 100
                            )
                            contentDescription = "Playback progress: ${(progress * 100).toInt()}%"
                        },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    gapSize = 5.dp,
                    drawStopIndicator = {}
                )

                // Time Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${DateUtils.formatElapsedTime(scaledRemainingTime, scaledTotalDuration)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (progress >= 1f && scaledTotalDuration > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = DateUtils.formatElapsedTime(scaledTotalDuration, scaledTotalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Button
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
                                    contentDescription = "Playback Speed: current ${String.format(Locale.US, "%.1f", uiState.playbackSpeed)}x"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f", uiState.playbackSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // Rewind
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSkipBackward()
                            },
                            modifier = Modifier.height(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind ${uiState.rewindSkipTime}",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = uiState.rewindSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // Play/Pause
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlayPause()
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onBackground
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
                            modifier = Modifier.height(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Fast forward ${uiState.fastForwardSkipTime}",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = uiState.fastForwardSkipTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // Next
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.height(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next article",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground
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
    onSetTimer: (Int?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            val options = listOf(
                "Off" to null,
                "5 minutes" to 5,
                "15 minutes" to 15,
                "30 minutes" to 30,
                "45 minutes" to 45,
                "1 hour" to 60
            )
            
            options.forEach { (label, minutes) ->
                ListItem(
                    headlineContent = { Text(label) },
                    modifier = Modifier.clickable {
                        onSetTimer(minutes)
                        onDismiss()
                    }
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
    onResultClick: (com.mienaiknife.narra.ui.viewmodels.SearchResult) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search in article...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            HorizontalDivider()
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.searchResults) { _, result ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = result.previewText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        overlineContent = {
                            Text("Paragraph ${result.paragraphIndex + 1}")
                        },
                        modifier = Modifier.clickable {
                            onResultClick(result)
                            onDismiss()
                        }
                    )
                }
                
                if (uiState.searchQuery.length >= 2 && uiState.searchResults.isEmpty()) {
                    item {
                        Text(
                            text = "No results found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(
    error: Throwable,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh, // Using Refresh as a placeholder for error
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.message ?: "An unexpected error occurred while loading the article.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Go Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry")
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
    highlightColor: Color
): Modifier {
    if (!isCurrentParagraph || currentWordRange == null || layoutResult == null) return this

    val wordInfo = remember(currentWordRange, layoutResult) {
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
        val isSameLine = layoutResult.getLineForOffset(start) == layoutResult.getLineForOffset(trimmedEnd - 1)
        
        val offset = Offset(startRect.left, startRect.top)
        val size = if (isSameLine) {
            Size(endRect.right - startRect.left, startRect.height)
        } else {
            Size(startRect.width, startRect.height)
        }
        
        Pair(offset, size)
    } ?: return this

    return this.drawBehind {
        drawRoundRect(
            color = highlightColor,
            topLeft = wordInfo.first,
            size = wordInfo.second,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

private fun Char.isPunctuationOrWhitespace(): Boolean {
    return isWhitespace() || !isLetterOrDigit()
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
    showSystemUi = true
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
                currentWordRange = 330..334
            ),
            readerFontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            readerFontSize = 20f,
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
            onSetSearchQuery = {}
        )
    }
}
