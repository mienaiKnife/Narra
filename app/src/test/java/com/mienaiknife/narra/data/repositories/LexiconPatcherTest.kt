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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.Locale

class LexiconPatcherTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var base: File

    private val supplement = linkedMapOf(
        "her" to "hˈ ɜː",
        "or" to "ˈ oː ɹ",
        "seem" to "sˈ iː m",
    )

    @Before
    fun setUp() {
        base = tempFolder.newFile("lexicon-en-us.txt")
        base.writeText(
            """
            # Lexicon: word(s) || space-separated phones
            hello || h ə lˈ o ʊ
            ore || ˈ oː ɹ
            Her || h ɜː
            Baton rouge || bˌ æ t ə n ɹˈ uː ʒ
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `languages without a supplement keep the base path`() {
        assertEquals(base, LexiconPatcher.resolve(base, null, LexiconSupplement.VERSION))
        assertEquals(base, LexiconPatcher.resolve(base, emptyMap(), LexiconSupplement.VERSION))
        assertFalse(LexiconPatcher.mergedFileFor(base).exists())
    }

    @Test
    fun `merge keeps the base, adds a version marker, and appends missing keys`() {
        val merged = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)

        assertNotEquals(base, merged)
        assertTrue("base lexicon must stay in place", base.isFile)
        assertTrue("base lexicon must be untouched", base.readText().contains("Her || h ɜː"))
        assertEquals("lexicon-en-us.narra.txt", merged.name)

        val lines = merged.readLines()
        assertEquals("# Narra lexicon supplement v1", lines.first())
        assertTrue(lines.contains("hello || h ə lˈ o ʊ"))
        assertTrue(lines.contains("Baton rouge || bˌ æ t ə n ɹˈ uː ʒ"))
        assertTrue(lines.contains("or || ˈ oː ɹ"))
        assertTrue(lines.contains("seem || sˈ iː m"))

        // "Her" is already in the base, so the supplement's "her" must not be appended again.
        assertEquals(1, lines.count { it.substringBefore("||").trim().lowercase(Locale.ROOT) == "her" })
        assertEquals(1, lines.count { it.startsWith("or ||") })

        assertFalse("no .part file should be left behind", File(merged.parentFile, merged.name + ".part").exists())
    }

    @Test
    fun `patching twice is idempotent`() {
        val first = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)
        val content = first.readText()

        val second = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)

        assertEquals(first, second)
        assertEquals(content, second.readText())
    }

    @Test
    fun `a new supplement version regenerates the merged file`() {
        val first = LexiconPatcher.resolve(base, supplement, 1)
        assertEquals("# Narra lexicon supplement v1", first.readLines().first())

        val second = LexiconPatcher.resolve(base, supplement, 2)

        assertEquals(first, second)
        assertEquals("# Narra lexicon supplement v2", second.readLines().first())
    }

    @Test
    fun `a newer base regenerates the merged file`() {
        val merged = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)

        base.writeText("# Lexicon\nhello || h ə lˈ o ʊ\nnewer || nˈ uː ɚ\n")
        assertTrue(base.setLastModified(merged.lastModified() + 10_000))

        val resolved = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)

        assertTrue(resolved.readText().contains("newer || nˈ uː ɚ"))
        assertTrue(resolved.readText().contains("seem || sˈ iː m"))
    }

    @Test
    fun `a missing or truncated merged file is rebuilt`() {
        val merged = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)

        merged.delete()
        assertTrue(LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION).readText().contains("hello"))

        merged.writeText("# Narra lexicon supplement v1\n")
        val rebuilt = LexiconPatcher.resolve(base, supplement, LexiconSupplement.VERSION)
        assertTrue(rebuilt.readText().contains("hello || h ə lˈ o ʊ"))
        assertTrue(rebuilt.readText().contains("seem || sˈ iː m"))
    }

    @Test
    fun `patching a missing base fails instead of writing a partial lexicon`() {
        val missing = File(tempFolder.root, "missing.txt")
        val merged = LexiconPatcher.mergedFileFor(missing)

        assertThrows(IOException::class.java) {
            LexiconPatcher.resolve(missing, supplement, LexiconSupplement.VERSION)
        }
        assertFalse(merged.exists())
    }
}
