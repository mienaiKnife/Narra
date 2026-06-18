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

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val NarraBlack = Color(0xFF191919)
val NarraDarkGrey = Color(0xFF282828)
val NarraCyan = Color(0xFF1d8a9c) // Adjusted for better contrast on light backgrounds
val NarraCyanTrack = NarraCyan.copy(alpha = 0.5f)
val NarraWhite = Color(0xFFededed)
val NarraGrey = Color(0xFF949494)
val NarraLightGrey = Color(0xFFe0e0e0)

val LightColorScheme =
    lightColorScheme(
        primary = NarraCyan,
        onPrimary = NarraWhite,
        background = NarraWhite,
        surface = NarraGrey,
        onSurface = NarraBlack,
        onSurfaceVariant = NarraDarkGrey,
        surfaceContainer = NarraLightGrey,
        primaryContainer = NarraCyanTrack,
    )

val DarkColorScheme =
    darkColorScheme(
        primary = NarraCyan,
        onPrimary = NarraWhite,
        background = NarraBlack,
        surface = NarraDarkGrey,
        onBackground = NarraWhite,
        onSurface = NarraWhite,
        onSurfaceVariant = NarraGrey,
        surfaceContainer = NarraDarkGrey,
        primaryContainer = NarraCyanTrack,
    )
