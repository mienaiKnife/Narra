package com.mienaiknife.narra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Mockup 2 Colors
private val NarraBlack = Color(0xFF121212)
private val NarraDarkGrey = Color(0xFF2C2C2C)
private val NarraCyan = Color(0xFF00ACC1)
private val NarraCyanTrack = Color(0xFF1B3134)
private val NarraWhite = Color(0xFFFFFFFF)
private val NarraGrey = Color(0xFF9E9E9E)

val LightColorScheme = lightColorScheme(
    primary = NarraCyan,
    onPrimary = NarraBlack,
    background = NarraWhite,
    surface = NarraWhite,
    onSurface = NarraBlack,
    onSurfaceVariant = NarraGrey
)

val DarkColorScheme = darkColorScheme(
    primary = NarraCyan,
    onPrimary = NarraBlack,
    background = NarraBlack,
    surface = NarraDarkGrey,
    onBackground = NarraWhite,
    onSurface = NarraWhite,
    onSurfaceVariant = NarraGrey,
    surfaceVariant = NarraDarkGrey // For the image placeholder
)
