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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WorkerRetryTest {
    @Test
    fun `transient failures are retryable`() {
        assertTrue(NarraError.Network.NoConnection().isRetryable())
        assertTrue(NarraError.Network.WifiRequired().isRetryable())
        assertTrue(NarraError.Model.DownloadFailed("boom").isRetryable())
        assertTrue(NarraError.Unknown(null).isRetryable())
        assertTrue(IOException("network").isRetryable())
    }

    @Test
    fun `permanent failures are not retryable`() {
        assertFalse(NarraError.Model.NotFound().isRetryable())
        assertFalse(NarraError.Content.InvalidFeed().isRetryable())
        assertFalse(NarraError.Content.ParsingFailed().isRetryable())
        assertFalse(NarraError.Storage.AccessDenied("no").isRetryable())
        assertFalse(RuntimeException("bug").isRetryable())
        assertFalse(null.isRetryable())
    }
}
