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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.mienaiknife.narra.R

val OpenDyslexic3 = FontFamily(
    Font(R.font.opendyslexic3_regular, FontWeight.Normal),
    Font(R.font.opendyslexic3_bold, FontWeight.Bold),
)

fun getFontFamily(name: String): FontFamily {
    return when (name) {
        "OpenDyslexic3" -> OpenDyslexic3
        else -> FontFamily.Default
    }
}

@Suppress("DEPRECATION")
fun getTypography(fontFamily: FontFamily): Typography {
    val defaultPlatformStyle = PlatformTextStyle(includeFontPadding = false)
    val defaultLineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )

    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            platformStyle = defaultPlatformStyle,
            lineHeightStyle = defaultLineHeightStyle
        )
    )
}
