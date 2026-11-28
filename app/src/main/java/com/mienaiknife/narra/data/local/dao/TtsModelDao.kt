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
package com.mienaiknife.narra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mienaiknife.narra.data.local.entities.TtsModelEntity
import com.mienaiknife.narra.data.local.entities.withPreservedState
import kotlinx.coroutines.flow.Flow

@Dao
interface TtsModelDao {
    @Query("SELECT * FROM tts_models")
    fun getAllModels(): Flow<List<TtsModelEntity>>

    @Query("SELECT * FROM tts_models WHERE id = :id")
    suspend fun getModelById(id: String): TtsModelEntity?

    @Query("SELECT COUNT(*) FROM tts_models")
    suspend fun getModelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: TtsModelEntity)

    /**
     * Refreshes the catalog metadata for an existing row in a single transaction, preserving the
     * download state and the user's selected speaker. Does nothing when the row is gone.
     */
    @Transaction
    suspend fun updateCatalogMetadata(model: TtsModelEntity) {
        val existing = getModelById(model.id) ?: return
        insertModel(model.withPreservedState(existing))
    }

    @Query("UPDATE tts_models SET isDownloaded = :isDownloaded, dataDir = :dataDir WHERE id = :id")
    suspend fun updateDownloadStatus(
        id: String,
        isDownloaded: Boolean,
        dataDir: String?,
    )

    @Query("UPDATE tts_models SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        progress: Float,
    )

    @Query("UPDATE tts_models SET lastError = :error WHERE id = :id")
    suspend fun updateError(
        id: String,
        error: String?,
    )

    @Query("DELETE FROM tts_models WHERE id = :id")
    suspend fun deleteModel(id: String)
}
