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

package com.mienaiknife.narra.domain.models

data class TtsModel(
    val id: String,
    val name: String,
    val language: String,
    val description: String,
    val type: TtsModelType,
    val modelUrl: String,
    val tokensUrl: String = "",
    val extraUrls: Map<String, String> = emptyMap(),
    val dataDir: String? = null,
    val sizeBytes: Long = 0,
    val isDownloaded: Boolean = false,
    val progress: Float = 0f,
    val speakerId: Int? = null,
    val lastError: String? = null
)

enum class TtsModelType {
    VITS,
    MATCHA,
    KOKORO,
    ZIPVOICE,
    KITTEN,
    POCKET,
    SUPERTONIC
}
