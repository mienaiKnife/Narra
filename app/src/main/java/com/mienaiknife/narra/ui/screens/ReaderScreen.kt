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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.data.models.SampleArticles
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.theme.NarraTheme
import com.mienaiknife.narra.ui.utils.HtmlParser
import com.mienaiknife.narra.ui.viewmodels.ReaderViewModel
import com.mienaiknife.narra.utils.DateUtils
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
@Composable
fun ReaderScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentParagraphIndex by viewModel.currentParagraphIndex.collectAsState()
    val currentWordRange by viewModel.currentWordRange.collectAsState()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            article?.let {
                ReaderContent(
                    article = it,
                    blocks = blocks,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    currentPosition = currentPosition,
                    duration = duration,
                    currentParagraphIndex = currentParagraphIndex,
                    currentWordRange = currentWordRange,
                    onBack = onBack,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekToWord = viewModel::seekToWord,
                    onSkipForward = viewModel::skipForward,
                    onSkipBackward = viewModel::skipBackward,
                    onCycleSpeed = viewModel::cycleSpeed
                )
            }
        }
    }
}

@Composable
fun ReaderContent(
    article: Article,
    blocks: List<ContentBlock>,
    isPlaying: Boolean,
    playbackSpeed: Float,
    currentPosition: Long,
    duration: Long,
    currentParagraphIndex: Int,
    currentWordRange: IntRange?,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekToWord: (Int, IntRange) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onCycleSpeed: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val view = LocalView.current
    
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

    LaunchedEffect(isPlaying, isControlsVisible, lastInteractionTrigger) {
        if (isPlaying) {
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
        val scrollState = rememberLazyListState()
        
        // State to track if we should follow the current paragraph
        var isFollowing by remember { mutableStateOf(true) }
        var currentWordYInItem by remember { mutableFloatStateOf(0f) }
        
        // Delayed word range to create a slight lag behind TTS
        var delayedWordRange by remember { mutableStateOf<IntRange?>(null) }
        LaunchedEffect(currentWordRange) {
            delay(15) // 15ms lag
            delayedWordRange = currentWordRange
        }

        var currentWordYIndex by remember { mutableIntStateOf(-1) }
        
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
        LaunchedEffect(currentParagraphIndex, currentWordYInItem, isFollowing) {
            if (isFollowing) {
                val layoutInfo = scrollState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                if (viewportHeight > 0) {
                    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == currentParagraphIndex }
                    
                    // Target the exact middle of the screen (0.5) as requested.
                    val targetViewportY = viewportHeight * 0.5f

                    if (visibleItem != null && currentWordYIndex == currentParagraphIndex) {
                        // Smoothly center the active word at the target line
                        val currentWordViewportY = visibleItem.offset + currentWordYInItem
                        val delta = currentWordViewportY - targetViewportY
                        
                        if (kotlin.math.abs(delta) > 2f) {
                            scrollState.animateScrollBy(delta)
                        }
                    } else {
                        // Item not visible or word position not yet measured, perform an initial jump
                        val scrollOffset = if (currentWordYInItem > 0 && currentWordYIndex == currentParagraphIndex) {
                            (currentWordYInItem - targetViewportY).toInt()
                        } else {
                            (-viewportHeight * 0.5f).toInt()
                        }
                        scrollState.animateScrollToItem(currentParagraphIndex, scrollOffset)
                    }
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 105.dp,
                bottom = 220.dp,
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

                        // Apply primary color to footnotes and links
                        listOf("footnote", "link").forEach { tag ->
                            baseAnnotatedString.getStringAnnotations(tag, 0, baseAnnotatedString.length)
                                .forEach { annotation ->
                                    addStyle(
                                        SpanStyle(color = colorScheme.primary),
                                        annotation.start,
                                        annotation.end
                                    )
                                }
                        }

                        if (isCurrentParagraph) {
                            // Highlight current paragraph
                            addStyle(
                                SpanStyle(background = colorScheme.primary.copy(alpha = 0.2f)),
                                0,
                                text.length
                            )

                            // Highlight current word with delay
                            delayedWordRange?.let { range ->
                                if (range.first in 0 until text.length) {
                                    addStyle(
                                        SpanStyle(
                                            background = colorScheme.primary.copy(alpha = 0.5f),
                                            color = colorScheme.onSurface
                                        ),
                                        range.first,
                                        (range.last + 1).coerceAtMost(text.length)
                                    )
                                }
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    is ContentBlock.BlockQuote -> MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        fontSize = 20.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    else -> MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (block is ContentBlock.BlockQuote) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .then(
                                if (isCurrentParagraph) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                ) else Modifier
                            )
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
                                        currentWordYInItem = boundingBox.center.y
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
                } else {
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Text(
                        text = annotatedString,
                        style = baseStyle,
                        onTextLayout = { lr ->
                            layoutResult = lr
                            if (isCurrentParagraph && currentWordRange != null) {
                                if (currentWordRange.first in 0 until lr.layoutInput.text.length) {
                                    val boundingBox = lr.getBoundingBox(currentWordRange.first)
                                    currentWordYInItem = boundingBox.center.y
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
                Spacer(modifier = Modifier.height(16.dp))
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Floating Button: Scrolls back to the current paragraph if it's off-screen
        val currentParagraphScrollStatus by remember {
            derivedStateOf {
                val visibleItems = scrollState.layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@derivedStateOf 0 // Visible

                val targetIndex = currentParagraphIndex
                val firstVisible = visibleItems.firstOrNull()?.index ?: 0
                val lastVisible = visibleItems.lastOrNull()?.index ?: 0

                when {
                    targetIndex < firstVisible -> -1 // Above
                    targetIndex > lastVisible -> 1 // Below
                    else -> 0 // Visible
                }
            }
        }

        val fabBottomPadding by animateDpAsState(
            targetValue = if (isControlsVisible) 240.dp else 40.dp,
            label = "fabBottomPadding"
        )

        AnimatedVisibility(
            visible = !isFollowing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
                    imageVector = when (currentParagraphScrollStatus) {
                        -1 -> Icons.Default.KeyboardArrowUp
                        1 -> Icons.Default.KeyboardArrowDown
                        else -> Icons.Default.CenterFocusStrong
                    },
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

                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
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
                        .padding(top = 0.dp, bottom = 30.dp)
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${formatTime(duration - currentPosition)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Control Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(48.dp)
                        ) {
                            IconButton(onClick = onCycleSpeed) {
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
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Rewind 15
                        IconButton(onClick = onSkipBackward) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 15 seconds",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Play/Pause
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

                        // Forward 15
                        IconButton(onClick = onSkipForward) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 15 seconds",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Next
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
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
            article = article,
            blocks = blocks,
            isPlaying = false,
            playbackSpeed = 1.0f,
            currentPosition = 46000L,
            duration = 180000L,
            currentParagraphIndex = 0,
            currentWordRange = 0..1,
            onBack = {},
            onTogglePlayPause = {},
            onSeekToWord = { _, _ -> },
            onSkipForward = {},
            onSkipBackward = {},
            onCycleSpeed = {}
        )
    }
}
