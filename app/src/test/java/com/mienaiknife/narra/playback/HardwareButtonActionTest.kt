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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HardwareButtonActionTest {
    @Test
    fun `fromKey round-trips every action`() {
        HardwareButtonAction.entries.forEach { action ->
            assertEquals(action, HardwareButtonAction.fromKey(action.key))
        }
    }

    @Test
    fun `fromKey returns null for unknown or null keys`() {
        assertNull(HardwareButtonAction.fromKey("previous_article"))
        assertNull(HardwareButtonAction.fromKey(null))
    }

    @Test
    fun `fast forward options never offer rewind`() {
        assertEquals(
            listOf(
                HardwareButtonAction.FAST_FORWARD,
                HardwareButtonAction.SKIP_ARTICLE,
                HardwareButtonAction.RESTART_ARTICLE,
            ),
            HardwareButtonAction.fastForwardOptions,
        )
    }

    @Test
    fun `rewind options never offer fast forward`() {
        assertEquals(
            listOf(
                HardwareButtonAction.REWIND,
                HardwareButtonAction.SKIP_ARTICLE,
                HardwareButtonAction.RESTART_ARTICLE,
            ),
            HardwareButtonAction.rewindOptions,
        )
    }
}
