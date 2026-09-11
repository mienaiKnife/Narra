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
package com.mienaiknife.narra.data

object NetworkConstants {
    /**
     * Desktop browser user agent used when fetching pages that reject non-browser clients.
     */
    const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    const val APP_USER_AGENT = "Narra/1.0"

    const val TIMEOUT_SECONDS = 60L

    const val SHERPA_MODELS_BASE_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
}
