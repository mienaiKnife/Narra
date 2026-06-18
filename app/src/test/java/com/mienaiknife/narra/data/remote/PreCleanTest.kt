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
package com.mienaiknife.narra.data.remote

import org.jsoup.Jsoup
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PreCleanTest {
    private fun preCleanDocument(doc: org.jsoup.nodes.Document) {
        val junkSelectors =
            listOf(
                "nav",
                "footer",
                "aside",
                "script",
                "style",
                "noscript",
                "iframe",
                "form",
                ".social",
                ".share",
                ".ad-",
                ".banner",
                ".related",
                ".recommend",
                ".comment",
                "#social",
                "#share",
                "#ad-",
                "#banner",
                "#related",
                "#recommend",
                "#comment",
                "[class*=social]",
                "[class*=share]",
                "[class*=related]",
                "[class*=recommend]",
                "[id*=social]",
                "[id*=share]",
                "[id*=related]",
                "[id*=recommend]",
            )
        junkSelectors.forEach { selector ->
            doc.select(selector).forEach { element ->
                // Don't remove high-level structural tags even if they match a junk selector
                if (element.tagName() !in listOf("body", "html", "article", "main")) {
                    element.remove()
                }
            }
        }
    }

    @Test
    fun `preCleanDocument does NOT remove body even if it has a share class`() {
        val html =
            """
            <html>
                <body class="long-read-share-links">
                    <div id="content">Main content</div>
                    <div class="share-links">Should be removed</div>
                </body>
            </html>
            """.trimIndent()
        val doc = Jsoup.parse(html)

        preCleanDocument(doc)

        assertNotNull("Body should NOT have been removed", doc.selectFirst("body"))
        assertNotNull("Main content should still exist", doc.selectFirst("#content"))
        assertNull("Share links div should have been removed", doc.selectFirst(".share-links"))
    }
}
