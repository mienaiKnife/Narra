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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageDataSourceImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) : ImageDataSource {
    private val imagesDir: File by lazy {
        File(context.filesDir, "images").apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun downloadAndSaveImage(
        url: String,
        fileName: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val base = fileName.substringBeforeLast('.')
                val knownExtension = extensionFromContentType(response.header("Content-Type"))
                var file = File(imagesDir, "$base.${knownExtension ?: TEMP_EXTENSION}")
                FileOutputStream(file).use { output ->
                    body.byteStream().copyTo(output)
                }
                if (knownExtension == null) {
                    file = renameWithSniffedExtension(file, base)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageDataSource", "Failed to download image: $url", e)
            null
        }
    }

    override suspend fun saveImage(
        data: ByteArray,
        fileName: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val base = fileName.substringBeforeLast('.')
            val file = File(imagesDir, "$base.${sniffExtension(data)}")
            FileOutputStream(file).use { output ->
                output.write(data)
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageDataSource", "Failed to save image: $fileName", e)
            null
        }
    }

    override suspend fun pruneUnreferenced(referencedPaths: Set<String>) {
        withContext(Dispatchers.IO) {
            imagesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.absolutePath !in referencedPaths) {
                    if (!file.delete()) {
                        android.util.Log.w("ImageDataSource", "Failed to delete orphaned image: ${file.name}")
                    }
                }
            }
        }
    }

    private fun renameWithSniffedExtension(
        file: File,
        base: String,
    ): File {
        val renamed = File(imagesDir, "$base.${sniffExtension(file)}")
        return if (renamed != file && file.renameTo(renamed)) renamed else file
    }

    private fun extensionFromContentType(contentType: String?): String? = when (contentType?.substringBefore(';')?.trim()?.lowercase()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/bmp", "image/x-ms-bmp" -> "bmp"
        "image/heic", "image/heif" -> "heic"
        else -> null
    }

    private fun sniffExtension(file: File): String = try {
        val header = ByteArray(MAGIC_LENGTH)
        val read = file.inputStream().use { it.read(header) }
        sniffExtension(if (read > 0) header.copyOf(read) else ByteArray(0))
    } catch (e: Exception) {
        android.util.Log.w("ImageDataSource", "Failed to sniff image type: ${file.name}", e)
        DEFAULT_EXTENSION
    }

    private fun sniffExtension(bytes: ByteArray): String = when {
        bytes.startsWith(PNG_MAGIC) -> "png"
        bytes.startsWith(JPEG_MAGIC) -> "jpg"
        bytes.startsWith(GIF_MAGIC) -> "gif"
        bytes.isWebP() -> "webp"
        bytes.startsWith(BMP_MAGIC) -> "bmp"
        else -> DEFAULT_EXTENSION
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean = size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.isWebP(): Boolean = size >= 12 && copyOfRange(0, 4).contentEquals(WEBP_RIFF_MAGIC) && copyOfRange(8, 12).contentEquals(WEBP_TAG)

    private companion object {
        const val TEMP_EXTENSION = "tmp"
        const val DEFAULT_EXTENSION = "bin"
        const val MAGIC_LENGTH = 12
        val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val GIF_MAGIC = byteArrayOf(0x47, 0x49, 0x46)
        val BMP_MAGIC = byteArrayOf(0x42, 0x4D)
        val WEBP_RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        val WEBP_TAG = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    }
}
