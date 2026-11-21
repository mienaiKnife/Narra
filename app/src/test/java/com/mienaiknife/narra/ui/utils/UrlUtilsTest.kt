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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class UrlUtilsTest {
    @Test
    fun `isPublicUrl blocks localhost and internal IPs`() {
        assertFalse("Should block localhost", UrlUtils.isPublicUrl("http://localhost"))
        assertFalse("Should block 127.0.0.1", UrlUtils.isPublicUrl("http://127.0.0.1"))
        assertFalse("Should block 10.0.0.1", UrlUtils.isPublicUrl("http://10.0.0.1"))
        assertFalse("Should block 192.168.1.1", UrlUtils.isPublicUrl("http://192.168.1.1"))
        assertFalse("Should block 172.16.0.1", UrlUtils.isPublicUrl("http://172.16.0.1"))
    }

    @Test
    fun `isPublicUrl allows public domains`() {
        assertTrue("Should allow example.com", UrlUtils.isPublicUrl("https://example.com"))
        assertTrue("Should allow www hosts", UrlUtils.isPublicUrl("https://www.wikipedia.org/wiki/Kotlin"))
    }

    @Test
    fun `isPublicUrl returns false for invalid URLs`() {
        assertFalse("Should return false for invalid scheme", UrlUtils.isPublicUrl("not-a-url"))
        assertFalse("Should return false for empty string", UrlUtils.isPublicUrl(""))
    }

    @Test
    fun `getDomainName extracts domain correctly`() {
        assertEquals("example.com", UrlUtils.getDomainName("https://example.com/path"))
        assertEquals("wikipedia.org", UrlUtils.getDomainName("https://www.wikipedia.org/wiki/Kotlin"))
        assertEquals("blog.jetbrains.com", UrlUtils.getDomainName("https://blog.jetbrains.com/kotlin/"))
        assertEquals("sub.example.com", UrlUtils.getDomainName("http://sub.example.com"))
    }

    @Test
    fun `faviconUrl uses the site's own origin`() {
        assertEquals("https://example.com/favicon.ico", UrlUtils.faviconUrl("https://example.com"))
        assertEquals(
            "https://example.com/favicon.ico",
            UrlUtils.faviconUrl("https://example.com/articles/one?utm_source=feed"),
        )
        assertEquals("http://example.com/favicon.ico", UrlUtils.faviconUrl("http://example.com/path"))
        assertEquals(
            "https://blog.jetbrains.com/favicon.ico",
            UrlUtils.faviconUrl("https://blog.jetbrains.com/kotlin/"),
        )
    }

    @Test
    fun `faviconUrl keeps http and https schemes`() {
        assertEquals("http://example.com/favicon.ico", UrlUtils.faviconUrl("http://example.com"))
        assertEquals("https://example.com/favicon.ico", UrlUtils.faviconUrl("https://example.com"))
    }

    @Test
    fun `faviconUrl extracts the host from feed urls`() {
        assertEquals(
            "https://feeds.example.org/favicon.ico",
            UrlUtils.faviconUrl("https://feeds.example.org/rss.xml"),
        )
    }

    @Test
    fun `faviconUrl returns null for missing or invalid urls`() {
        assertNull(UrlUtils.faviconUrl(null))
        assertNull(UrlUtils.faviconUrl(""))
        assertNull(UrlUtils.faviconUrl("   "))
        assertNull(UrlUtils.faviconUrl("example.com"))
        assertNull(UrlUtils.faviconUrl("not a url"))
        assertNull(UrlUtils.faviconUrl("https://"))
    }

    @Test
    fun `faviconUrl returns null for non-http urls`() {
        assertNull(UrlUtils.faviconUrl("ftp://example.com/file.txt"))
        assertNull(UrlUtils.faviconUrl("file:///etc/passwd"))
        assertNull(UrlUtils.faviconUrl("epub://book/chapter"))
    }

    @Test
    fun `faviconUrl never points at a third-party icon service`() {
        val sources =
            listOf(
                "https://example.com/post",
                "http://news.example.org/rss",
                "https://blog.jetbrains.com/kotlin/",
            )
        sources.forEach { source ->
            val favicon = requireNotNull(UrlUtils.faviconUrl(source)) { "Expected a favicon for $source" }
            val sourceUri = URI(source)
            val faviconUri = URI(favicon)
            assertEquals("Favicon must be served by $source", sourceUri.host, faviconUri.host)
            assertEquals("Favicon must keep the scheme of $source", sourceUri.scheme, faviconUri.scheme)
            assertEquals("Favicon must use the site's root path", "/favicon.ico", faviconUri.path)
            assertNull("Favicon must not carry a query string", faviconUri.query)
        }
    }
}
