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
package com.mienaiknife.narra.ui.screens.reader

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.models.Article
import com.mienaiknife.narra.ui.models.ContentBlock
import com.mienaiknife.narra.ui.viewmodels.ReaderUiState

@Composable
fun ReaderContentList(
    article: Article,
    blocks: List<ContentBlock>,
    uiState: ReaderUiState,
    scrollState: LazyListState,
    readerFontFamily: FontFamily,
    readerFontSize: Float,
    lineSpacing: Float,
    topPadding: Dp,
    bottomPadding: Dp,
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
                    fontStyle = FontStyle.Italic,
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
                is ContentBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 32.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                is ContentBlock.Table -> {
                    TableItem(
                        table = block,
                        baseStyle = baseStyle,
                        isCurrentParagraph = isCurrentParagraph,
                        modifier = paragraphSemantics,
                        onMeasureWordY = { y ->
                            currentWordYInItem = y + verticalPaddingPx
                            currentWordYIndex = index
                        },
                    )
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
fun TableItem(
    table: ContentBlock.Table,
    baseStyle: TextStyle,
    isCurrentParagraph: Boolean,
    modifier: Modifier = Modifier,
    onMeasureWordY: (Float) -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(modifier)
            .then(if (isCurrentParagraph) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onGloballyPositioned { coords ->
                    if (isCurrentParagraph) {
                        onMeasureWordY(coords.size.height / 2f)
                    }
                }
                .width(IntrinsicSize.Max),
        ) {
            table.rows.forEach { row ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .then(if (cell.isHeader) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else Modifier)
                                .padding(8.dp)
                                .widthIn(min = 100.dp, max = 300.dp),
                        ) {
                            Text(
                                text = cell.text,
                                style = if (cell.isHeader) baseStyle.copy(fontWeight = FontWeight.Bold) else baseStyle,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun BlockQuoteItem(
    annotatedString: AnnotatedString,
    baseStyle: TextStyle,
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
    annotatedString: AnnotatedString,
    baseStyle: TextStyle,
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
