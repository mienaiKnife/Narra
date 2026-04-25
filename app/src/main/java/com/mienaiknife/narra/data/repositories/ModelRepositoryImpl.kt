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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mienaiknife.narra.data.local.dao.TtsModelDao
import com.mienaiknife.narra.data.local.entities.TtsModelEntity
import com.mienaiknife.narra.data.workers.DownloadWorker
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
    @ApplicationContext private val context: Context,
    private val ttsModelDao: TtsModelDao,
    private val okHttpClient: OkHttpClient,
    private val workManager: WorkManager
) : ModelRepository {

    private val modelsDir = File(context.filesDir, "tts_models")

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

    override suspend fun ensureDefaultModelsInitialized() = withContext(Dispatchers.IO) {
        if (ttsModelDao.getModelCount() == 0) {
            initializeDefaultModels()
        }
    }

    private suspend fun initializeDefaultModels() {
        val defaultModels = listOf(
            TtsModel(
                id = "vits-piper-en_US-amy-low",
                name = "Piper Amy",
                language = "en",
                description = "Low quality, fast American English female voice",
                type = TtsModelType.VITS,
                modelUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2",
                sizeBytes = 28000000 
            ),
            TtsModel(
                id = "vits-piper-en_US-ryan-medium",
                name = "Piper Ryan",
                language = "en",
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
                name = "Matcha Icefall",
                language = "en",
                description = "High quality American English female TTS model",
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
                    dataDir = existing.dataDir,
                    lastError = existing.lastError
                )
                ttsModelDao.insertModel(updated)
            } else {
                ttsModelDao.insertModel(TtsModelEntity.fromDomain(model))
            }
        }
    }

    override fun enqueueDownload(modelId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("modelId" to modelId))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                java.util.concurrent.TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$modelId",
            ExistingWorkPolicy.KEEP, // Keep existing work if already running
            downloadRequest
        )
    }

    override suspend fun downloadModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = ttsModelDao.getModelById(modelId) ?: return@withContext Result.failure(Exception("Model not found"))
        val model = entity.toDomain()

        val targetDir = File(modelsDir, modelId)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        try {
            // Ensure progress is at least slightly above 0 so UI shows progress bar immediately
            ttsModelDao.updateProgress(modelId, 0.01f)
            ttsModelDao.updateError(modelId, null)

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
                    ttsModelDao.updateProgress(modelId, overallProgress.coerceAtLeast(0.01f))
                    android.util.Log.d("ModelRepository", "Download progress: $overallProgress")
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
                    android.util.Log.d("ModelRepository", "Download progress: $overallProgress")
                }
                currentStep++

                if (model.tokensUrl.isNotEmpty()) {
                    val tokensFile = File(targetDir, "tokens.txt")
                    downloadFile(model.tokensUrl, tokensFile) { progress ->
                        val overallProgress = (currentStep + progress) / totalSteps
                        ttsModelDao.updateProgress(modelId, overallProgress)
                        android.util.Log.d("ModelRepository", "Download progress: $overallProgress")
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
                    android.util.Log.d("ModelRepository", "Download progress: $overallProgress")
                }
                currentStep++
            }

            ttsModelDao.updateProgress(modelId, 1.0f)
            ttsModelDao.updateDownloadStatus(modelId, true, targetDir.absolutePath)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ModelRepository", "Failed to download model $modelId", e)
            ttsModelDao.updateError(modelId, e.message ?: "Unknown error")
            // We don't delete the directory here to allow for resumable downloads.
            // But we should reset the progress if it was a total failure (though resumable logic handles it)
            Result.failure(e)
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File, onProgress: suspend (Float) -> Unit = {}) {
        var downloadedBytes = if (targetFile.exists()) targetFile.length() else 0L
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Narra/1.0")
            .apply {
                if (downloadedBytes > 0) {
                    addHeader("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()
        
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 416) {
                    // Requested Range Not Satisfiable - might mean it's already finished
                    onProgress(1.0f)
                    return
                }
                
                if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code} ${response.message}")
                
                val body = response.body ?: throw Exception("Response body is null")
                val contentLength = body.contentLength()
                
                val isPartial = response.code == 206
                if (!isPartial) {
                    // Server doesn't support range or something went wrong, start from scratch
                    downloadedBytes = 0L
                }
                
                val totalLength = if (isPartial) downloadedBytes + contentLength else contentLength
                
                android.util.Log.d("ModelRepository", "Downloading $url, isPartial: $isPartial, totalLength: $totalLength")
                
                val bodyStream = body.byteStream()
                
                FileOutputStream(targetFile, isPartial).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var lastUpdateTime = 0L
                    
                    while (true) {
                        bytesRead = bodyStream.read(buffer)
                        if (bytesRead == -1) break
                        
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > 500) {
                            if (totalLength > 0) {
                                val p = downloadedBytes.toFloat() / totalLength
                                onProgress(p)
                            } else {
                                onProgress(0.01f) 
                            }
                            lastUpdateTime = currentTime
                        }
                    }
                    onProgress(1.0f)
                }
                android.util.Log.d("ModelRepository", "Finished downloading $url")
            }
        } catch (e: Exception) {
            android.util.Log.e("ModelRepository", "Exception during download of $url", e)
            throw e
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
        // Models might be in a subdirectory after extraction
        val allFiles = targetDir.walkTopDown().toList()
        
        // Find main model file
        val onnxFile = allFiles.find { it.name.endsWith(".onnx") }
        if (onnxFile != null && onnxFile.parentFile != targetDir) {
            onnxFile.renameTo(File(targetDir, "model.onnx"))
        } else if (onnxFile != null) {
            onnxFile.renameTo(File(targetDir, "model.onnx"))
        }

        // Find tokens file
        val tokensFile = allFiles.find { it.name == "tokens.txt" }
        if (tokensFile != null && tokensFile.parentFile != targetDir) {
            tokensFile.renameTo(File(targetDir, "tokens.txt"))
        }

        // Cleanup subdirectories
        targetDir.listFiles()?.forEach { 
            if (it.isDirectory) it.deleteRecursively()
        }
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(modelsDir, modelId)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            ttsModelDao.updateDownloadStatus(modelId, false, null)
            ttsModelDao.updateProgress(modelId, 0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModelPath(modelId: String): String? = withContext(Dispatchers.IO) {
        val targetDir = File(modelsDir, modelId)
        if (targetDir.exists()) targetDir.absolutePath else null
    }
}
