# AGENTS.md

This file is the operating manual for AI agents working on Narra. It keeps the
must-not-break rules inline and links to the authoritative documents for detail. For a
human-facing overview, start with [README.md](README.md) and [CONTRIBUTING.md](docs/CONTRIBUTING.md).

## Project Overview
Narra is an open source Android app that converts text from RSS feeds, imported EPUB files, and
saved web articles into audio using text-to-speech (TTS), delivered in a podcast-like listening
experience. The MVP targets native Android, with planned expansion to other platforms and features
over time. See [README.md](README.md) for the full description.

## Scope
[ROADMAP.md](ROADMAP.md) is authoritative for what is in and out of scope. Do not implement
**Planned** or **Non-goals** items unless the user explicitly asks.

## Critical Invariants
These rules are load-bearing. Keep them in mind on every task, and read the linked document before
changing the relevant area.

- **Clean Architecture layering**: UI → ViewModel → Repository → Data sources. ViewModels stay free
  of Android framework dependencies. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- **TTS engines are interchangeable** behind the `TtsEngine` interface. New providers (cloud or
  self-hosted) are separate modules configured by base URL + API key. See
  [docs/TTS_ENGINES.md](docs/TTS_ENGINES.md).
- **Database schema changes are mandatory migrations**: bump the version in `AppDatabase.kt`, add a
  `Migration` in `DatabaseModule.kt`, update indices, and add a migration test. Missing migrations
  crash existing installs. See
  [docs/ARCHITECTURE.md#database-migrations](docs/ARCHITECTURE.md#database-migrations) and
  [docs/CONTRIBUTING.md#database-changes](docs/CONTRIBUTING.md#database-changes).
- **Database encryption**: the Room database is SQLCipher-encrypted; the key lives in the Android
  Keystore. If it cannot be opened with the current passphrase, `DatabaseModule` deletes and
  recreates it. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- **Playback state contract**: `TtsPlayer` MUST report `STATE_IDLE` whenever `getState()` exposes a
  `PlaybackException`. Samsung devices require the `MediaButtonReceiver`/session-extras workarounds,
  and widgets must use direct `Intent` signals (e.g. `ACTION_TOGGLE`) to `PlaybackService`, never
  `MediaController`. See [docs/PLAYBACK_LIFECYCLE.md](docs/PLAYBACK_LIFECYCLE.md) and
  [docs/samsung-media-session-fixes.md](docs/samsung-media-session-fixes.md).
- **Content normalization**: RSS, EPUB, and web content flow through the specialized repositories
  (`ArticleRepository`, `FeedRepository`, `ImportExportRepository`) into the common `Article` model;
  always persist the source URL for saved web articles. See
  [docs/CONTENT_PARSING.md](docs/CONTENT_PARSING.md).
- **On-device models are downloaded, never bundled**: Sherpa-ONNX models are large and fetched at
  runtime. `ModelRepository` owns download/storage/selection and is separate from `TtsEngine`. See
  [docs/TTS_ENGINES.md](docs/TTS_ENGINES.md).
- **Sherpa-ONNX word highlighting**: native timestamps exist in the C++ core (v1.13.4+) but are not
  exposed in the Java/JNI bindings; `SherpaTtsEngine` uses a heuristic. Do not refactor to native
  timestamps until `GeneratedAudio.getTimestamps()` is exposed. See
  [docs/TTS_ENGINES.md](docs/TTS_ENGINES.md).
- **Licensing & secrets**: all dependencies must be Apache 2.0/MIT/LGPL-compatible (flag copyleft).
  Never commit API keys or secrets. This project will never require a first-party account or a
  project-operated server. See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md),
  [SECURITY.md](SECURITY.md), and [ROADMAP.md#non-goals](ROADMAP.md#non-goals).
- **CI must pass**: every change must pass the checks in `.github/workflows/ci.yml`. A failing style
  check blocks the whole pipeline, so run the checks locally (below) before declaring work complete.
- **Dependency toolchain versions move together**: Kotlin, KSP, Hilt (Dagger), AGP, Android Lint,
  `compileSdk`, and the Compose BOM are compatibility-coupled. A newer Kotlin emits metadata that
  older Hilt/KSP cannot read, and newer androidx libraries can require a higher `compileSdk`. Update
  the set together and never merge a toolchain Dependabot group until CI passes against current
  `main`. See [docs/CONTRIBUTING.md#dependency-updates](docs/CONTRIBUTING.md#dependency-updates).

## Writing Warning-Free Code
Android Studio warnings are defects: fix them instead of shipping them. CI only fails on lint
**errors** (`app/build.gradle.kts` sets `lint.abortOnError = true`; `warningsAsErrors` is off), so a
warning that is not checked here will slip through. Before writing or reviewing code, avoid these
common patterns (each links to the class of Android Lint / Kotlin compiler warning it triggers):

- **Unused declarations**: delete unused imports, parameters, local variables, and resources.
  `UnusedResources` covers `strings.xml`/drawables too.
- **Redundant API-level guards**: do not check `Build.VERSION.SDK_INT` for levels below `minSdk`
  (24) — the check can never be false (`ObsoleteSdkInt`).
- **Wake locks**: pass a timeout to `PowerManager.WakeLock.acquire(...)` (`WakelockTimeout`).
- **Hardcoded user-facing values**: use `stringResource`/`getString` (and `<plurals>` for counts),
  `dimens.xml`, and theme/color resources — never inline literals in layouts or composables
  (`HardcodedText`, `PluralsCandidate`).
- **Kotlin compiler warnings**: avoid unnecessary non-null assertions (`!!`), unchecked casts,
  deprecated APIs, redundant qualifiers / `else` branches, non-exhaustive `when`, and unused
  `suspend`/lambda parameters. Prefer safe calls, `requireNotNull` with a message, and exhaustive
  `sealed` handling.
- **Coroutines and Context**: no `GlobalScope`; scope work to the lifecycle/ViewModel; never retain
  an `Activity`/`Context` in a longer-lived object.
- **Localization**: keep every user-visible string in resources; mark intentionally-untranslated
  strings with `translatable="false"` rather than leaving lint to guess.

Do **not** silence a warning with `@Suppress`/`//noinspection` unless the suppression is
unavoidable and the reason is documented in a comment on the same declaration.

## Definition of Done
Before you report a task as finished, run these locally and fix every failure:

```bash
./scripts/local-ci.sh  # Or ./scripts/local-ci.sh --low-mem if needed
```

For manual control:
```bash
./gradlew spotlessApply        # auto-fix ktlint formatting and add Apache 2.0 headers
./gradlew spotlessCheck lintDebug
./gradlew testDebugUnitTest -PskipPaparazzi # Skips memory-heavy screenshot tests
```

`spotlessApply` is the fast path to avoiding style failures; `spotlessCheck` is what CI runs. Because
`lintDebug` only fails on errors, also open `app/build/reports/lint-results-debug.txt` and confirm
your change adds **zero new warnings**; fix any warning in code you touched (see
[Writing Warning-Free Code](#writing-warning-free-code)). For schema, Compose, or UI changes also run
`./gradlew verifyPaparazziDebug` and, where an emulator is available,
`./gradlew connectedDebugAndroidTest`. `.github/workflows/ci.yml` gates merges on
`spotlessCheck`, `lintDebug`, `testDebugUnitTest`, `testReleaseUnitTest`, `verifyPaparazziDebug`,
`jacocoTestReport`, `connectedDebugAndroidTest`, and the release builds. Do not consider a task done
while any of them fail. See [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md#cicd).

## What to Ask Before Doing
- If a task would require adding a new third-party dependency, confirm before adding it.
- If a feature touches the `TtsEngine`, `ArticleRepository`, `FeedRepository`,
  `ImportExportRepository`, or `ModelRepository` contracts, flag it — these are load-bearing
  abstractions.
- If something is ambiguous between MVP scope and planned features, ask rather than assume.

## Keeping Docs in Sync
When you change an invariant, update the linked document in the same change so this file and the
`docs/` tree do not drift apart.

This project wouldn't have been possible without help from agents like you. Thank you!
