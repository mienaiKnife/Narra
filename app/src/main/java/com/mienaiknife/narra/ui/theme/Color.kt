package com.mienaiknife.narra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Mockup 2 Colors
private val NarraBlack = Color(0xFF191919)
private val NarraDarkGrey = Color(0xFF282828)
private val NarraCyan = Color(0xFF26a9be)
private val NarraCyanTrack = Color(0xFF264a4f)
private val NarraWhite = Color(0xFFededed)
private val NarraGrey = Color(0xFF949494)

val LightColorScheme = lightColorScheme(
    primary = NarraCyan,
    onPrimary = NarraBlack,
    background = NarraWhite,
    surface = NarraWhite,
    onSurface = NarraBlack,
    onSurfaceVariant = NarraGrey,
    surfaceContainer = NarraWhite,
    primaryContainer = NarraCyanTrack
)

val DarkColorScheme = darkColorScheme(
    primary = NarraCyan,
    onPrimary = NarraBlack,
    background = NarraBlack,
    surface = NarraDarkGrey,
    onBackground = NarraWhite,
    onSurface = NarraWhite,
    onSurfaceVariant = NarraGrey,
    surfaceContainer = NarraDarkGrey,
    primaryContainer = NarraCyanTrack
)
