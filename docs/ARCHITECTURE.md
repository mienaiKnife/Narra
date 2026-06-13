# Architecture Overview

Narra follows a Clean Architecture approach with a clear separation of concerns: **UI → ViewModel → Repository → Data sources**.

## Layers

- **UI Layer**: Built with **Jetpack Compose**. ViewModels observe state from the domain layer and are kept free of Android framework dependencies to ensure testability.
- **Domain Layer**: The "brain" of the app. Contains use cases, domain models (`TtsModel`), and core interfaces (`TtsEngine`, `ContentRepository`).
- **Data Layer**: Implementation of repositories. Handles data orchestration between local storage and remote APIs. Contains the `Article` model and Room entities.

## Key Components

### `TtsEngine`
Abstracts the underlying speech synthesis. Implementations like `AndroidTtsEngine` (system TTS) and `SherpaTtsEngine` (on-device AI) are interchangeable.

### `ContentRepository`
The central hub for data. It handles fetching from RSS, parsing EPUBs, and extracting Web content, normalizing everything into the `Article` model. It also manages database operations, feed subscriptions, and backup/restore functionality.

### `HtmlParser`
Located in `ui.utils` (within `HtmlToAnnotatedString.kt`), it is responsible for converting raw HTML content from various sources into a list of `ContentBlock`s, which are then used for both UI rendering and TTS synthesis.

### `ModelRepository`
Manages the lifecycle of on-device AI models. It handles downloading from remote sources, local storage management, and versioning.

## Data Persistence

Narra uses **Room** for local persistence, ensuring that all articles and settings are available offline.

- **`ArticleEntity`**: Stores article content, metadata, and playback progress (percentage, paragraph index, word offset).
- **`FeedEntity`**: Stores RSS feed subscriptions and sync settings.
- **`TtsModelEntity`**: Tracks downloaded TTS models and their local file paths.

## Background Work

Narra utilizes **WorkManager** for reliable background operations:
- **`DownloadWorker`**: Manages the multi-part download of large TTS models.
- **`SyncManager`**: Coordinates periodic RSS feed refreshes.
- **`DatabaseExportWorker` / `ImportWorker`**: Handles the file-based backup and restore system.

## Project Structure

```
app/
  src/main/
    java/com/mienaiknife/narra/
      data/          # Repositories, Room DAOs, Entities, Workers, Models
      domain/        # Use cases, Interfaces, Domain Models (TtsModel)
      tts/           # TTS engine implementations
      ui/            # Composables, ViewModels, Theme, Models
      utils/         # Core utilities (Security, Notifications)
      service/       # PlaybackService (Media3), SyncManager
      playback/      # TtsPlayer and PlaybackManager
      di/            # Hilt dependency injection modules
```,targetContent:

## Technologies
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Async**: Coroutines & Flow
- **Audio**: Media3 / ExoPlayer
- **Dependency Injection**: Hilt
- **Persistence**: Room
- **Background**: WorkManager
