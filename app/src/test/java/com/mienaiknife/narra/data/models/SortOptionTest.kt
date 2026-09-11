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
package com.mienaiknife.narra.data.models

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOptionTest {
    @Test
    fun `toggled flips direction for ordered options`() {
        assertEquals(SortOption.DATE_ASC, SortOption.DATE_DESC.toggled())
        assertEquals(SortOption.DATE_DESC, SortOption.DATE_ASC.toggled())
        assertEquals(SortOption.TITLE_DESC, SortOption.TITLE_ASC.toggled())
        assertEquals(SortOption.TITLE_ASC, SortOption.TITLE_DESC.toggled())
        assertEquals(SortOption.SOURCE_DESC, SortOption.SOURCE_ASC.toggled())
        assertEquals(SortOption.SOURCE_ASC, SortOption.SOURCE_DESC.toggled())
    }

    @Test
    fun `toggled leaves manual unchanged`() {
        assertEquals(SortOption.MANUAL, SortOption.MANUAL.toggled())
    }

    @Test
    fun `toggled is an involution`() {
        SortOption.entries.forEach { option ->
            assertEquals(option, option.toggled().toggled())
        }
    }

    @Test
    fun `selectedFrom flips the active option and applies a new one`() {
        assertEquals(SortOption.DATE_ASC, SortOption.DATE_DESC.selectedFrom(SortOption.DATE_DESC))
        assertEquals(SortOption.TITLE_ASC, SortOption.TITLE_ASC.selectedFrom(SortOption.DATE_DESC))
        SortOption.entries.forEach { option ->
            assertEquals(option.selectedFrom(option), option.toggled())
        }
    }
}
