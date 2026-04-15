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

package com.mienaiknife.narra.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mienaiknife.narra.domain.models.TtsModel

@Entity(tableName = "tts_models")
data class TtsModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
    val description: String,
    val modelUrl: String,
    val tokensUrl: String,
    val dataDir: String?,
    val sizeBytes: Long,
    val isDownloaded: Boolean,
    val progress: Float
) {
    fun toDomain() = TtsModel(
        id = id,
        name = name,
        language = language,
        description = description,
        modelUrl = modelUrl,
        tokensUrl = tokensUrl,
        dataDir = dataDir,
        sizeBytes = sizeBytes,
        isDownloaded = isDownloaded,
        progress = progress
    )

    companion object {
        fun fromDomain(model: TtsModel) = TtsModelEntity(
            id = model.id,
            name = model.name,
            language = model.language,
            description = model.description,
            modelUrl = model.modelUrl,
            tokensUrl = model.tokensUrl,
            dataDir = model.dataDir,
            sizeBytes = model.sizeBytes,
            isDownloaded = model.isDownloaded,
            progress = model.progress
        )
    }
}
