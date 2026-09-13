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

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

class EpubDataSourceImplTest {
    private lateinit var epubDataSource: EpubDataSource
    private val imageDataSource: ImageDataSource = mock()
    private val context: Context = mock()

    @Before
    fun setUp() {
        epubDataSource = EpubDataSourceImpl(imageDataSource)
    }

    @Test
    fun parseEpub_returnsFailure_forInvalidStream() = runTest {
        // Stub imageDataSource to avoid any actual processing if called
        whenever(imageDataSource.saveImage(any(), any())).thenReturn(null)

        val inputStream = ByteArrayInputStream("not an epub".toByteArray())
        val result = epubDataSource.parseEpub(context, inputStream, "Test Title")
        assertTrue(result.isFailure)
    }

    @Test
    fun parseEpub_returnsFailure_forTruncatedZipStream() = runTest {
        whenever(imageDataSource.saveImage(any(), any())).thenReturn(null)

        // Starts with a valid ZIP local header but has no central directory, which used to
        // send Epublib's streaming reader into an infinite loop.
        val truncated = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(32) { it.toByte() }
        val inputStream = ByteArrayInputStream(truncated)
        val result = epubDataSource.parseEpub(context, inputStream, "Test Title")
        assertTrue(result.isFailure)
    }
}
