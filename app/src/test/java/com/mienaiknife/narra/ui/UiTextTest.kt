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
import org.junit.Assert.assertEquals
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

        val innerText = UiText.StringResource(R.string.error_generic)
        val outerText = UiText.StringResource(R.string.message_import_failed, innerText)

        whenever(context.getString(R.string.error_generic)).thenReturn("Something went wrong")
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
}
