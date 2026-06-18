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
}
