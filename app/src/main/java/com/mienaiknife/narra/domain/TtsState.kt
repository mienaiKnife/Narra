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
package com.mienaiknife.narra.domain

sealed class TtsState {
    object Idle : TtsState()

    object Initializing : TtsState()

    object Ready : TtsState()

    data class Speaking(
        val utteranceId: String,
        val start: Int = 0,
        val end: Int = 0,
        val frame: Int = 0,
    ) : TtsState()

    data class Finished(val utteranceId: String) : TtsState()

    data class Error(
        val message: String,
    ) : TtsState()
}
