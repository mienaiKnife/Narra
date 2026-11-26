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
package com.mienaiknife.narra.domain.models

/**
 * A user-supplied pronunciation for a word or phrase.
 *
 * The on-device Sherpa-ONNX frontend pronounces only what its downloaded lexicon contains.
 * [arpabet] is stored in the user's notation (American English ARPAbet) rather than as IPA so the
 * mapping can be improved later without discarding what the user entered. The repository converts
 * it to the lexicon's phones when the patched lexicon is written.
 *
 * [language] scopes the entry; only English lexicons are supported today, but other languages will
 * need their own phonetic alphabet.
 */
data class CustomPronunciation(
    val word: String,
    val arpabet: String,
    val language: String = DEFAULT_LANGUAGE,
) {
    companion object {
        const val DEFAULT_LANGUAGE: String = "en"
    }
}
