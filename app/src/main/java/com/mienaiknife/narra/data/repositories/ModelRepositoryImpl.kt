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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
     * Ensures that the default models are present in the database and updated if needed.
     * Should be called during app startup or when entering the voices settings.
     */
    override suspend fun ensureDefaultModelsInitialized() = withContext(Dispatchers.IO) {
        ttsModelDao.deleteStaleKokoroModels()
        initializeDefaultModels()
    }

    private suspend fun initializeDefaultModels() {
        val defaultModels = listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Piper Amy (English, US)",
                language = "en-US",
                description = "Low quality, fast American English female voice",
                type = TtsModelType.VITS,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2",
                sizeBytes = 28000000 
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Piper Ryan (English, US)",
                language = "en-US",
                description = "Medium quality American English male voice",
                type = TtsModelType.VITS,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-ryan-medium.tar.bz2",
                sizeBytes = 60000000 
            ),
            TtsModel(
                id = "kokoro-en-v0_19",
                name = "Kokoro (English)",
                language = "en",
                description = "High quality multi-voice Kokoro TTS",
                type = TtsModelType.KOKORO,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2",
                extraUrls = mapOf("voices.bin" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/voices.bin"),
                sizeBytes = 80000000
            ),
            TtsModel(
                id = "matcha-icefall-en_US-ljspeech",
                name = "Matcha Icefall (English)",
                language = "en-US",
                description = "High quality Matcha TTS model",
                type = TtsModelType.MATCHA,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-en_US-ljspeech.tar.bz2",
                extraUrls = mapOf("vocoder.onnx" to "https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx"),
                sizeBytes = 150000000
            )
        )
        defaultModels.forEach { model ->
            val existing = ttsModelDao.getModelById(model.id)
            if (existing != null) {
                // Update only the metadata, preserve download status and dataDir
                val updated = TtsModelEntity.fromDomain(model).copy(
                    isDownloaded = existing.isDownloaded,
                    progress = existing.progress,
                    dataDir = existing.dataDir
                )
                ttsModelDao.insertModel(updated)
            } else {
                ttsModelDao.insertModel(TtsModelEntity.fromDomain(model))
            }
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
            val isArchive = model.modelUrl.endsWith(".tar.bz2")
            val extraFiles = model.extraUrls.toList()
            // Archives contain everything (model + tokens). Non-archives might need tokens separately.
            val totalSteps = if (isArchive) {
                1 + extraFiles.size
            } else {
                1 + (if (model.tokensUrl.isNotEmpty()) 1 else 0) + extraFiles.size
            }
            var currentStep = 0

            if (isArchive) {
                val archiveFile = File(targetDir, "archive.tar.bz2")
                android.util.Log.d("ModelRepository", "Downloading archive: ${model.modelUrl}")
                downloadFile(model.modelUrl, archiveFile) { progress ->
                    val overallProgress = (currentStep + progress) / totalSteps
                    // Ensure progress is at least slightly above 0 so UI shows progress bar
                    ttsModelDao.updateProgress(modelId, overallProgress.coerceAtLeast(0.01f))
                }
                android.util.Log.d("ModelRepository", "Extracting archive...")
                extractTarBz2(archiveFile, targetDir)
                archiveFile.delete()
                
                android.util.Log.d("ModelRepository", "Moving model files...")
                findAndMoveModelFiles(targetDir)
                currentStep++
            } else {
                val targetFile = File(targetDir, "model.onnx")
                downloadFile(model.modelUrl, targetFile) { progress ->
                    val overallProgress = (currentStep + progress) / totalSteps
                    ttsModelDao.updateProgress(modelId, overallProgress)
                }
                currentStep++

                if (model.tokensUrl.isNotEmpty()) {
                    val tokensFile = File(targetDir, "tokens.txt")
                    downloadFile(model.tokensUrl, tokensFile) { progress ->
                        val overallProgress = (currentStep + progress) / totalSteps
                        ttsModelDao.updateProgress(modelId, overallProgress)
                    }
                    currentStep++
                }
            }

            // Download extra files (like voices.bin or vocoder.onnx)
            extraFiles.forEach { (fileName, url) ->
                val targetFile = File(targetDir, fileName)
                downloadFile(url, targetFile) { progress ->
                    val overallProgress = (currentStep + progress) / totalSteps
                    ttsModelDao.updateProgress(modelId, overallProgress)
                }
                currentStep++
            }

            ttsModelDao.updateProgress(modelId, 1.0f)
            ttsModelDao.updateDownloadStatus(modelId, true, targetDir.absolutePath)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ModelRepository", "Failed to download model $modelId", e)
            ttsModelDao.updateProgress(modelId, 0f)
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File, onProgress: suspend (Float) -> Unit = {}) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Narra/1.0")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code} ${response.message}")
            
            val body = response.body ?: throw Exception("Response body is null")
            val contentLength = body.contentLength()
            
            android.util.Log.d("ModelRepository", "Downloading $url, size: $contentLength")
            
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
                        } else {
                            // If content length is unknown, we can't show accurate progress,
                            // but we can still signal that activity is happening.
                            // We use a small fake progress to keep the UI in "downloading" state.
                            onProgress(0.01f) 
                        }
                    }
                }
            }
            android.util.Log.d("ModelRepository", "Finished downloading $url")
        }
    }

    private fun extractTarBz2(archiveFile: File, targetDir: File) {
        FileInputStream(archiveFile).use { fis ->
            BZip2CompressorInputStream(fis).use { bzis ->
                TarArchiveInputStream(bzis).use { tais ->
                    var entry = tais.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val curFile = File(targetDir, entry.name)
                            curFile.parentFile?.mkdirs()
                            FileOutputStream(curFile).use { fos ->
                                tais.copyTo(fos)
                            }
                        }
                        entry = tais.nextEntry
                    }
                }
            }
        }
    }

    private fun findAndMoveModelFiles(targetDir: File) {
        // 1. Flatten all files to the root directory
        targetDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val destFile = File(targetDir, file.name)
            if (file.absolutePath != destFile.absolutePath) {
                file.copyTo(destFile, overwrite = true)
            }
        }

        // 2. Clean up all subdirectories
        targetDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.deleteRecursively()
            }
        }

        // 3. Standardize the main model name if necessary.
        // Some models (like VITS/Matcha/Kokoro) expect a 'model.onnx', while others 
        // (like ZipVoice/Pocket/Supertonic) use specific filenames for multiple components.
        val rootFiles = targetDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val specialOnnxNames = listOf(
            "vocoder.onnx", "encoder.onnx", "decoder.onnx",
            "lm_flow.onnx", "lm_main.onnx", "text_conditioner.onnx",
            "duration_predictor.onnx", "text_encoder.onnx", "vector_estimator.onnx", "unicode_indexer.onnx"
        )

        val genericOnnxFiles = rootFiles.filter {
            it.name.endsWith(".onnx") && it.name !in specialOnnxNames && it.name != "model.onnx"
        }

        // If there's exactly one generic .onnx file and no 'model.onnx' exists, rename it to 'model.onnx'
        if (genericOnnxFiles.size == 1 && rootFiles.none { it.name == "model.onnx" }) {
            genericOnnxFiles[0].renameTo(File(targetDir, "model.onnx"))
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
