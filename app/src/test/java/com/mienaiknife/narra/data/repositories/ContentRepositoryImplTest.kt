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
package com.mienaiknife.narra.data.repositories

import com.mienaiknife.narra.domain.NarraError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ContentRepositoryImplTest {
    private val articleRepository: ArticleRepositoryImpl = mock()
    private val feedRepository: FeedRepositoryImpl = mock()
    private val importExportRepository: ImportExportRepositoryImpl = mock()

    private lateinit var contentRepository: ContentRepositoryImpl

    @Before
    fun setUp() {
        contentRepository =
            ContentRepositoryImpl(
                articleRepository,
                feedRepository,
                importExportRepository,
            )
    }

    @Test
    fun `downloadWebPage returns failure when offline`() = runBlocking {
        whenever(articleRepository.downloadWebPage("https://example.com"))
            .thenReturn(Result.failure(NarraError.Network.NoConnection()))

        val result = contentRepository.downloadWebPage("https://example.com")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NarraError.Network.NoConnection)
    }

    @Test
    fun `importOpml returns success with count`() = runBlocking {
        whenever(importExportRepository.importOpml(any())).thenReturn(Result.success(0))

        val result = contentRepository.importOpml("".byteInputStream())

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == 0)
    }
}
