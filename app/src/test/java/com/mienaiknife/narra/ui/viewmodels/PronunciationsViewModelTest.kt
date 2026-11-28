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
package com.mienaiknife.narra.ui.viewmodels

import com.mienaiknife.narra.domain.TtsEngine
import com.mienaiknife.narra.domain.TtsState
import com.mienaiknife.narra.domain.models.CustomPronunciation
import com.mienaiknife.narra.domain.pronunciation.PronunciationValidator
import com.mienaiknife.narra.domain.repository.CustomPronunciationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PronunciationsViewModelTest {
    private val repository = FakeCustomPronunciationRepository()
    private val ttsEngine: TtsEngine = mock()
    private val engineState = MutableStateFlow<TtsState>(TtsState.Ready)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PronunciationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(ttsEngine.state).thenReturn(engineState)
        viewModel = PronunciationsViewModel(repository, ttsEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save uses upsert for a new word`() = runTest {
        viewModel.save("Narra", "N AA1 R AH0")
        advanceUntilIdle()

        assertEquals(listOf("upsert:narra"), repository.calls)
    }

    @Test
    fun `save replaces the old entry when the word changes`() = runTest {
        val original = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")

        viewModel.save("Nara", "N AA1 R AH0", original)
        advanceUntilIdle()

        assertEquals(listOf("replace:narra->nara"), repository.calls)
    }

    @Test
    fun `save only changes the pronunciation in place`() = runTest {
        val original = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")

        viewModel.save("Narra", "N AA1 R AA0", original)
        advanceUntilIdle()

        assertEquals(listOf("upsert:narra"), repository.calls)
    }

    @Test
    fun `invalid pronunciation is rejected before storage`() = runTest {
        viewModel.save("Narra", "N ZZ1")
        advanceUntilIdle()

        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `delete delegates to the repository`() = runTest {
        val entry = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")

        viewModel.delete(entry)
        advanceUntilIdle()

        assertEquals(listOf("delete:narra"), repository.calls)
    }

    @Test
    fun `preview speaks the entry through the engine`() = runTest {
        val entry = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")

        viewModel.preview(entry)
        advanceUntilIdle()

        verify(ttsEngine).speak("Narra", "pronunciation-preview-Narra")
    }

    @Test
    fun `preview waits for initialization to finish instead of being dropped`() = runTest {
        val entry = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")
        engineState.value = TtsState.Initializing

        viewModel.preview(entry)
        runCurrent()
        verify(ttsEngine, never()).speak(any(), any())

        engineState.value = TtsState.Ready
        advanceUntilIdle()

        verify(ttsEngine).speak("Narra", "pronunciation-preview-Narra")
    }

    @Test
    fun `preview after a save waits for the lexicon reload`() = runTest {
        val entry = CustomPronunciation(word = "Narra", arpabet = "N AA1 R AH0")

        viewModel.save("Narra", "N AA1 R AH0")
        advanceUntilIdle()
        viewModel.preview(entry)
        runCurrent()

        // The reload is debounced, so the engine is still Ready here. The preview must not
        // start until the reload has run, otherwise its audio is discarded.
        engineState.value = TtsState.Initializing
        runCurrent()
        verify(ttsEngine, never()).speak(any(), any())

        engineState.value = TtsState.Ready
        advanceUntilIdle()

        verify(ttsEngine).speak("Narra", "pronunciation-preview-Narra")
    }

    @Test
    fun `ui state shows English entries sorted by their normalized word`() = runTest {
        repository.entries.value = listOf(
            CustomPronunciation(word = "Zeta", arpabet = "Z EY1 T AH0"),
            CustomPronunciation(word = "Nara", arpabet = "N AA1 R AH0", language = "fr"),
            CustomPronunciation(word = "narra", arpabet = "N AA1 R AH0"),
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertEquals(
            listOf("narra", "Zeta"),
            viewModel.uiState.value.entries.map { it.word },
        )

        collector.cancel()
    }

    private class FakeCustomPronunciationRepository : CustomPronunciationRepository {
        val entries = MutableStateFlow<List<CustomPronunciation>>(emptyList())
        val calls = mutableListOf<String>()

        override fun observeAll() = entries

        override suspend fun snapshot(language: String): List<CustomPronunciation> = entries.value

        override suspend fun upsert(entry: CustomPronunciation): Result<Unit> {
            calls += "upsert:${PronunciationValidator.normalizeWord(entry.word)}"
            return Result.success(Unit)
        }

        override suspend fun replace(
            previous: CustomPronunciation,
            replacement: CustomPronunciation,
        ): Result<Unit> {
            calls += "replace:${PronunciationValidator.normalizeWord(previous.word)}" +
                "->${PronunciationValidator.normalizeWord(replacement.word)}"
            return Result.success(Unit)
        }

        override suspend fun delete(entry: CustomPronunciation): Result<Unit> {
            calls += "delete:${PronunciationValidator.normalizeWord(entry.word)}"
            return Result.success(Unit)
        }
    }
}
