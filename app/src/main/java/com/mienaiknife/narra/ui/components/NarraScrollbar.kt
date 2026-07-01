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
package com.mienaiknife.narra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoxScope.NarraScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 0.dp,
    onInteraction: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var visibleRecently by remember { mutableStateOf(false) }

    val isScrollInProgress = lazyListState.isScrollInProgress
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            visibleRecently = true
        } else {
            delay(1500)
            visibleRecently = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visibleRecently) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha",
    )

    val itemHeightMap = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                visibleItems.forEach { item ->
                    itemHeightMap[item.index] = item.size
                }
            }
    }

    val thumbHeightFraction by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            if (visibleItems.isEmpty() || totalItems == 0) {
                0.1f
            } else {
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                val avgHeight = if (itemHeightMap.isEmpty()) {
                    visibleItems.first().size.toFloat()
                } else {
                    itemHeightMap.values.average().toFloat()
                }

                val estimatedTotalHeight = (0 until totalItems).sumOf { i ->
                    itemHeightMap[i]?.toDouble() ?: avgHeight.toDouble()
                }.toFloat()

                if (estimatedTotalHeight <= viewportHeight) 1f else (viewportHeight / estimatedTotalHeight).coerceIn(0.1f, 1f)
            }
        }
    }

    val thumbOffsetFractionRaw by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            if (visibleItems.isEmpty() || totalItems == 0) {
                0f
            } else {
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                val firstItem = visibleItems.first()

                val avgHeight = if (itemHeightMap.isEmpty()) {
                    firstItem.size.toFloat()
                } else {
                    itemHeightMap.values.average().toFloat()
                }

                val estimatedTotalHeight = (0 until totalItems).sumOf { i ->
                    itemHeightMap[i]?.toDouble() ?: avgHeight.toDouble()
                }.toFloat()

                if (estimatedTotalHeight <= viewportHeight) {
                    0f
                } else {
                    val scrolledPixels = (0 until firstItem.index).sumOf { i ->
                        itemHeightMap[i]?.toDouble() ?: avgHeight.toDouble()
                    }.toFloat() - firstItem.offset
                    (scrolledPixels / (estimatedTotalHeight - viewportHeight)).coerceIn(0f, 1f)
                }
            }
        }
    }

    val thumbOffsetFraction by animateFloatAsState(
        targetValue = thumbOffsetFractionRaw,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "thumbOffsetAnimation",
    )

    val totalItems by remember {
        derivedStateOf { lazyListState.layoutInfo.totalItemsCount }
    }

    ScrollbarContainer(
        alpha = alpha,
        thumbHeightFraction = thumbHeightFraction,
        thumbOffsetFraction = thumbOffsetFraction,
        modifier = modifier
            .padding(vertical = verticalPadding)
            .pointerInput(totalItems) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        onInteraction()
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        val y = change.position.y
                        val height = size.height.toFloat()
                        if (height > 0 && totalItems > 0) {
                            val fraction = (y / height).coerceIn(0f, 1f)
                            val targetIndex = (fraction * totalItems).toInt()
                            coroutineScope.launch {
                                lazyListState.scrollToItem(targetIndex)
                            }
                            onInteraction()
                        }
                    },
                )
            }
            .pointerInput(totalItems) {
                detectTapGestures { offset ->
                    val y = offset.y
                    val height = size.height.toFloat()
                    if (height > 0 && totalItems > 0) {
                        val fraction = (y / height).coerceIn(0f, 1f)
                        val targetIndex = (fraction * totalItems).toInt()
                        coroutineScope.launch {
                            lazyListState.scrollToItem(targetIndex)
                        }
                        onInteraction()
                    }
                }
            },
    )
}

@Composable
fun BoxScope.NarraScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 0.dp,
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var visibleRecently by remember { mutableStateOf(false) }

    val isScrollInProgress = scrollState.isScrollInProgress
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            visibleRecently = true
        } else {
            delay(1500)
            visibleRecently = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visibleRecently) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha",
    )

    val thumbHeightFraction by remember {
        derivedStateOf {
            val maxValue = scrollState.maxValue
            if (maxValue == 0) {
                1f
            } else {
                // Rough estimate for verticalScroll
                0.2f
            }
        }
    }

    val thumbOffsetFraction by remember {
        derivedStateOf {
            val maxValue = scrollState.maxValue
            if (maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / maxValue
            }
        }
    }

    ScrollbarContainer(
        alpha = alpha,
        thumbHeightFraction = thumbHeightFraction,
        thumbOffsetFraction = thumbOffsetFraction,
        modifier = modifier
            .padding(vertical = verticalPadding)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        val y = change.position.y
                        val height = size.height.toFloat()
                        if (height > 0) {
                            val fraction = (y / height).coerceIn(0f, 1f)
                            coroutineScope.launch {
                                scrollState.scrollTo((fraction * scrollState.maxValue).toInt())
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val y = offset.y
                    val height = size.height.toFloat()
                    if (height > 0) {
                        val fraction = (y / height).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            scrollState.scrollTo((fraction * scrollState.maxValue).toInt())
                        }
                    }
                }
            },
    )
}

@Composable
private fun BoxScope.ScrollbarContainer(
    alpha: Float,
    thumbHeightFraction: Float,
    thumbOffsetFraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .graphicsLayer { this.alpha = alpha }
            .fillMaxHeight()
            .width(16.dp)
            .semantics {
                contentDescription = "Scrollbar"
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp)
                .width(4.dp)
                .fillMaxHeight(thumbHeightFraction)
                .graphicsLayer {
                    if (thumbHeightFraction > 0f) {
                        translationY = (size.height / thumbHeightFraction) * thumbOffsetFraction * (1f - thumbHeightFraction)
                    }
                }
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
        )
    }
}
