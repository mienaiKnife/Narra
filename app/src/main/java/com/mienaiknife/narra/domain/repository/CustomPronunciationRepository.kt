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
package com.mienaiknife.narra.domain.repository

import com.mienaiknife.narra.domain.models.CustomPronunciation
import kotlinx.coroutines.flow.Flow

/**
 * User-managed pronunciations for the on-device Sherpa-ONNX lexicon.
 *
 * Entries are stored separately from the downloaded lexicon and appended to the patched copy, so
 * the base download is never modified. Upserts replace an existing entry with the same normalized
 * word, which is what lets a user correct a word the shared lexicon already contains.
 */
interface CustomPronunciationRepository {
    /** All entries, regardless of language. Used to trigger an engine reload when the list changes. */
    fun observeAll(): Flow<List<CustomPronunciation>>

    /** Entries that apply to [language], for building the patched lexicon. */
    suspend fun snapshot(language: String): List<CustomPronunciation>

    /** Validates and stores [entry], replacing an existing entry with the same normalized word. */
    suspend fun upsert(entry: CustomPronunciation): Result<Unit>

    /**
     * Replaces [previous] with [replacement] in one write. Used when the user edits the word itself:
     * a plain upsert would add the new word and leave the old entry behind.
     */
    suspend fun replace(previous: CustomPronunciation, replacement: CustomPronunciation): Result<Unit>

    /** Removes the entry with the same normalized word. */
    suspend fun delete(entry: CustomPronunciation): Result<Unit>
}
