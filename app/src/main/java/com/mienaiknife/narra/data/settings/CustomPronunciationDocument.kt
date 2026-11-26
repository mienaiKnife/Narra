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
package com.mienaiknife.narra.data.settings

import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.utils.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serialized shape of a user pronunciation. Kept separate from the domain model so the stored JSON
 * can evolve without changing [CustomPronunciation].
 */
@Serializable
data class CustomPronunciationEntry(
    val word: String,
    val arpabet: String,
    val language: String = CustomPronunciation.DEFAULT_LANGUAGE,
) {
    fun toDomain(): CustomPronunciation = CustomPronunciation(
        word = word,
        arpabet = arpabet,
        language = language,
    )

    companion object {
        fun fromDomain(entry: CustomPronunciation): CustomPronunciationEntry = CustomPronunciationEntry(
            word = entry.word,
            arpabet = entry.arpabet,
            language = entry.language,
        )
    }
}

/**
 * Contents of the `custom_pronunciations` settings entry.
 *
 * Decoding is deliberately forgiving: a corrupt or partially written value must not break the
 * settings flow, so [decode] logs and falls back to an empty document.
 */
@Serializable
data class CustomPronunciationDocument(
    val version: Int = CURRENT_VERSION,
    val entries: List<CustomPronunciationEntry> = emptyList(),
) {
    fun encode(): String = json.encodeToString(this)

    fun toDomain(): List<CustomPronunciation> = entries.map { it.toDomain() }

    companion object {
        const val CURRENT_VERSION: Int = 1

        private const val TAG = "CustomPronunciationDocument"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun decode(text: String?): CustomPronunciationDocument {
            if (text.isNullOrBlank()) return CustomPronunciationDocument()
            return try {
                json.decodeFromString(text)
            } catch (e: Exception) {
                Log.e(TAG, "Could not read stored custom pronunciations; starting empty", e)
                CustomPronunciationDocument()
            }
        }

        fun fromDomain(entries: List<CustomPronunciation>): CustomPronunciationDocument = CustomPronunciationDocument(
            entries = entries.map { CustomPronunciationEntry.fromDomain(it) },
        )
    }
}
