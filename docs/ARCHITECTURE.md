# Architecture Overview

Narra follows a Clean Architecture approach with a clear separation of concerns.

## Layers

- **UI Layer**: Jetpack Compose and ViewModels. ViewModels observe state from the domain layer and should be free of Android framework dependencies where possible.
- **Domain Layer**: Contains use cases and core interfaces such as `TtsEngine`, `ContentRepository`, and `ModelRepository`.
- **Data Layer**: Implementation of repositories and data sources (Room, Retrofit, File system).

## Key Components

### TtsEngine
Abstracts different TTS providers. All TTS implementations (Android TTS, Sherpa-ONNX, Cloud, etc.) must implement this interface.

### ContentRepository
Normalizes data from various sources (RSS, EPUB, Web articles) into a common `Article` / `Chapter` model.

### ModelRepository
Handles the downloading, storage, and selection of on-device AI TTS models (e.g., Sherpa-ONNX).

## Project Structure

```
app/
  src/main/
    java/com/<package>/
      data/          # Repositories, data sources, models
      domain/        # Use cases, interfaces (TtsEngine, ContentRepository, ModelRepository)
      tts/           # TTS engine implementations
        android/     # Android built-in TTS
        ondevice/    # On-device AI TTS (Sherpa-ONNX)
        cloud/       # Cloud AI TTS providers
        selfhosted/  # Self-hosted AI TTS servers (planned)
      ui/            # Composables, ViewModels, navigation
      service/       # PlaybackService and media session
```

## Technologies
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Async**: Coroutines & Flow
- **Audio**: Media3 / ExoPlayer
- **Dependency Injection**: Hilt
- **Build System**: Gradle with Kotlin DSL
