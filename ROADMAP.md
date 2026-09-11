# Roadmap

This is a living document, not a commitment. Priorities may change based on user feedback and
contributor availability. It is authoritative for what is in and out of scope; see
[AGENTS.md](AGENTS.md) for how AI agents and contributors should operate within that scope.

## MVP (current target)

- RSS feed subscription and article fetching
- EPUB import and parsing
- Web page import with reader-mode extraction
- Podcast-style listening queue/playlist UI
- Playback via Android's built-in TTS engine
- On-device AI TTS using Sherpa-ONNX (models downloaded at runtime, not bundled)
- OPML import/export
- File-based backup and restore (no account required)
- Foreground media service with system media controls
- Glance-based home screen widget

## Planned

- PDF file importing and parsing
- Self-hosted AI TTS server support (for example, Kokoro, Coqui, or Piper via a local API)
- Additional cloud AI TTS providers
- Builds for other platforms (desktop and iOS, via Kotlin Multiplatform or a separate app)
- Optional sync via a self-hosted, user-controlled server (for example, a
  Nextcloud/gpodder-compatible API) — no first-party accounts
- Automatic readability/reader-mode heuristic improvements
- Importing articles by scanning photos
- User-customizable color themes
- Support for more languages

## Non-goals

- Requiring a first-party account or any server operated by this project
- Bundling large AI TTS model files in the APK
- Proprietary or license-incompatible dependencies
