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
package com.mienaiknife.narra.data.local.backup

import com.mienaiknife.narra.data.local.entities.ArticleEntity
import com.mienaiknife.narra.data.local.entities.FeedEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portable, device-independent backup of user content. Deliberately excludes the encrypted
 * database file and downloaded model state (models can be re-downloaded).
 */
@Serializable
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val feeds: List<FeedEntity>,
    val articles: List<ArticleEntity>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        fun encode(payload: BackupPayload): String = json.encodeToString(payload)

        fun decode(text: String): BackupPayload = json.decodeFromString(text)
    }
}

const val STAGED_BACKUP_FILE = "narra_backup_staged"
