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
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class ImageDataSourceImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var dataSource: ImageDataSourceImpl

    @Before
    fun setUp() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)
        dataSource = ImageDataSourceImpl(context, mock<OkHttpClient>())
    }

    @Test
    fun `saveImage uses the sniffed extension instead of the requested one`() = runTest {
        val png = dataSource.saveImage(pngBytes(), "cover.png")
        val jpeg = dataSource.saveImage(jpegBytes(), "cover.png")

        assertTrue("PNG should be saved as .png: $png", png!!.endsWith(".png"))
        assertTrue("JPEG should be saved as .jpg: $jpeg", jpeg!!.endsWith(".jpg"))
    }

    @Test
    fun `pruneUnreferenced deletes only orphaned images`() = runTest {
        val referenced = File(tempFolder.root, "images/referenced.png")
        referenced.parentFile?.mkdirs()
        referenced.writeBytes(pngBytes())
        val orphan = File(tempFolder.root, "images/orphan.png")
        orphan.writeBytes(pngBytes())

        dataSource.pruneUnreferenced(setOf(referenced.absolutePath))

        assertTrue("Referenced image should be kept", referenced.exists())
        assertFalse("Orphaned image should be deleted", orphan.exists())
    }

    private fun pngBytes() = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)

    private fun jpegBytes() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
}
