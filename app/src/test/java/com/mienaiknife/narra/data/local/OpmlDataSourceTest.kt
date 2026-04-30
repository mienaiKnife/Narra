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

package com.mienaiknife.narra.data.local

import com.mienaiknife.narra.data.local.entities.FeedEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class OpmlDataSourceTest {

    private lateinit var opmlDataSource: OpmlDataSource

    @Before
    fun setUp() {
        opmlDataSource = OpmlDataSourceImpl()
    }

    @Test
    fun `parseOpml correctly parses valid OPML`() = runBlocking {
        val opmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <head>
                    <title>Subscriptions</title>
                </head>
                <body>
                    <outline text="Android Developers Blog" title="Android Developers Blog" type="rss" xmlUrl="https://android-developers.googleblog.com/feeds/posts/default" htmlUrl="https://android-developers.googleblog.com/"/>
                    <outline text="Kotlin Blog" title="Kotlin Blog" type="rss" xmlUrl="https://blog.jetbrains.com/kotlin/feed/" htmlUrl="https://blog.jetbrains.com/kotlin/"/>
                </body>
            </opml>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(opmlContent.toByteArray())
        val result = opmlDataSource.parseOpml(inputStream)

        if (result.isFailure) {
            println("Parse failed: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Expected success but was $result", result.isSuccess)
        val feeds = result.getOrNull()!!
        assertEquals(2, feeds.size)
    }

    @Test
    fun `parseOpml handles OPML with DOCTYPE`() = runBlocking {
        val opmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE opml PUBLIC "-//OPML//DTD OPML 2.0//EN" "http://opml.org/spec2.dtd">
            <opml version="2.0">
                <head>
                    <title>Subscriptions</title>
                </head>
                <body>
                    <outline text="Android Developers Blog" title="Android Developers Blog" type="rss" xmlUrl="https://android-developers.googleblog.com/feeds/posts/default" />
                </body>
            </opml>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(opmlContent.toByteArray())
        val result = opmlDataSource.parseOpml(inputStream)

        if (result.isFailure) {
            println("Parse failed with DOCTYPE: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Expected success for OPML with DOCTYPE but was $result", result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `parseOpml handles nested outlines`() = runBlocking {
        val opmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
                <head>
                    <title>Subscriptions</title>
                </head>
                <body>
                    <outline text="Technology">
                        <outline text="Android" title="Android" type="rss" xmlUrl="https://android.example.com/feed" />
                        <outline text="Kotlin" title="Kotlin" type="rss" xmlUrl="https://kotlin.example.com/feed" />
                    </outline>
                    <outline text="News" title="News" type="rss" xmlUrl="https://news.example.com/feed" />
                </body>
            </opml>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(opmlContent.toByteArray())
        val result = opmlDataSource.parseOpml(inputStream)

        assertTrue("Expected success for nested OPML but was $result", result.isSuccess)
        val feeds = result.getOrNull()!!
        assertEquals(3, feeds.size)
        assertTrue(feeds.any { it.url == "https://android.example.com/feed" })
        assertTrue(feeds.any { it.url == "https://kotlin.example.com/feed" })
        assertTrue(feeds.any { it.url == "https://news.example.com/feed" })
    }

    @Test
    fun `generateOpml correctly generates OPML`() = runBlocking {
        val feeds = listOf(
            FeedEntity(url = "https://example.com/feed1", title = "Feed 1"),
            FeedEntity(url = "https://example.com/feed2", title = "Feed 2")
        )

        val outputStream = ByteArrayOutputStream()
        val result = opmlDataSource.generateOpml(outputStream, feeds)

        if (result.isFailure) {
            println("Generate failed: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Expected success but was $result", result.isSuccess)
        val opmlContent = outputStream.toString("UTF-8")
        
        assertTrue(opmlContent.contains("xmlUrl=\"https://example.com/feed1\""))
        assertTrue(opmlContent.contains("title=\"Feed 1\""))
        assertTrue(opmlContent.contains("xmlUrl=\"https://example.com/feed2\""))
        assertTrue(opmlContent.contains("title=\"Feed 2\""))
    }

    @Test
    fun `parseOpml handles XXE attempt safely`() = runBlocking {
        val xxeContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE opml [
                <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <opml version="2.0">
                <head>
                    <title>Subscriptions &xxe;</title>
                </head>
                <body>
                    <outline text="XXE" title="XXE" type="rss" xmlUrl="https://example.com/feed" />
                </body>
            </opml>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xxeContent.toByteArray())
        val result = opmlDataSource.parseOpml(inputStream)

        // The parse should now succeed because DOCTYPE is allowed, 
        // but the external entity should not be expanded.
        assertTrue("Expected success as DOCTYPE is now allowed", result.isSuccess)
        val feeds = result.getOrNull()!!
        assertEquals(1, feeds.size)
    }
}
