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
import com.mienaiknife.narra.data.local.dao.TtsModelDao
import com.mienaiknife.narra.data.local.entities.TtsModelEntity
import com.mienaiknife.narra.domain.models.TtsModel
import com.mienaiknife.narra.domain.models.TtsModelType
import com.mienaiknife.narra.domain.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsModelDao: TtsModelDao,
    private val okHttpClient: OkHttpClient
) : ModelRepository {

    private val modelsDir = File(context.filesDir, "models")

    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }

    override fun getAvailableModels(): Flow<List<TtsModel>> {
        return ttsModelDao.getAllModels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Ensures that the default models are present in the database.
     * Should be called during app startup or when entering the voices settings.
     */
    suspend fun ensureDefaultModelsInitialized() = withContext(Dispatchers.IO) {
        val count = ttsModelDao.getModelCount()
        if (count == 0) {
            initializeDefaultModels()
        }
    }

    private suspend fun initializeDefaultModels() {
        val defaultModels = listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Amy (English, US)",
                language = "en-US",
                description = "Low quality, fast American English female voice",
                type = TtsModelType.VITS,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.onnx",
                tokensUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/tokens.txt",
                sizeBytes = 28000000 
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Ryan (English, US)",
                language = "en-US",
                description = "Medium quality American English male voice",
                type = TtsModelType.VITS,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-ryan-medium.onnx",
                tokensUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/tokens.txt",
                sizeBytes = 60000000 
            ),
            TtsModel(
                id = "kokoro-en-v0_19",
                name = "Kokoro (English)",
                language = "en",
                description = "High quality Kokoro TTS model",
                type = TtsModelType.KOKORO,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/model-small.onnx",
                tokensUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/tokens.txt",
                extraUrls = mapOf("voices.bin" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/voices.bin"),
                sizeBytes = 80000000
            ),
            TtsModel(
                id = "matcha-en-ljspeech",
                name = "Matcha (English)",
                language = "en",
                description = "High quality Matcha TTS model",
                type = TtsModelType.MATCHA,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-en-ljspeech.onnx",
                tokensUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/tokens.txt",
                extraUrls = mapOf("vocoder.onnx" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/hifigan_v2.onnx"),
                sizeBytes = 150000000
            )
        )
        defaultModels.forEach { model ->
            ttsModelDao.insertModel(TtsModelEntity.fromDomain(model))
        }
    }

    override suspend fun downloadModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = ttsModelDao.getModelById(modelId) ?: return@withContext Result.failure(Exception("Model not found"))
        val model = entity.toDomain()

        val targetDir = File(modelsDir, modelId)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        try {
            val filesToDownload = mutableListOf<Pair<String, String>>()
            filesToDownload.add(model.modelUrl to "model.onnx")
            filesToDownload.add(model.tokensUrl to "tokens.txt")
            model.extraUrls.forEach { (fileName, url) ->
                filesToDownload.add(url to fileName)
            }

            val totalFiles = filesToDownload.size
            filesToDownload.forEachIndexed { index, (url, fileName) ->
                val targetFile = File(targetDir, fileName)
                downloadFile(url, targetFile) { fileProgress ->
                    val overallProgress = (index + fileProgress) / totalFiles
                    // Update progress in DB every ~1% to avoid excessive DB writes
                    if (overallProgress - model.progress > 0.01f || overallProgress >= 0.99f) {
                        ttsModelDao.updateProgress(modelId, overallProgress)
                    }
                }
            }

            ttsModelDao.updateProgress(modelId, 1.0f)
            ttsModelDao.updateDownloadStatus(modelId, true, targetDir.absolutePath)
            Result.success(Unit)
        } catch (e: Exception) {
            ttsModelDao.updateProgress(modelId, 0f)
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File, onProgress: suspend (Float) -> Unit = {}) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")
            
            val body = response.body ?: throw Exception("Response body is null")
            val contentLength = body.contentLength()
            
            body.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalBytesRead.toFloat() / contentLength)
                        }
                    }
                }
            }
        }
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val model = ttsModelDao.getModelById(modelId) ?: return@withContext Result.failure(Exception("Model not found"))
        
        model.dataDir?.let {
            val dir = File(it)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
        
        ttsModelDao.updateDownloadStatus(modelId, false, null)
        Result.success(Unit)
    }

    override suspend fun getModelPath(modelId: String): String? {
        return ttsModelDao.getModelById(modelId)?.dataDir
    }
}
