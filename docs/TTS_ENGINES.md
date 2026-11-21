# TTS Engine Implementation Guide

Narra is designed to be engine-agnostic, and our goal is to support a wide variety of TTS engines. All speech synthesis is handled through the `TtsEngine` interface.

## The `TtsEngine` Interface

Every TTS implementation must reside in its own package under `com.<package>.tts` and implement the `TtsEngine` interface defined in the `domain` layer.

### Core Responsibilities
1. **Initialization**: Handle setup (e.g., connecting to the Android TTS service or loading ONNX models).
2. **Synthesis**: Convert text to audio.
3. **Voice Management**: Provide a list of available voices and handle selection.
4. **Lifecycle**: Properly release resources when the engine is no longer needed.

## Current Implementations

- **Android TTS** (`/tts/android`): Wraps `android.speech.tts.TextToSpeech`. Best for low latency and zero-download availability.
- **On-Device AI** (`/tts/ondevice`): Uses Sherpa-ONNX for high-quality offline synthesis. Supports various model types including VITS, Matcha, Kokoro, and more. Model files are large and **must never be bundled in the APK**; `ModelRepository` (separate from the `TtsEngine` interface) handles downloading, storing, and selecting them at runtime.
    - **Version and coordinate**: `sherpaOnnx = "v2.0.2"` in `gradle/libs.versions.toml`, resolved from JitPack as the Android AAR module `com.github.k2-fsa.sherpa-onnx:sherpa-onnx`. Do **not** use the aggregator `com.github.k2-fsa:sherpa-onnx` for 2.x: its POM pulls in the desktop JVM API (duplicate `com.k2fsa.sherpa.onnx` classes that D8 rejects) plus six desktop native bundles (~60 MB) that would be packaged into the APK. The AAR POM declares no dependencies and ships only the four Android ABIs.
    - **Update Check**: To check if a newer version of Sherpa-ONNX is available on GitHub, run: `./gradlew :app:checkSherpaUpdate`.
    - **Updating**: If an update is available, bump the `sherpaOnnx` version in `gradle/libs.versions.toml`. JitPack publishes the release tag with its `v` prefix (`v2.0.2`; plain `2.0.2` 404s), and `checkSherpaUpdate` normalizes that before comparing. No local `.aar` is vendored.
    - **Lexicons (2.x)**: 2.x removed the espeak-ng frontend, so `dataDir` (the `espeak-ng-data` directory inside Piper/Matcha archives) is deprecated and ignored. VITS, Matcha, Kokoro, Kitten, and ZipVoice models instead need a lexicon (word → phonemes) or pre-phonemized input; without one the native layer logs `phoneme_codepoints is empty, no lexicon, and no frontend available` and plays silence.
        - `ModelLexicons` maps a model's `language` to a lexicon file and `ModelRepository.ensureLexiconAvailable(language)` downloads it once into `files/tts_models/lexicons/`, returning its absolute path. Every model of that language shares the file (English uses `lexicon-en-us.txt`, ~6 MB, from the `tts-models` GitHub release). `ModelRepositoryImpl.downloadModel` fetches it as part of a model download, and `SherpaTtsEngine` asks for it again during initialization so models downloaded before the migration repair themselves.
        - Only languages listed in `ModelLexicons` are supported. For any other language the engine reports `TtsState.Error` rather than synthesizing silence. Add a language only when a lexicon can actually be sourced.
        - Coverage is word-based: proper nouns and numbers that are not in the dictionary are reported by the native layer as `OOV word skipped` and omitted, while the rest of the sentence still synthesizes. Punctuation attached to words (em dashes, ellipses, curly quotes) is stripped before lookup, so no text preprocessing is needed; a sentence made only of OOV tokens produces a benign `Failed to convert … to token IDs` line but does not affect other sentences.
        - Licensing: `lexicon-en-us.txt` is generated from CMUDict (BSD-2-Clause) plus the espeak-ng `en_list` (espeak-ng is GPL-3.0), and the release asset itself carries no license header. It is downloaded at runtime and never bundled or committed, so it is not redistributed in the APK, but treat it as copyleft-adjacent when adding other languages.
    - **Word Highlighting**: Uses a heuristic-based estimation for word boundaries (see `SherpaTtsEngine.estimateWordBoundaries`). The 2.x lexicon path does not expose word timestamps, so the heuristic is still used. Native word timestamps exist in the C++ core since v1.13.4 but are still **not** exposed in the Kotlin/JVM bindings as of v2.0.2 (`GeneratedAudio` carries only `samples` and `sampleRate`). Re-check `sherpa-onnx/kotlin-api/Tts.kt` before replacing the heuristic.
        - The intended replacement is **not** a `GeneratedAudio` field: 2.x ships `ForcedAligner` (`AlignmentInterval`/`AlignmentTier`/`ForcedAlignmentResult` in the Android AAR), which aligns the synthesized audio against the transcript and returns phone and word intervals. It is a classic Kaldi HMM/GMM aligner — the model package needs `final.alimdl` (TransitionModel + AmDiagGmm), `tree`, `lda.mat`, a lexicon and phone table. Ready-made packages live at `https://github.com/k2-fsa/sherpa-onnx/releases/tag/fa-models` (English MFA ≈ 45 MB compressed, Chinese pinyin ≈ 31 MB), so it is another `ModelRepository`-managed download, and alignment cost per utterance has to be budgeted in the synthesis path.
    - **Text normalization**: `SpeechTextNormalizer` spells out digits before synthesis because the 2.x lexicons contain no numbers, and anything out of vocabulary is silently skipped by the native library. It handles integers (with separators), decimals, percentages, currency, ordinals, numeric ranges (`1990-2000` → "… to …"), `CVE-2026`-style prefixes, `&`, em/en dashes (turned into a comma so the pause survives) and letter/digit identifiers (`v0.1` → "v zero point one"). Each rewrite keeps a character mapping back to the displayed paragraph: `TtsState.Speaking.start/end` are offsets into the reader's text, not into the synthesized text. Add rules together with `SpeechTextNormalizerTest`, and keep the mapping invariant (mapped ranges must stay inside the original string).
- **Delegating Engine** (`/tts/common`): A wrapper that delegates to the currently selected engine, allowing for seamless switching at runtime.
- **Cloud Providers** (`/tts/cloud`): (Planned) Will handle network-based synthesis (Google Cloud, OpenAI, etc.).

## Adding a New Engine

To add a new provider (e.g., a self-hosted Piper API), follow these steps:

### 1. Define the Implementation
Create a new class implementing `TtsEngine`.

```kotlin
class MyNewTtsEngine @Inject constructor(
    private val context: Context
) : TtsEngine {
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    override fun speak(text: String, utteranceId: String) {
        // Stop current and speak new text
    }

    override fun enqueue(text: String, utteranceId: String) {
        // Add to queue
    }

    override fun stop() {
        // Stop playback and clear queue
    }

    override fun setPlaybackSpeed(speed: Float) {
        // Adjust speed
    }

    override fun setAudioAttributes(usage: Int, contentType: Int) {
        // Store audio attributes for the underlying player
    }

    override fun setVolume(volume: Float) {
        // Adjust output volume (0.0f to 1.0f)
    }

    override fun release() {
        // Cleanup
    }
}
```

### 2. Update the Hilt Module
If you want the new engine to be selectable, it must be provided via a Hilt module. Narra binds `DelegatingTtsEngine` as the single `TtsEngine`; it delegates to the engine currently selected by the user, so the new engine is wired up inside the delegating engine (or via a `@Named` binding) rather than bound directly.

### 3. Handle Configuration
If the engine requires an API key or a Base URL (common for self-hosted or cloud), ensure these are fetched from a settings manager (for example, `PlaybackSettingsManager`) or `local.properties` rather than being hardcoded.

## Best Practices
- **Handle Interruption**: Ensure that if `speak` is called while another synthesis is active, the previous one is gracefully cancelled.
- **Error Handling**: Report engine-specific failures through the domain-level `TtsState.Error` (optionally with a `TtsErrorReason`) so the UI can show consistent messages.
- **Don't Block**: Always perform heavy computation or network requests on the appropriate `CoroutineDispatcher` (usually `Dispatchers.IO`).
