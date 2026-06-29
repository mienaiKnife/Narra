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

import com.mienaiknife.narra.ui.models.ContentBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlParserReproTest {
    @Test
    fun `parse div with multiple p tags does not merge them`() {
        val html = "<div><p>Para 1</p><p>Para 2</p></div>"
        val result = HtmlParser.parse(html)
        // Current implementation merges them because p tag traverse adds single \n
        // and addBlocksFromAnnotatedString splits on \n\s*\n+ (2 or more newlines)
        assertEquals(2, result.size)
        assertEquals("Para 1", result[0].text.text)
        assertEquals("Para 2", result[1].text.text)
    }
}
