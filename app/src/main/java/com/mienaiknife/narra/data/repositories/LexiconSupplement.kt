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

/**
 * Words that the downloaded Sherpa-ONNX English lexicon is missing, with pronunciations derived
 * from related entries that it does contain.
 *
 * The lexicon silently drops out-of-vocabulary words, so a hole like "or" or "technician" makes a
 * sentence lose a word. This is the lexicon-only behavior of Sherpa-ONNX 2.x (espeak-ng/G2P was
 * removed); upstream's guidance is to add OOV entries to the lexicon or pre-phonemize the text
 * (https://github.com/k2-fsa/sherpa-onnx/issues/4139). The shared `lexicon-en-us.txt` release asset
 * itself has gaps: as of the 2026-10-13 asset it lacks common words such as her, or, per, seem,
 * technician, the British -ise forms and enquiries, as well as names and new technical terms. The
 * large lexicon still lives in `ModelRepository` and is downloaded at runtime; this object is the
 * small, versioned part that ships with the app and is merged into a patched copy by
 * [LexiconPatcher]. Keys are lowercase, and every entry was checked against the live
 * `lexicon-en-us.txt` (168,654 keys) before being added: none of them exists as a single-word
 * key, and no case variant exists either.
 *
 * See the "Known Issue: Dropped Out-of-Vocabulary Words" section in `docs/TTS_ENGINES.md` for the
 * full problem description, the verified missing-word audit, and the maintenance procedure.
 *
 * Derivations:
 * - Homophones already in the lexicon: "her" (hear the vowel in "herd"), "or" from "ore"/"oar",
 *   "per" from "purr", "seem" from "seam".
 * - Related forms: "technician" is "technicians" without the final `z`; "enquiries" is "enquiry"
 *   plus `z`; the British `-ise` verbs copy the `-ize` pronunciations the lexicon carries
 *   ("organize", "organizes", "customize", "customizes") and append `d` (past) or `ɪ ŋ` (present
 *   participle).
 */
internal object LexiconSupplement {
    /**
     * Bumped whenever the entries below change. The merged file records it in a comment, so the
     * patch is regenerated on upgrade instead of serving stale pronunciations.
     */
    const val VERSION: Int = 1

    private val english: Map<String, String> = linkedMapOf(
        // Function words and common vocabulary the lexicon lacks entirely.
        "her" to "hˈ ɜː",
        "or" to "ˈ oː ɹ",
        "per" to "pˈ ɜː",
        "seem" to "sˈ iː m",

        // "technicians" is present; the singular is not.
        "technician" to "t ɛ k nˈ ɪ ʃ ə n",

        // British -ise spellings of the -ize entries the lexicon does contain.
        "organise" to "ˈ ɔː ɹ ɡ ə nˌ a ɪ z",
        "organises" to "ˈ ɔː ɹ ɡ ə nˌ a ɪ z ᵻ z",
        "organised" to "ˈ ɔː ɹ ɡ ə nˌ a ɪ z d",
        "organising" to "ˈ ɔː ɹ ɡ ə nˌ a ɪ z ɪ ŋ",
        "customise" to "kˈ ʌ s t ə mˌ a ɪ z",
        "customises" to "kˈ ʌ s t ə mˌ a ɪ z ᵻ z",
        "customised" to "kˈ ʌ s t ə mˌ a ɪ z d",
        "customising" to "kˈ ʌ s t ə mˌ a ɪ z ɪ ŋ",

        // "enquiry" is present; the plural is not.
        "enquiries" to "ɛ ŋ k wˈ a ɪ ɚ ɹ i z",
    )

    /** Returns the `word -> phones` entries to merge for [language], or `null` when there are none. */
    fun forLanguage(language: String): Map<String, String>? = when (ModelLexicons.primarySubtag(language)) {
        "en" -> english
        else -> null
    }
}
