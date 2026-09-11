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

import android.content.Context
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TtsPlayerTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var player: TtsPlayer

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        player = TtsPlayer(context, FakeTtsEngine(), PlaybackSettingsManager(context))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setMediaItems with an empty list does not throw`() {
        player.setMediaItems(emptyList())
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    @Test
    fun `speak with no paragraphs is idle rather than stuck buffering`() {
        player.speak(Article(id = "article-1", title = "Empty", source = "Test"), emptyList())
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    private class FakeTtsEngine : TtsEngine {
        private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
        override val state: StateFlow<TtsState> = _state

        override fun speak(
            text: String,
            utteranceId: String,
        ) = Unit

        override fun enqueue(
            text: String,
            utteranceId: String,
        ) = Unit

        override fun stop() = Unit

        override fun setPlaybackSpeed(speed: Float) = Unit

        override fun setAudioAttributes(
            usage: Int,
            contentType: Int,
        ) = Unit

        override fun setVolume(volume: Float) = Unit

        override fun release() = Unit
    }
}
