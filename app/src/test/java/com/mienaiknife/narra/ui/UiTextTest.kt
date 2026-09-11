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
package com.mienaiknife.narra.ui

import android.content.Context
import android.content.res.Resources
import com.mienaiknife.narra.R
import com.mienaiknife.narra.domain.NarraError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UiTextTest {

    @Test
    fun `asString resolves nested UiText arguments`() {
        val context = mock<Context>()
        val resources = mock<Resources>()
        whenever(context.resources).thenReturn(resources)

        val innerText = UiText.DynamicString("Something went wrong")
        val outerText = UiText.StringResource(R.string.message_import_failed, innerText)

        whenever(context.getString(eq(R.string.message_import_failed), any())).thenAnswer { invocation ->
            "Import failed: ${invocation.arguments[1]}"
        }

        val result = outerText.asString(context)
        assertEquals("Import failed: Something went wrong", result)
    }

    @Test
    fun `fromError handles unknown error with blank message`() {
        val error = Exception("")
        val result = UiText.fromError(error)
        assert(result is UiText.StringResource)
        assertEquals(R.string.error_generic, (result as UiText.StringResource).resId)
    }

    @Test
    fun `fromError handles unknown error with null message`() {
        val error = Exception(null as String?)
        val result = UiText.fromError(error)
        assert(result is UiText.StringResource)
        assertEquals(R.string.error_generic, (result as UiText.StringResource).resId)
    }

    @Test
    fun `fromError maps WifiRequired to a wifi message`() {
        val result = UiText.fromError(NarraError.Network.WifiRequired())
        assertEquals(
            UiText.StringResource(R.string.error_wifi_required),
            result,
        )
    }

    @Test
    fun `StringResource uses value equality`() {
        val first = UiText.StringResource(R.string.error_generic)
        val second = UiText.StringResource(R.string.error_generic)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, UiText.StringResource(R.string.error_no_internet))
    }

    @Test
    fun `StringResource equality includes arguments`() {
        val first = UiText.StringResource(R.string.message_import_failed, "a")
        val second = UiText.StringResource(R.string.message_import_failed, "a")
        val different = UiText.StringResource(R.string.message_import_failed, "b")
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
    }

    @Test
    fun `PluralResource uses value equality`() {
        val first = UiText.PluralResource(R.plurals.unit_articles, 2, 2)
        val second = UiText.PluralResource(R.plurals.unit_articles, 2, 2)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, UiText.PluralResource(R.plurals.unit_articles, 3, 3))
    }
}
