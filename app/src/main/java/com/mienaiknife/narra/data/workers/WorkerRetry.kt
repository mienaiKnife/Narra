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
package com.mienaiknife.narra.data.workers

import com.mienaiknife.narra.domain.NarraError
import java.io.IOException

internal const val MAX_RETRY_ATTEMPTS = 3

/**
 * Whether a failure is worth retrying. Permanent errors (not found, parsing, invalid feeds)
 * should fail fast instead of looping until the battery dies.
 */
internal fun Throwable?.isRetryable(): Boolean = when (this) {
    is NarraError.Network -> true
    is NarraError.Model.DownloadFailed -> true
    is NarraError.Unknown -> true
    is IOException -> true
    else -> false
}
