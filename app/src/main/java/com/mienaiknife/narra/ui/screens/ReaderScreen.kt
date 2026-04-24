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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.ui.viewmodels.ReaderViewModel
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.delay
import java.util.Locale
import com.mienaiknife.narra.ui.components.NarraScrollbar
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mienaiknife.narra.ui.theme.ThemeViewModel
import com.mienaiknife.narra.ui.theme.getFontFamily

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            uiState.article?.let {
                ReaderContent(
                    uiState = uiState,
                    readerFontFamily = getFontFamily(themeUiState.readerFontFamily),
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
    val isPlaying = uiState.isPlaying
    val playbackSpeed = uiState.playbackSpeed
    val currentPosition = uiState.currentPosition
    val duration = uiState.duration
    val currentParagraphIndex = uiState.currentParagraphIndex
    val currentWordRange = uiState.currentWordRange
    val fastForwardTime = uiState.fastForwardSkipTime
    val rewindTime = uiState.rewindSkipTime
    val sleepTimerMillis = uiState.sleepTimerMillisLeft
    val searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults

    var isControlsVisible by remember { mutableStateOf(true) }
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
    val uriHandler = LocalUriHandler.current
    
    // Keep screen on while in the reader
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    LaunchedEffect(isPlaying, isControlsVisible, lastInteractionTrigger, isAtTop) {
        if (isAtTop) {
            isControlsVisible = true
        } else if (isPlaying) {
            if (isControlsVisible) {
                delay(5000)
                isControlsVisible = false
            }
        } else {
            isControlsVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        isControlsVisible = true
                        lastInteractionTrigger++
                    }
                }
            }
    ) {
        // Article Content
        
        // State to track if we should follow the current paragraph
        var isFollowing by remember(article.id) { mutableStateOf(true) }
        var isInitialScroll by remember(article.id) { mutableStateOf(true) }
        var currentWordYInItem by remember(article.id) { mutableFloatStateOf(0f) }
        
        // Delayed word range to create a slight lag behind TTS
        var delayedWordRange by remember { mutableStateOf<IntRange?>(null) }
        LaunchedEffect(currentWordRange) {
            delay(15) // 15ms lag
            delayedWordRange = currentWordRange
        }

        var currentWordYIndex by remember(article.id) { mutableIntStateOf(-1) }
        val density = LocalDensity.current
        val verticalPaddingPx = with(density) { 4.dp.toPx() }

        // Content padding
        val topPadding = 105.dp
        val bottomPadding = 220.dp
        
        // Listen for manual scroll interactions
        val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
        LaunchedEffect(isDragged) {
            if (isDragged) {
                isFollowing = false
                isControlsVisible = true
                lastInteractionTrigger++
            }
        }

        // Auto-scroll to current paragraph when it changes, if following is enabled
        LaunchedEffect(currentParagraphIndex, currentWordYInItem, currentWordRange, isFollowing) {
            if (isFollowing) {
                val layoutInfo = scrollState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                if (viewportHeight > 0) {
                    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == currentParagraphIndex }
                    
                    // Target the exact middle of the screen
                    // Use total viewport height to find the center of the physical screen
                    val targetViewportY = viewportHeight * 0.5f

                    if (visibleItem != null && currentWordYIndex == currentParagraphIndex) {
                        // Smoothly center the active word at the target line
                        val currentWordViewportY = visibleItem.offset + currentWordYInItem
                        val delta = currentWordViewportY - targetViewportY
                        
                        if (kotlin.math.abs(delta) > 2f) {
                            if (isInitialScroll) {
                                scrollState.scrollToItem(currentParagraphIndex, (currentWordYInItem - targetViewportY).toInt())
                                isInitialScroll = false
                            } else {
                                scrollState.animateScrollBy(delta)
                            }
                        } else if (isInitialScroll) {
                            isInitialScroll = false
                        }
                    } else {
                        // Item not visible or word position not yet measured, perform an initial jump
                        val scrollOffset = (currentWordYInItem - targetViewportY).toInt()
                        
                        if (isInitialScroll) {
                            scrollState.scrollToItem(currentParagraphIndex, scrollOffset)
                            if (currentWordYInItem > 0 && currentWordYIndex == currentParagraphIndex) {
                                isInitialScroll = false
                            }
                        } else {
                            scrollState.animateScrollToItem(currentParagraphIndex, scrollOffset)
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = bottomPadding,
                start = 24.dp,
                end = 24.dp
            )
        ) {

            itemsIndexed(blocks) { index, block ->
                val isHeading = block is ContentBlock.Heading
                val isCurrentParagraph = index == currentParagraphIndex
                
                if (isHeading && index > 0) {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                val baseAnnotatedString = block.text
                val colorScheme = MaterialTheme.colorScheme

                val annotatedString = remember(baseAnnotatedString, isCurrentParagraph, delayedWordRange, colorScheme) {
                    buildAnnotatedString {
                        val text = baseAnnotatedString.text
                        
                        // Copy original styles from baseAnnotatedString
                        append(baseAnnotatedString)

                        // Apply primary color to links
                        baseAnnotatedString.getStringAnnotations("link", 0, baseAnnotatedString.length)
                            .forEach { annotation ->
                                addStyle(
                                    SpanStyle(color = colorScheme.primary),
                                    annotation.start,
                                    annotation.end
                                )
                            }

                        val highlightRange = delayedWordRange ?: currentWordRange
                        if (isCurrentParagraph && highlightRange != null) {
                            // Highlight current word
                            if (highlightRange.first in 0 until text.length) {
                                addStyle(
                                    SpanStyle(
                                        background = colorScheme.primary.copy(alpha = 0.4f),
                                        color = colorScheme.onSurface
                                    ),
                                    highlightRange.first,
                                    (highlightRange.last + 1).coerceAtMost(text.length)
                                )
                            }
                        }

                        // Add word annotations for clicking
                        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
                        var currentOffset = 0
                        words.forEach { word ->
                            val start = currentOffset
                            val end = currentOffset + word.length
                            if (start < end && word.trim().isNotEmpty()) {
                                addStringAnnotation(
                                    tag = "word",
                                    annotation = "$index|$start|$end",
                                    start = start,
                                    end = end
                                )
                            }
                            currentOffset = end
                        }
                    }
                }

                val baseStyle = when (block) {
                    is ContentBlock.Heading -> {
                        when (block.level) {
                            1 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 42.sp)
                            2 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp)
                            else -> MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp)
                        }.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = readerFontFamily
                        )
                    }
                    is ContentBlock.BlockQuote -> MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        fontSize = 20.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontFamily = readerFontFamily
                    )
                    else -> MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = readerFontFamily
                    )
                }

                if (block is ContentBlock.Image) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .then(
                                if (isCurrentParagraph) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) else Modifier
                            )
                    ) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.altText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (block is ContentBlock.BlockQuote) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(MaterialTheme.shapes.small)
                            .then(
                                if (isCurrentParagraph) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) else Modifier
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        Text(
                            text = annotatedString,
                            style = baseStyle,
                            onTextLayout = { lr ->
                                layoutResult = lr
                                if (isCurrentParagraph && currentWordRange != null) {
                                    if (currentWordRange.first in 0 until lr.layoutInput.text.length) {
                                        val boundingBox = lr.getBoundingBox(currentWordRange.first)
                                        currentWordYInItem = boundingBox.center.y + verticalPaddingPx
                                        currentWordYIndex = index
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(annotatedString) {
                                detectTapGestures { pos ->
                                    layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                        annotatedString.getStringAnnotations(
                                            tag = "word",
                                            start = offset,
                                            end = offset
                                        )
                                            .firstOrNull()?.let { annotation ->
                                                val parts = annotation.item.split("|")
                                                val pIdx = parts[0].toInt()
                                                val start = parts[1].toInt()
                                                val end = parts[2].toInt()
                                                onSeekToWord(pIdx, start until end)
                                                isFollowing = true
                                            }
                                    }
                                }
                            }
                        )
                    }
                } else {
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .then(
                                if (isCurrentParagraph) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = annotatedString,
                            style = baseStyle,
                            onTextLayout = { lr ->
                                layoutResult = lr
                                if (isCurrentParagraph && currentWordRange != null) {
                                    if (currentWordRange.first in 0 until lr.layoutInput.text.length) {
                                        val boundingBox = lr.getBoundingBox(currentWordRange.first)
                                        currentWordYInItem = boundingBox.center.y + verticalPaddingPx
                                        currentWordYIndex = index
                                    }
                                }
                            },
                            modifier = Modifier.pointerInput(annotatedString) {
                                detectTapGestures { pos ->
                                    layoutResult?.getOffsetForPosition(pos)?.let { offset ->
                                        annotatedString.getStringAnnotations(
                                            tag = "word",
                                            start = offset,
                                            end = offset
                                        )
                                            .firstOrNull()?.let { annotation ->
                                                val parts = annotation.item.split("|")
                                                val pIdx = parts[0].toInt()
                                                val start = parts[1].toInt()
                                                val end = parts[2].toInt()
                                                onSeekToWord(pIdx, start until end)
                                                isFollowing = true
                                            }
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        val fabBottomPadding by animateDpAsState(
            targetValue = if (isControlsVisible) 150.dp else 40.dp,
            label = "fabBottomPadding"
        )

        AnimatedVisibility(
            visible = !isFollowing && isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = fabBottomPadding, end = 24.dp)
        ) {
            IconButton(
                onClick = {
                    isFollowing = true
                },
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

        // Top Bar
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
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
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search") },
                                onClick = {
                                    isMenuExpanded = false
                                    isSearchSheetVisible = true
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
                                    isMenuExpanded = false
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
                                    isMenuExpanded = false
                                    isSleepTimerSheetVisible = true
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
                                    isMenuExpanded = false
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

        // Playback Controls
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
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
                    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Butt,
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
                        Text(
                            text = DateUtils.formatElapsedTime(currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${DateUtils.formatElapsedTime(duration - currentPosition)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                onClick = onCycleSpeed,
                                modifier = Modifier.height(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f", playbackSpeed),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        // Rewind
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onSkipBackward,
                                modifier = Modifier.height(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind $rewindTime",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = rewindTime,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        // Play/Pause
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Forward
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onSkipForward,
                                modifier = Modifier.height(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Fast forward $fastForwardTime",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = fastForwardTime,
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
                                    contentDescription = "Next",
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

        // Vertical Scrollbar
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
            ModalBottomSheet(
                onDismissRequest = { isSleepTimerSheetVisible = false },
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
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    onSetSleepTimer(minutes)
                                    isSleepTimerSheetVisible = false
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isSearchSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { 
                    isSearchSheetVisible = false
                    onSetSearchQuery("")
                },
                sheetState = rememberModalBottomSheetState(),
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSetSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search in article...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSetSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    HorizontalDivider()
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(searchResults) { _, result ->
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
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures {
                                        onSeekToWord(result.paragraphIndex, result.wordRange)
                                        isFollowing = true
                                        isSearchSheetVisible = false
                                        onSetSearchQuery("")
                                    }
                                }
                            )
                        }
                        
                        if (searchQuery.length >= 2 && searchResults.isEmpty()) {
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
    }
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
    NarraTheme(darkTheme = true, dynamicColor = false) {
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
