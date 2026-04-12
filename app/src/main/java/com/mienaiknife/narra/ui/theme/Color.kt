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

package com.mienaiknife.narra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Mockup 2 Colors
private val NarraBlack = Color(0xFF191919)
private val NarraDarkGrey = Color(0xFF282828)
private val NarraCyan = Color(0xFF26a9be)
private val NarraCyanTrack = NarraCyan.copy(alpha = 0.5f)
private val NarraWhite = Color(0xFFededed)
private val NarraGrey = Color(0xFF949494)
private val NarraLightGrey = Color(0xFFe0e0e0)

val LightColorScheme = lightColorScheme(
    primary = NarraCyan,
    onPrimary = NarraWhite,
    background = NarraWhite,
    surface = NarraGrey,
    onSurface = NarraBlack,
    onSurfaceVariant = NarraDarkGrey,
    surfaceContainer = NarraLightGrey,
    primaryContainer = NarraCyanTrack
)

val DarkColorScheme = darkColorScheme(
    primary = NarraCyan,
    onPrimary = NarraWhite,
    background = NarraBlack,
    surface = NarraDarkGrey,
    onBackground = NarraWhite,
    onSurface = NarraWhite,
    onSurfaceVariant = NarraGrey,
    surfaceContainer = NarraDarkGrey,
    primaryContainer = NarraCyanTrack
)
