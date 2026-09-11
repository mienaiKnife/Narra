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
package com.mienaiknife.narra.playback

/**
 * The action performed when the fast-forward/rewind hardware media buttons are pressed.
 * The [key] values are persisted in settings, so they must remain stable.
 */
enum class HardwareButtonAction(val key: String) {
    FAST_FORWARD("fast_forward"),
    SKIP_ARTICLE("skip_article"),
    REWIND("rewind"),
    RESTART_ARTICLE("restart_article"),
    ;

    companion object {
        /** Options offered for the fast-forward hardware button. */
        val fastForwardOptions: List<HardwareButtonAction> = listOf(FAST_FORWARD, SKIP_ARTICLE, RESTART_ARTICLE)

        /** Options offered for the rewind hardware button. */
        val rewindOptions: List<HardwareButtonAction> = listOf(REWIND, SKIP_ARTICLE, RESTART_ARTICLE)

        fun fromKey(key: String?): HardwareButtonAction? = entries.firstOrNull { it.key == key }
    }
}
