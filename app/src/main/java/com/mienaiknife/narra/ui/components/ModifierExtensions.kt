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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

fun Modifier.flashHighlight(enabled: Boolean): Modifier = composed {
    var isHighlighted by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (enabled) {
            delay(300) // Delay to wait for navigation/scroll to settle
            isHighlighted = true
            delay(1000)
            isHighlighted = false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue =
        if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 500),
        label = "flashHighlight",
    )

    this.background(backgroundColor)
}
