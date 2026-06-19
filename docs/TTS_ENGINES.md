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
- **On-Device AI** (`/tts/ondevice`): Uses Sherpa-ONNX for high-quality offline synthesis. Requires model management via `ModelRepository`.
    - **Update Check**: To check if a newer version of Sherpa-ONNX is available on GitHub, run: `./gradlew :app:checkSherpaUpdate`.
    - **Updating**: If an update is available, download the latest `.aar` and replace `app/libs/sherpa-onnx.aar`, then update the `sherpaOnnx` version in `gradle/libs.versions.toml`.
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

    override fun release() {
        // Cleanup
    }
}
```

### 2. Update the Hilt Module
If you want the new engine to be selectable, it must be provided via a Hilt module. We typically use a `TtsProvider` factory or a specific `Named` binding to switch between engines at runtime.

### 3. Handle Configuration
If the engine requires an API key or a Base URL (common for self-hosted or cloud), ensure these are fetched from a `PreferenceRepository` or `local.properties` rather than being hardcoded.

## Best Practices
- **Handle Interruption**: Ensure that if `synthesize` is called while another synthesis is active, the previous one is gracefully cancelled.
- **Error Handling**: Map engine-specific errors to the domain-level `TtsError` sealed class so the UI can show consistent messages.
- **Don't Block**: Always perform heavy computation or network requests on the appropriate `CoroutineDispatcher` (usually `Dispatchers.IO`).
