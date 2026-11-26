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

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.Normalizer
import java.util.Locale

/**
 * Merges a small [LexiconSupplement] and the user's custom pronunciations into a patched copy of
 * the downloaded pronunciation lexicon.
 *
 * The downloaded file is never modified, so a bad merge cannot destroy it and a new download simply
 * makes the copy stale. The merged file is written next to the base as `<base>.narra.txt` through a
 * `.part` file and an atomic rename, so the engine never opens a half-written lexicon. A leading
 * `#` comment records the supplement version and, when overrides exist, a revision of their
 * contents; the native parser skips comment lines, and its presence lets [isUpToDate] detect a
 * stale version/revision and a truncated file.
 *
 * User overrides are written last, after both the base lines and the built-in supplement. The
 * Sherpa-ONNX 2.0.2 parsers (`ParseLexiconFile` and `lexicon_[key] = ...` in
 * `offline-tts-vits-impl.h` and the matcha/kokoro/kitten/zipvoice variants) keep the last duplicate
 * key, so an appended override wins over the base and the supplement without rewriting either.
 */
internal object LexiconPatcher {
    private const val MARKER_PREFIX = "# Narra lexicon supplement v"
    private const val KEY_SEPARATOR = "||"

    /** File name of the merged copy: `lexicon-en-us.txt` -> `lexicon-en-us.narra.txt`. */
    fun mergedFileFor(base: File): File = File(base.parentFile, base.name.removeSuffix(".txt") + ".narra.txt")

    /** First line of a patched lexicon for [version] and the optional override [revision]. */
    fun markerFor(version: Int, revision: String = ""): String = if (revision.isBlank()) "$MARKER_PREFIX$version" else "$MARKER_PREFIX$version $revision"

    /**
     * Returns the lexicon the engine should open. Languages without a supplement or overrides get
     * [base] itself, so their behavior is unchanged; otherwise a merged copy is returned,
     * regenerated when the base changes, the copy is missing/corrupt, or [version]/[revision]
     * differs.
     */
    fun resolve(
        base: File,
        supplement: Map<String, String>?,
        version: Int,
        overrides: Map<String, String> = emptyMap(),
        revision: String = "",
    ): File {
        if (supplement.isNullOrEmpty() && overrides.isEmpty()) return base
        return ensurePatched(base, mergedFileFor(base), supplement.orEmpty(), version, overrides, revision)
    }

    /** Returns [merged], writing it first when it is missing, stale, or from another version. */
    fun ensurePatched(
        base: File,
        merged: File,
        supplement: Map<String, String>,
        version: Int,
        overrides: Map<String, String> = emptyMap(),
        revision: String = "",
    ): File {
        if (isUpToDate(base, merged, version, revision)) return merged
        writePatched(base, merged, supplement, version, overrides, revision)
        return merged
    }

    /**
     * Whether [merged] already contains [base] plus the [version]'s supplement and overrides. A
     * merged file that is smaller than its base is treated as corrupt, as is one whose first line is
     * not the full marker.
     */
    fun isUpToDate(
        base: File,
        merged: File,
        version: Int,
        revision: String = "",
    ): Boolean {
        if (!base.isFile || base.length() == 0L) return false
        if (!merged.isFile || merged.length() < base.length()) return false
        if (base.lastModified() > merged.lastModified()) return false
        val firstLine = merged.bufferedReader(Charsets.UTF_8).use { it.readLine() }
        return firstLine == markerFor(version, revision)
    }

    /**
     * Streams [base] into [merged], appends the supplement entries whose keys are not already
     * present, then appends every override (sorted by key). Only the (small) supplement and override
     * key sets are tracked in memory, so the 6 MB base never has to be read into a map. The write
     * goes through `<merged>.part` and a rename.
     */
    fun writePatched(
        base: File,
        merged: File,
        supplement: Map<String, String>,
        version: Int,
        overrides: Map<String, String> = emptyMap(),
        revision: String = "",
    ) {
        if (!base.isFile || base.length() == 0L) {
            throw IOException("Cannot patch a missing lexicon: ${base.absolutePath}")
        }
        val directory = merged.absoluteFile.parentFile
            ?: throw IOException("Lexicon has no parent directory: ${merged.absolutePath}")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create lexicon directory: ${directory.absolutePath}")
        }

        val pending = LinkedHashMap<String, String>()
        for ((word, phones) in supplement) {
            val key = normalizeKey(word)
            require(key.isNotEmpty()) { "Supplement entry has no key: '$word'" }
            pending[key] = "$key $KEY_SEPARATOR $phones"
        }

        // Overrides are never filtered against the base: the native loader's last-wins duplicate
        // handling is what applies them.
        val pendingOverrides = LinkedHashMap<String, String>()
        for ((word, phones) in overrides) {
            val key = normalizeKey(word)
            require(key.isNotEmpty()) { "Override entry has no key: '$word'" }
            pendingOverrides[key] = "$key $KEY_SEPARATOR ${phones.trim()}"
        }

        val partial = File(directory, merged.name + ".part")
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(partial), Charsets.UTF_8)).use { writer ->
                writer.write(markerFor(version, revision))
                writer.newLine()
                base.bufferedReader(Charsets.UTF_8).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        writer.write(line)
                        writer.newLine()
                        val key = keyOf(line)
                        if (key != null) pending.remove(key)
                        line = reader.readLine()
                    }
                }
                // Sorted so the same inputs always produce the same file.
                for (entry in pending.toSortedMap().values) {
                    writer.write(entry)
                    writer.newLine()
                }
                for (entry in pendingOverrides.toSortedMap().values) {
                    writer.write(entry)
                    writer.newLine()
                }
            }

            if (merged.exists() && !merged.delete()) {
                throw IOException("Could not replace the merged lexicon: ${merged.absolutePath}")
            }
            if (!partial.renameTo(merged)) {
                throw IOException("Could not move the merged lexicon into place: ${merged.absolutePath}")
            }
        } finally {
            if (partial.exists()) partial.delete()
        }
    }

    /** Key on the left of `||` in a lexicon line, or `null` for blank and comment lines. */
    private fun keyOf(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        val separator = trimmed.indexOf(KEY_SEPARATOR)
        if (separator <= 0) return null
        return normalizeKey(trimmed.substring(0, separator))
    }

    /**
     * Matches the native lexicon loader's key transform (`NormalizeLexiconKey`): NFC, lowercase,
     * NFC. Punctuation is kept, because the base stores entries such as `mr.` with it.
     */
    internal fun normalizeKey(raw: String): String {
        val nfc = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
        return Normalizer.normalize(nfc.lowercase(Locale.ROOT), Normalizer.Form.NFC)
    }
}
