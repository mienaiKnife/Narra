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

import android.content.Context
import androidx.work.WorkManager
import com.mienaiknife.narra.data.local.dao.TtsModelDao
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.FileOutputStream

class ModelRepositorySecurityTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: ModelRepositoryImpl
    private lateinit var targetDir: File

    @Before
    fun setUp() {
        val mockContext = mock(Context::class.java)
        val mockDao = mock(TtsModelDao::class.java)
        val mockClient = mock(OkHttpClient::class.java)
        val mockWorkManager = mock(WorkManager::class.java)

        val filesDir = tempFolder.newFolder("files")
        `when`(mockContext.filesDir).thenReturn(filesDir)

        repository = ModelRepositoryImpl(mockContext, mockDao, mockClient, mockWorkManager)
        targetDir = tempFolder.newFolder("target")
    }

    @Test
    fun `extractTarBz2 throws SecurityException on path traversal`() {
        val archiveFile = tempFolder.newFile("malicious.tar.bz2")

        // Create a malicious tar.bz2 with a ../ entry
        FileOutputStream(archiveFile).use { fos ->
            BZip2CompressorOutputStream(fos).use { bzos ->
                TarArchiveOutputStream(bzos).use { tos ->
                    // Use a path that would go outside the target directory
                    val entry = TarArchiveEntry("../outside.txt")
                    val content = "malicious content".toByteArray()
                    entry.size = content.size.toLong()
                    tos.putArchiveEntry(entry)
                    tos.write(content)
                    tos.closeArchiveEntry()
                }
            }
        }

        assertThrows(SecurityException::class.java) {
            runBlocking {
                repository.extractTarBz2(archiveFile, targetDir)
            }
        }
    }

    @Test
    fun `extractTarBz2 succeeds on valid entries`() = runBlocking {
        val archiveFile = tempFolder.newFile("valid.tar.bz2")

        FileOutputStream(archiveFile).use { fos ->
            BZip2CompressorOutputStream(fos).use { bzos ->
                TarArchiveOutputStream(bzos).use { tos ->
                    val entry = TarArchiveEntry("model.onnx")
                    val content = "model content".toByteArray()
                    entry.size = content.size.toLong()
                    tos.putArchiveEntry(entry)
                    tos.write(content)
                    tos.closeArchiveEntry()
                }
            }
        }

        repository.extractTarBz2(archiveFile, targetDir)

        val extractedFile = File(targetDir, "model.onnx")
        org.junit.Assert.assertTrue(extractedFile.exists())
        org.junit.Assert.assertEquals("model content", extractedFile.readText())
    }
}
