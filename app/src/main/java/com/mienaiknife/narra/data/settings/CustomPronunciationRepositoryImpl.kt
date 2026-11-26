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

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.domain.pronunciation.PronunciationValidator
import com.mienaiknife.narra.domain.repository.CustomPronunciationRepository
import com.mienaiknife.narra.utils.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores user pronunciations as a single JSON document in the shared settings DataStore.
 *
 * Every write reads, edits, and writes the document inside [androidx.datastore.preferences.core.edit]
 * so concurrent upserts or deletes cannot drop each other's changes.
 */
@Singleton
class CustomPronunciationRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) : CustomPronunciationRepository {

    private val customPronunciationsKey = stringPreferencesKey("custom_pronunciations")

    override fun observeAll(): Flow<List<CustomPronunciation>> = context.settingsDataStore.data
        .map { prefs ->
            CustomPronunciationDocument.decode(prefs[customPronunciationsKey]).toDomain()
        }
        .catch { e ->
            Log.e(TAG, "Could not read custom pronunciations; using none", e)
            emit(emptyList())
        }
        .distinctUntilChanged()

    override suspend fun snapshot(language: String): List<CustomPronunciation> {
        val normalizedLanguage = normalizeLanguage(language)
        return observeAll()
            .first()
            .filter { normalizeLanguage(it.language) == normalizedLanguage }
    }

    override suspend fun upsert(entry: CustomPronunciation): Result<Unit> {
        val prepared = prepare(entry).getOrElse { return Result.failure(it) }

        return editDocument(operation = "save", word = entry.word) { document ->
            val remaining = document.entries.filterNot { existing ->
                PronunciationValidator.normalizeWord(existing.word) == prepared.normalizedWord
            }

            if (remaining.size == document.entries.size && remaining.size >= MAX_ENTRIES) {
                return@editDocument null
            }

            remaining + prepared.toEntry()
        }
    }

    override suspend fun replace(
        previous: CustomPronunciation,
        replacement: CustomPronunciation,
    ): Result<Unit> {
        val previousKey = PronunciationValidator.normalizeWord(previous.word)
            ?: return Result.failure(
                IllegalArgumentException("Invalid pronunciation word: '${previous.word}'"),
            )
        val prepared = prepare(replacement).getOrElse { return Result.failure(it) }

        return editDocument(operation = "replace", word = previous.word) { document ->
            val hadPrevious = document.entries.any { existing ->
                PronunciationValidator.normalizeWord(existing.word) == previousKey
            }
            val newKeyExisted = document.entries.any { existing ->
                PronunciationValidator.normalizeWord(existing.word) == prepared.normalizedWord
            }
            val remaining = document.entries.filterNot { existing ->
                val key = PronunciationValidator.normalizeWord(existing.word)
                key == previousKey || key == prepared.normalizedWord
            }

            if (!hadPrevious && !newKeyExisted && remaining.size >= MAX_ENTRIES) {
                return@editDocument null
            }

            remaining + prepared.toEntry()
        }
    }

    /**
     * Validates [entry] and returns the normalized values written to storage. Validation lives here
     * as well as in the ViewModel so no caller can persist a line the lexicon cannot use.
     */
    private fun prepare(entry: CustomPronunciation): Result<PreparedEntry> {
        val validated = when (val result = PronunciationValidator.validate(entry.word, entry.arpabet)) {
            is PronunciationValidator.Result.Valid -> result
            is PronunciationValidator.Result.Invalid -> {
                val detail = result.detail?.let { " ($it)" }.orEmpty()
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid pronunciation for '${entry.word}': ${result.error}$detail",
                    ),
                )
            }
        }

        return Result.success(
            PreparedEntry(
                word = validated.word,
                normalizedWord = validated.normalizedWord,
                arpabet = entry.arpabet.trim(),
                language = entry.language.ifBlank { CustomPronunciation.DEFAULT_LANGUAGE },
            ),
        )
    }

    /**
     * Runs one read-modify-write inside [androidx.datastore.preferences.core.edit]. Returning `null`
     * from [transform] means the entry cap was reached and nothing should be written; returning the
     * unchanged list means no write is necessary.
     */
    private suspend fun editDocument(
        operation: String,
        word: String,
        transform: (CustomPronunciationDocument) -> List<CustomPronunciationEntry>?,
    ): Result<Unit> = try {
        var limitReached = false
        context.settingsDataStore.edit { prefs ->
            val document = CustomPronunciationDocument.decode(prefs[customPronunciationsKey])
            val entries = transform(document)
            if (entries == null) {
                limitReached = true
                return@edit
            }
            if (entries == document.entries) {
                return@edit
            }
            prefs[customPronunciationsKey] = CustomPronunciationDocument(
                version = document.version,
                entries = entries,
            ).encode()
        }

        if (limitReached) {
            Result.failure(
                IllegalStateException("Cannot add more than $MAX_ENTRIES custom pronunciations"),
            )
        } else {
            Result.success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Could not $operation custom pronunciation for '$word'", e)
        Result.failure(e)
    }

    private data class PreparedEntry(
        val word: String,
        val normalizedWord: String,
        val arpabet: String,
        val language: String,
    ) {
        fun toEntry(): CustomPronunciationEntry = CustomPronunciationEntry(
            word = word,
            arpabet = arpabet,
            language = language,
        )
    }

    override suspend fun delete(entry: CustomPronunciation): Result<Unit> {
        val normalizedWord = PronunciationValidator.normalizeWord(entry.word)
            ?: return Result.failure(
                IllegalArgumentException("Invalid pronunciation word: '${entry.word}'"),
            )

        return try {
            context.settingsDataStore.edit { prefs ->
                val document = CustomPronunciationDocument.decode(prefs[customPronunciationsKey])
                val remaining = document.entries.filterNot { existing ->
                    PronunciationValidator.normalizeWord(existing.word) == normalizedWord
                }

                if (remaining.size == document.entries.size) {
                    return@edit
                }

                prefs[customPronunciationsKey] = CustomPronunciationDocument(
                    version = document.version,
                    entries = remaining,
                ).encode()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not delete custom pronunciation for '${entry.word}'", e)
            Result.failure(e)
        }
    }

    private fun normalizeLanguage(language: String): String = language.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)

    private companion object {
        const val MAX_ENTRIES = 500
        const val TAG = "CustomPronunciationRepository"
    }
}
