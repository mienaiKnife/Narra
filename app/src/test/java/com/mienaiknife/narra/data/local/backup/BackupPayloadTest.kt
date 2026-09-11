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
package com.mienaiknife.narra.data.local.backup

import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPayloadTest {
    @Test
    fun `payload round trips feeds and playback progress`() {
        val articles =
            listOf(
                ArticleEntity(
                    id = "a1",
                    title = "Article",
                    source = "Example",
                    content = "body",
                    url = "https://example.com/a1",
                    feedUrl = "https://example.com/rss",
                    progress = 0.5f,
                    currentParagraphIndex = 3,
                    currentWordOffset = 7,
                    isFromFeed = true,
                    isInInbox = true,
                ),
            )
        val payload =
            BackupPayload(
                version = BackupPayload.CURRENT_VERSION,
                exportedAt = 123L,
                feeds = listOf(FeedEntity(url = "https://example.com/rss", title = "Example")),
                articles = articles,
            )

        val restored = BackupPayload.decode(BackupPayload.encode(payload))

        assertEquals(payload, restored)
    }

    @Test
    fun `decode ignores unknown fields for forward compatibility`() {
        val json =
            """
            {
              "version": 1,
              "exportedAt": 1,
              "feeds": [],
              "articles": [],
              "futureField": "ignored"
            }
            """.trimIndent()

        val payload = BackupPayload.decode(json)

        assertEquals(BackupPayload.CURRENT_VERSION, payload.version)
        assertEquals(0, payload.feeds.size)
        assertEquals(0, payload.articles.size)
    }
}
