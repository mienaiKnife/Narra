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
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue("Should allow google.com", UrlUtils.isPublicUrl("https://www.google.com/search?q=test"))
    }

    @Test
    fun `isPublicUrl returns false for invalid URLs`() {
        assertFalse("Should return false for invalid scheme", UrlUtils.isPublicUrl("not-a-url"))
        assertFalse("Should return false for empty string", UrlUtils.isPublicUrl(""))
    }

    @Test
    fun `getDomainName extracts domain correctly`() {
        assertEquals("example.com", UrlUtils.getDomainName("https://example.com/path"))
        assertEquals("google.com", UrlUtils.getDomainName("https://www.google.com/search"))
        assertEquals("blog.jetbrains.com", UrlUtils.getDomainName("https://blog.jetbrains.com/kotlin/"))
        assertEquals("sub.example.com", UrlUtils.getDomainName("http://sub.example.com"))
    }

    @Test
    fun `faviconUrl builds a favicon service url`() {
        assertEquals(
            "https://www.google.com/s2/favicons?domain=https://example.com&sz=128",
            UrlUtils.faviconUrl("https://example.com"),
        )
        assertEquals(
            "https://www.google.com/s2/favicons?domain=https://example.com&sz=64",
            UrlUtils.faviconUrl("https://example.com", size = 64),
        )
    }

    @Test
    fun `faviconUrl returns null for blank or missing urls`() {
        assertEquals(null, UrlUtils.faviconUrl(null))
        assertEquals(null, UrlUtils.faviconUrl(""))
        assertEquals(null, UrlUtils.faviconUrl("   "))
    }
}
