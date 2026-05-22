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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * A wrapper around [Text] that automatically shrinks the font size to fit within the available width
 * if it detects visual overflow.
 *
 * By default, it will not shrink below the "nominal" size (the size specified in [style] at 1.0 font scale).
 * This ensures consistency at standard font scales while providing flexibility for accessibility.
 */
@Composable
fun AdaptiveText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    minFontSize: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    shrinkToFitContent: Boolean = false,
) {
    val density = LocalDensity.current
    val fontScale = density.fontScale

    // Calculate the lower bound for shrinking.
    // By default, we don't want to shrink below what the size would be at 1.0 font scale.
    val nominalSize = style.fontSize
    val scaleLimit = remember(nominalSize, fontScale) {
        if (nominalSize.isSp) (nominalSize.value / fontScale).sp else nominalSize
    }

    val actualMinFontSize = remember(minFontSize, scaleLimit, shrinkToFitContent) {
        if (shrinkToFitContent) {
            if (minFontSize.isUnspecified) 10.sp else minFontSize
        } else {
            if (minFontSize.isUnspecified) scaleLimit else {
                if (scaleLimit.isUnspecified) minFontSize else {
                    if (minFontSize > scaleLimit) minFontSize else scaleLimit
                }
            }
        }
    }

    // Reset state when content or style-critical parameters change
    var fontSize by remember(text, style.fontSize, fontScale) { mutableStateOf(style.fontSize) }
    var readyToDraw by remember(text, style.fontSize, fontScale) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = if (fontSize.isUnspecified) style.fontSize else fontSize),
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow &&
                !actualMinFontSize.isUnspecified &&
                fontSize > actualMinFontSize
            ) {
                fontSize *= 0.9f
            } else {
                readyToDraw = true
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 300)
@Composable
fun AdaptiveTextPreview() {
    val longTitle = "This is a very very long article title that definitely overflows its container"
    
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scale 1.0 (Should NOT shrink, should truncate)", style = MaterialTheme.typography.labelSmall)
                AdaptiveText(
                    text = longTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Scale 1.5 (Should shrink to fit)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp))
                CompositionLocalProvider(
                    LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1.5f)
                ) {
                    AdaptiveText(
                        text = longTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("shrinkToFitContent = true (Old behavior, should shrink even at 1.0)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp))
                AdaptiveText(
                    text = longTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    shrinkToFitContent = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
