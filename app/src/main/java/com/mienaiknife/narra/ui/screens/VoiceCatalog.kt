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
package com.mienaiknife.narra.ui.screens

import com.mienaiknife.narra.domain.models.TtsModel
import java.util.Locale

/** A named speaker inside a model that exposes more than one voice. */
data class CuratedVoice(
    val id: Int,
    val name: String,
    val gender: VoiceGender,
)

enum class VoiceGender {
    FEMALE,
    MALE,
}

/**
 * Curated speaker names for models that ship several voices. Sherpa exposes only speaker indices,
 * so the labels live here as presentation data; models without an entry use `sid = 0`.
 */
private val kokoroVoices = listOf(
    CuratedVoice(5, "Amelie", VoiceGender.FEMALE),
    CuratedVoice(1, "Bella", VoiceGender.FEMALE),
    CuratedVoice(6, "Michael", VoiceGender.MALE),
    CuratedVoice(2, "Sarah", VoiceGender.FEMALE),
    CuratedVoice(3, "Nicole", VoiceGender.FEMALE),
    CuratedVoice(4, "Sky", VoiceGender.FEMALE),
    CuratedVoice(7, "George", VoiceGender.MALE),
    CuratedVoice(8, "Lewis", VoiceGender.MALE),
    CuratedVoice(9, "Alice", VoiceGender.FEMALE),
    CuratedVoice(10, "Lily", VoiceGender.FEMALE),
    CuratedVoice(0, "Julia", VoiceGender.FEMALE),
).sortedBy { it.name }

fun curatedVoicesFor(modelId: String?): List<CuratedVoice> = when (modelId) {
    "kokoro-en-v0_19" -> kokoroVoices
    else -> emptyList()
}

/** A language section in the voice list. */
data class LanguageGroup(
    val language: String,
    val models: List<TtsModel>,
)

/** Restricts the catalog to one language, or returns it unchanged when [language] is null. */
fun filterModelsByLanguage(
    models: List<TtsModel>,
    language: String?,
): List<TtsModel> = if (language == null) {
    models
} else {
    models.filter { it.language.ifBlank { "und" } == language }
}

/** The language name shown to the user, falling back to the raw tag. */
fun languageDisplayName(language: String): String = runCatching {
    Locale.forLanguageTag(language).getDisplayLanguage(Locale.getDefault())
}.getOrNull()
    ?.takeIf { it.isNotBlank() && !it.equals(language, ignoreCase = true) }
    ?: language.uppercase(Locale.getDefault())

/**
 * Groups the catalog by language so a growing multi-language list stays navigable. Sections and
 * voices are sorted case-insensitively; blank language tags fall into an "und" bucket.
 */
fun groupModelsByLanguage(
    models: List<TtsModel>,
    displayName: (String) -> String,
): List<LanguageGroup> = models
    .groupBy { model -> model.language.ifBlank { "und" } }
    .entries
    .sortedBy { (language, _) -> displayName(language).lowercase() }
    .map { (language, grouped) ->
        LanguageGroup(
            language = language,
            models = grouped.sortedBy { model -> model.name.lowercase() },
        )
    }
