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

class ImageDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : ImageDataSource {

    private val imagesDir: File by lazy {
        File(context.filesDir, "images").apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun downloadAndSaveImage(url: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val file = File(imagesDir, fileName)
                FileOutputStream(file).use { output ->
                    body.byteStream().copyTo(output)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageDataSource", "Failed to download image: $url", e)
            null
        }
    }

    override suspend fun saveImage(data: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagesDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(data)
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageDataSource", "Failed to save image: $fileName", e)
            null
        }
    }
}
