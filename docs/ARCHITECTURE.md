# Architecture Overview

Narra follows a Clean Architecture approach with a clear separation of concerns: **UI → ViewModel → Repository → Data sources**.

## Layers

- **UI Layer**: Built with **Jetpack Compose**. ViewModels observe state from the domain layer and are kept free of Android framework dependencies to ensure testability. Includes home screen widgets built with **Jetpack Glance**.
- **Domain Layer**: The "brain" of the app. Contains use cases, domain models (`TtsModel`), and core interfaces (`TtsEngine`, `ContentRepository`).
- **Data Layer**: Implementation of repositories. Handles data orchestration between local storage and remote APIs. Contains the `Article` model and Room entities. Data is encrypted using **SQLCipher**.

## Key Components

### `TtsEngine`
Abstracts the underlying speech synthesis. Implementations like `AndroidTtsEngine` (system TTS) and `SherpaTtsEngine` (on-device AI) are interchangeable.

### `ContentRepository`
The central hub for data. It handles fetching from RSS (via **RSSParser**), parsing EPUBs (via **Epublib**), and extracting Web content (via **Readability4J**), normalizing everything into the `Article` model. It also manages database operations, feed subscriptions, and backup/restore functionality.

### `ImageDataSource`
Handles the downloading and local persistence of images for articles and feeds, ensuring they are available for offline listening.

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
      data/          # Room entities, workers, models
        local/       # Local database, DAOs, and data sources (Epub, Opml, Image)
        remote/      # Remote data sources (Web, Feed)
        repositories/# Repository implementations
        settings/    # Settings/DataStore managers (Sync, Download)
      domain/        # Models (Article), repository interfaces, use cases
      tts/           # TTS engine implementations
        android/     # Android built-in TTS
        ondevice/    # On-device AI TTS (Sherpa-ONNX)
        common/      # Delegating engine and shared logic
      ui/            # Composables, ViewModels, Theme, Models
        widget/      # Glance-based home screen widgets
      utils/         # Core utilities (Security, Notifications)
      service/       # PlaybackService (Media3), SyncManager
      playback/      # TtsPlayer and PlaybackManager
      di/            # Hilt dependency injection modules
```
targetContent:

## Technologies
- **Language**: Kotlin
- **UI**: Jetpack Compose & Jetpack Glance (Widgets)
- **Async**: Coroutines & Flow
- **Audio**: Media3 / ExoPlayer
- **Image Loading**: Coil
- **Dependency Injection**: Hilt
- **Persistence**: Room & SQLCipher (Encryption)
- **Background**: WorkManager
- **Parsing**: RSSParser, Readability4J, Epublib
