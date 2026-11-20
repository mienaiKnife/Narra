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
package com.mienaiknife.narra.data.repositories

import com.mienaiknife.narra.data.NetworkConstants
import java.util.Locale

/**
 * Language-scoped pronunciation lexicons required by Sherpa-ONNX 2.x.
 *
 * 2.x removed the espeak-ng frontend, so the `dataDir` shipped inside Piper/Matcha archives no
 * longer produces phonemes. Models instead read a lexicon (word -> phonemes), which is matched by
 * language; one downloaded file therefore serves every model of that language. Only languages
 * Narra can source a lexicon for are listed, and the engine fails loudly for the rest instead of
 * synthesizing silence.
 */
internal object ModelLexicons {
    private const val ENGLISH_LEXICON_FILE = "lexicon-en-us.txt"

    private val fileNames = mapOf("en" to ENGLISH_LEXICON_FILE)

    fun fileNameFor(language: String): String? = fileNames[primarySubtag(language)]

    fun urlFor(language: String): String? {
        val fileName = fileNameFor(language) ?: return null
        return "${NetworkConstants.SHERPA_MODELS_BASE_URL}/$fileName"
    }

    /** Reduces a BCP 47-ish language tag to the subtag the lexicon map is keyed by. */
    internal fun primarySubtag(language: String): String = language.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
}
