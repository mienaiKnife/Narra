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

package com.mienaiknife.narra.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private val outputWithYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val outputWithoutYear = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    private val inputFormatters = listOf(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    )

    private val htmlTagRegex = Regex("<[^>]*>")
    private val whitespaceRegex = Regex("\\s+")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)

    fun formatDateTime(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        )
        return dateTime.format(dateTimeFormatter)
    }

    fun formatPublishedDate(publishedAt: String?): String? {
        if (publishedAt.isNullOrBlank()) return null

        val date = parseDate(publishedAt) ?: return publishedAt

        val currentYear = LocalDate.now().year
        return if (date.year == currentYear) {
            date.format(outputWithoutYear)
        } else {
            date.format(outputWithYear)
        }
    }

    fun parseToTimestamp(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        for (formatter in inputFormatters) {
            try {
                return ZonedDateTime.parse(dateString, formatter).toInstant().toEpochMilli()
            } catch (_: Exception) {}
            try {
                return OffsetDateTime.parse(dateString, formatter).toInstant().toEpochMilli()
            } catch (_: Exception) {}
            try {
                return LocalDateTime.parse(dateString, formatter).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {}
            try {
                return LocalDate.parse(dateString, formatter).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseDate(dateString: String): LocalDate? {
        for (formatter in inputFormatters) {
            try {
                return ZonedDateTime.parse(dateString, formatter).toLocalDate()
            } catch (_: Exception) {}
            try {
                return OffsetDateTime.parse(dateString, formatter).toLocalDate()
            } catch (_: Exception) {}
            try {
                return LocalDateTime.parse(dateString, formatter).toLocalDate()
            } catch (_: Exception) {}
            try {
                return LocalDate.parse(dateString, formatter)
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatElapsedTime(millis: Long, referenceDuration: Long = 0L): String {
        val totalToUse = maxOf(millis, referenceDuration)
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        val totalHours = TimeUnit.MILLISECONDS.toHours(totalToUse)

        return if (totalHours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun estimateReadingTimeMs(text: String?, speed: Float = 1.0f): Long {
        if (text.isNullOrBlank()) return 0L
        // Strip HTML tags roughly using pre-compiled regex
        val plainText = text.replace(htmlTagRegex, "")
        val words = plainText.split(whitespaceRegex).count { it.isNotBlank() }
        // Average reading speed is ~200-250 words per minute.
        // Let's use 200 wpm for a more generous estimate.
        // 200 wpm = 200 words / 60,000 ms = 1 word / 300 ms.
        val baseMs = words * 300L
        return (baseMs / speed).toLong().coerceAtLeast(1000L)
    }
}
