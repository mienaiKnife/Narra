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

package com.mienaiknife.narra.domain.repository

import com.mienaiknife.narra.domain.models.TtsModel
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    /**
     * Returns a list of all available TTS models.
     */
    fun getAvailableModels(): Flow<List<TtsModel>>

    /**
     * Enqueues a model download task using WorkManager.
     * @param modelId The ID of the model to download.
     */
    fun enqueueDownload(modelId: String)

    /**
     * Triggers the download of a model.
     * @param modelId The ID of the model to download.
     */
    suspend fun downloadModel(modelId: String): Result<Unit>

    /**
     * Deletes a downloaded model from local storage.
     * @param modelId The ID of the model to delete.
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Returns the local directory path where the model is stored, if downloaded.
     * @param modelId The ID of the model.
     */
    suspend fun getModelPath(modelId: String): String?

    /**
     * Ensures that the default models are present in the database and updated if needed.
     */
    suspend fun ensureDefaultModelsInitialized()
}
