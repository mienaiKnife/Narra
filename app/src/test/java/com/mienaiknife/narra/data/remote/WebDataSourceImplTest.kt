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

import com.mienaiknife.narra.domain.NarraError
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class WebDataSourceImplTest {
    private lateinit var webDataSource: WebDataSource
    private val okHttpClient: OkHttpClient = mock()

    @Before
    fun setUp() {
        webDataSource = WebDataSourceImpl(okHttpClient)
    }

    @Test
    fun `downloadArticle returns failure for non-public URL`() = runBlocking {
        val result = webDataSource.downloadArticle("file:///etc/passwd")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NarraError.Network.NoConnection)
    }

    @Test
    fun `downloadArticle returns failure for invalid host`() = runBlocking {
        val result = webDataSource.downloadArticle("https://non-existent-domain-12345.com")
        assertTrue(result.isFailure)
    }

    @Test
    fun `readability4j preserves svg tags after conversion to img`() {
        val html = """
            <html>
            <body>
                <main>
                    <h1>Title</h1>
                    <p>Some text</p>
                    <span>
                        <svg width="100" height="100" id="test-svg">
                            <circle cx="50" cy="50" r="40" fill="yellow" />
                        </svg>
                    </span>
                    <p>More text</p>
                </main>
            </body>
            </html>
        """.trimIndent()
        
        val doc = org.jsoup.Jsoup.parse(html, "https://example.com")
        // Manually invoke preClean to simulate the behavior
        // Since it's private, I'll use reflection or just test it via a public method if possible.
        // WebDataSourceImpl.downloadArticle is public.
        
        // Actually, I can just test the logic by copying it or making it internal for testing.
        // For now, let's use reflection to test the private method if I can't mock the whole download flow easily.
        
        val method = webDataSource.javaClass.getDeclaredMethod("preCleanDocument", org.jsoup.nodes.Document::class.java)
        method.isAccessible = true
        method.invoke(webDataSource, doc)

        val img = doc.selectFirst("img")
        org.junit.Assert.assertNotNull("SVG should be converted to img", img)
        assertTrue("Img should have data URI", img!!.attr("src").startsWith("data:image/svg+xml;base64,"))
    }
}
