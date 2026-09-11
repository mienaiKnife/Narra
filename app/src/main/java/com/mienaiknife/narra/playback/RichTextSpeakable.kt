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
package com.mienaiknife.narra.playback

import androidx.core.net.toUri
import com.mienaiknife.narra.domain.models.RichText
import com.mienaiknife.narra.domain.models.SpeakableText
import com.mienaiknife.narra.utils.LanguageUtils

/**
 * Converts a [RichText] into [SpeakableText], replacing footnotes with spaces (preserving length)
 * and optionally shortening URL-like link text to the link's domain.
 *
 * @param linkToFormat localized format string used to announce a shortened link, e.g. " link to %s ".
 */
fun RichText.toSpeakableText(
    linkToFormat: String,
    shortenLinks: Boolean = true,
): SpeakableText {
    val resultText = text
    val speakableText = StringBuilder(resultText)

    // 1. Handle footnotes: replace with spaces to preserve length
    spans.filter { it.isFootnote }.forEach { span ->
        for (i in span.start until span.end) {
            if (i < speakableText.length) speakableText.setCharAt(i, ' ')
        }
    }

    if (shortenLinks) {
        // 2. Handle links: shorten if they look like URLs, but preserve length via padding
        spans.filter { it.link != null }.forEach { span ->
            val start = span.start
            val end = span.end
            val originalLength = end - start
            if (originalLength <= 0) return@forEach

            val linkText = resultText.substring(start, end).trim()
            if (isUrlLike(linkText)) {
                val simplified =
                    try {
                        linkToFormat.format(simplifyUrl(span.link!!))
                    } catch (_: Exception) {
                        linkText
                    }

                if (simplified.length <= originalLength) {
                    val padded = simplified.padEnd(originalLength, ' ')
                    for (i in 0 until originalLength) {
                        speakableText.setCharAt(start + i, padded[i])
                    }
                } else {
                    // Truncate if simplified text is somehow longer than original (rare for URLs)
                    for (i in 0 until originalLength) {
                        speakableText.setCharAt(start + i, simplified[i])
                    }
                }
            }
        }
    }

    val finalText = speakableText.toString()
    val (transliterated, map) = LanguageUtils.transliterateWithMapping(finalText)
    return SpeakableText(transliterated, map)
}

private fun isUrlLike(text: String): Boolean = text.startsWith("http://") ||
    text.startsWith("https://") ||
    text.contains(Regex("\\.[a-z]{2,3}/")) ||
    text.split("/").size > 2

private fun simplifyUrl(url: String): String {
    return try {
        val uri = url.toUri()
        val host = uri.host ?: return url
        host.removePrefix("www.")
    } catch (_: Exception) {
        url
    }
}
