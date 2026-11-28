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
import com.mienaiknife.narra.domain.models.VoiceGender
import com.mienaiknife.narra.domain.models.VoiceProfile
import com.mienaiknife.narra.domain.models.VoiceQuality
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "tts_models")
data class TtsModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
    val type: String, // Store as String for Room compatibility
    val modelUrl: String,
    val tokensUrl: String,
    val extraUrls: String = "", // JSON map of extra file URLs
    val dataDir: String?,
    val sizeBytes: Long,
    val isDownloaded: Boolean,
    val progress: Float,
    val speakerId: Int? = null,
    val lastError: String? = null,
    val voiceQuality: String = VoiceQuality.UNKNOWN.name,
    val voiceGender: String = VoiceGender.UNSPECIFIED.name,
    val voiceLocale: String = "",
    val voiceSpeakerCount: Int = 1,
) {
    fun toDomain() = TtsModel(
        id = id,
        name = name,
        language = language,
        voice = VoiceProfile(
            quality = parseEnum(voiceQuality, VoiceQuality.UNKNOWN),
            gender = parseEnum(voiceGender, VoiceGender.UNSPECIFIED),
            locale = voiceLocale,
            speakerCount = voiceSpeakerCount.coerceAtLeast(1),
        ),
        type = parseType(type),
        modelUrl = modelUrl,
        tokensUrl = tokensUrl,
        extraUrls = parseExtraUrls(extraUrls),
        dataDir = dataDir,
        sizeBytes = sizeBytes,
        isDownloaded = isDownloaded,
        progress = progress,
        speakerId = speakerId,
        lastError = lastError,
    )

    // Unknown/legacy values must not throw: a single bad row would otherwise break the whole
    // models Flow. Fall back to VITS so the app stays usable.
    private fun parseType(raw: String): TtsModelType = parseEnum(raw, TtsModelType.VITS)

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
            type = model.type.name,
            modelUrl = model.modelUrl,
            tokensUrl = model.tokensUrl,
            extraUrls = formatExtraUrls(model.extraUrls),
            dataDir = model.dataDir,
            sizeBytes = model.sizeBytes,
            isDownloaded = model.isDownloaded,
            progress = model.progress,
            speakerId = model.speakerId,
            lastError = model.lastError,
            voiceQuality = model.voice.quality.name,
            voiceGender = model.voice.gender.name,
            voiceLocale = model.voice.locale,
            voiceSpeakerCount = model.voice.speakerCount,
        )

        private fun formatExtraUrls(urls: Map<String, String>): String {
            if (urls.isEmpty()) return ""
            return Json.encodeToString(urls)
        }
    }
}

/**
 * Copies the catalog metadata onto an existing row while keeping everything that belongs to the
 * installation: download state, progress, error, and the user's selected speaker.
 */
internal fun TtsModelEntity.withPreservedState(existing: TtsModelEntity): TtsModelEntity = copy(
    isDownloaded = existing.isDownloaded,
    progress = existing.progress,
    dataDir = existing.dataDir,
    lastError = existing.lastError,
    speakerId = existing.speakerId,
)

private inline fun <reified T : Enum<T>> parseEnum(raw: String, fallback: T): T = runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
