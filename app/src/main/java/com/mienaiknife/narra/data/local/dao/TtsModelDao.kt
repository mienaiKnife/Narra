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
import com.mienaiknife.narra.data.local.entities.TtsModelEntity
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

    @Query("UPDATE tts_models SET isDownloaded = :isDownloaded, dataDir = :dataDir WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, dataDir: String?)

    @Query("UPDATE tts_models SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)

    @Query("UPDATE tts_models SET lastError = :error WHERE id = :id")
    suspend fun updateError(id: String, error: String?)

    @Query("DELETE FROM tts_models WHERE id = :id")
    suspend fun deleteModel(id: String)

    @Query("DELETE FROM tts_models WHERE type = 'KOKORO' AND id != 'kokoro-en-v0_19'")
    suspend fun deleteStaleKokoroModels()
}
