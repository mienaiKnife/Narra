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
import com.mienaiknife.narra.domain.models.TtsModelType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Entity(tableName = "tts_models")
data class TtsModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
    val description: String,
    val type: String, // Store as String for Room compatibility
    val modelUrl: String,
    val tokensUrl: String,
    val extraUrls: String = "", // JSON map of extra file URLs
    val dataDir: String?,
    val sizeBytes: Long,
    val isDownloaded: Boolean,
    val progress: Float,
    val speakerId: Int? = null,
    val lastError: String? = null
) {
    fun toDomain() = TtsModel(
        id = id,
        name = name,
        language = language,
        description = description,
        type = TtsModelType.valueOf(type),
        modelUrl = modelUrl,
        tokensUrl = tokensUrl,
        extraUrls = parseExtraUrls(extraUrls),
        dataDir = dataDir,
        sizeBytes = sizeBytes,
        isDownloaded = isDownloaded,
        progress = progress,
        speakerId = speakerId,
        lastError = lastError
    )

    private fun parseExtraUrls(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun fromDomain(model: TtsModel) = TtsModelEntity(
            id = model.id,
            name = model.name,
            language = model.language,
            description = model.description,
            type = model.type.name,
            modelUrl = model.modelUrl,
            tokensUrl = model.tokensUrl,
            extraUrls = formatExtraUrls(model.extraUrls),
            dataDir = model.dataDir,
            sizeBytes = model.sizeBytes,
            isDownloaded = model.isDownloaded,
            progress = model.progress,
            speakerId = model.speakerId,
            lastError = model.lastError
        )

        private fun formatExtraUrls(urls: Map<String, String>): String {
            if (urls.isEmpty()) return ""
            return Json.encodeToString(urls)
        }
    }
}
