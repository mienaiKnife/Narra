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
package com.mienaiknife.narra.ui.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageUtilsTest {
    @Test
    fun `needsTransliteration returns false for plain English`() {
        val text = "Hello, this is a test. 123!"
        assertFalse(LanguageUtils.needsTransliteration(text))
    }

    @Test
    fun `needsTransliteration returns true for Japanese`() {
        val text = "あばらや"
        assertTrue(LanguageUtils.needsTransliteration(text))
    }

    @Test
    fun `needsTransliteration returns true for mixed text`() {
        val text = "Artist: あばらや (Producer)"
        assertTrue(LanguageUtils.needsTransliteration(text))
    }

    @Test
    fun `needsTransliteration returns true for Cyrillic`() {
        val text = "Привет"
        assertTrue(LanguageUtils.needsTransliteration(text))
    }

    @Test
    fun `needsTransliteration returns false for Latin Extended A`() {
        // Many European languages use these, and most TTS engines handle them.
        val text = "Hôtel déjà vu"
        assertFalse(LanguageUtils.needsTransliteration(text))
    }
}
