# AGENTS.md

## Project Overview
This is an open source Android app that converts text from RSS feeds, imported EPUB files, and saved web articles into audio using text-to-speech (TTS), delivered in a podcast-like listening experience. The MVP targets native Android, with planned expansion to other platforms and additional features over time.

## License
This project is licensed under the Apache License 2.0. All contributions must be compatible with this license. Add the standard Apache 2.0 header to all new source files.

## MVP Scope
The following defines the current build target. Do not implement beyond this unless explicitly asked:
- RSS feed subscription and article fetching
- EPUB file importing and parsing
- Web page importing with reader-mode extraction for clean article text
- A listening queue/playlist UI similar to a podcast app
- TTS playback using Android's built-in TTS engine (android.speech.tts)
- On-device AI TTS using Sherpa-ONNX (models downloaded at runtime, not bundled)
- OPML export and import for feed list portability
- File-based backup and restore (no account required)
- Foreground service for background audio playback with media session controls

## Planned Features (not yet in scope)
Be aware these are coming so that current architectural decisions don't block them:
- PDF file importing and parsing
- Self-hosted AI TTS server support (e.g. Kokoro, Coqui, Piper via local API)
- Additional cloud AI TTS providers
- Builds for other platforms (e.g. desktop and iOS via Kotlin Multiplatform, or a separate app)
- OPML export and import for feed list portability
- File-based backup and restore (no account required)
- Optional sync via self-hosted compatible server (e.g. Nextcloud/gpodder-compatible API),
  authenticated by server URL and credentials the user controls — no first-party accounts
- Automatic readability/reader-mode heuristic improvements over time
- Importing texts by scanning photos
- User-customizable color themes
- Customizable font in the reader screen

## Tech Stack
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM with a clean architecture layer separation (UI → ViewModel → Repository → Data sources)
- Audio playback: Media3 / ExoPlayer
- RSS parsing: Rome or a lightweight alternative
- EPUB parsing: Epublib or equivalent JVM-compatible library
- Web page parsing: Mozilla Readability port (or equivalent) for reader-mode extraction
- On-device AI TTS: Sherpa-ONNX (Apache 2.0)
- Dependency injection: Hilt
- Build system: Gradle with Kotlin DSL

## Code Style & Conventions
- Follow the official Kotlin coding conventions
- Use coroutines and Flow for async work; avoid callbacks
- Keep ViewModels free of Android framework dependencies where possible
- One class per file; file name matches class name
- Prefer `sealed class` for UI state modeling
- Write self-documenting code; only add comments for non-obvious logic

## Architecture Notes
- TTS engines must be abstracted behind a common `TtsEngine` interface so Android TTS,
  on-device AI TTS, cloud providers, and future self-hosted servers are all interchangeable
- AI TTS providers (cloud or self-hosted) should be implemented as separate modules
  conforming to the same interface, each configurable via a base URL + API key so
  self-hosted servers can slot in without code changes
- On-device TTS (Sherpa-ONNX) model files must be downloaded and stored at runtime;
  model download, storage, and selection are handled by a dedicated `ModelRepository`
  that is separate from the `TtsEngine` interface itself
- Do not bundle Sherpa-ONNX model files in the APK; they are too large and must be
  fetched on demand
- RSS articles, EPUB content, and saved web articles all flow through a shared
  `ContentRepository` that normalizes them into a common `Article` / `Chapter` model
  before handing off to TTS; content source type is tracked on the model but is
  otherwise transparent to the rest of the app
- Saved web articles are stored persistently like RSS articles; always persist the
  source URL so the content can be refreshed if the page changes
- Playback state should be managed in a single `PlaybackService` (foreground service);
  ViewModels observe it, never control it directly

## Project Structure (target layout)

```
app/
  src/main/
    java/com/<yourpackage>/
      data/          # Repositories, data sources, models
      domain/        # Use cases, interfaces (TtsEngine, ContentRepository, ModelRepository)
      tts/           # TTS engine implementations
        android/     # Android built-in TTS
        ondevice/    # On-device AI TTS (Sherpa-ONNX)
        cloud/       # Cloud AI TTS providers
        selfhosted/  # Self-hosted AI TTS servers (planned)
      ui/            # Composables, ViewModels, navigation
      service/       # PlaybackService and media session
    res/
```

## What to Ask Before Doing
- If a task would require adding a new third-party dependency, confirm before adding it
- If a feature touches the `TtsEngine` interface, `ContentRepository`, or `ModelRepository`
  contracts, flag it — these are load-bearing abstractions
- If something is ambiguous between MVP scope and planned features, ask rather than assume

## Open Source Considerations
- This project is Apache 2.0 licensed; all dependencies must be compatible (Apache 2.0,
  MIT, LGPL, or similar) — flag any GPL dependencies before adding them
- Sherpa-ONNX is Apache 2.0 licensed; verify that any Sherpa-ONNX models used are
  redistributable or clearly documented as user-sourced
- Do not include API keys or secrets in source files; use a `local.properties` or
  environment variable pattern
- Keep the project buildable from a clean checkout with no manual setup steps beyond
  providing API keys and downloading TTS models at runtime
- This app will never require a first-party user account; do not design any feature
  that depends on users registering with or authenticating against a server operated
  by this project
