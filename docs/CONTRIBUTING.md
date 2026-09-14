# Contributing to Narra

Thanks for your interest in improving Narra! This guide covers the basics. For a deeper look at the
project, see [AGENTS.md](../AGENTS.md) and the documents linked from the [README](../README.md).

## Getting Started

1. Fork and clone the repository.
2. Open the project in Android Studio (Ladybug or newer) with JDK 21.
3. Let Gradle sync, then build with `./gradlew assembleDebug` or run the `app` configuration.

See the [README](../README.md) for Docker-based builds if you'd rather not install Android Studio.

## Development Workflow

1. Create a topic branch off `main`.
2. Make your change in small, focused commits.
3. Add or update tests where it makes sense.
4. Run the checks below locally.
5. Open a pull request using the provided template.

## Coding Standards

- Follow the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
  and Android best practices.
- Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) with ktlint:
  `./gradlew spotlessCheck` (or `spotlessApply` to fix).
- Use coroutines and `Flow` for async work; avoid callbacks.
- Keep ViewModels free of Android framework dependencies where possible.
- Prefer `sealed interface`/`sealed class` for UI state.
- One class per file; the file name matches the class name.
- Do not add comments for self-explanatory code.
- All new source files must carry the Apache 2.0 header (run `./gradlew spotlessApply` to add it).

### Architecture

Follow the Clean Architecture layering described in
[docs/ARCHITECTURE.md](ARCHITECTURE.md): **UI → ViewModel → Repository → Data sources**. TTS engines
must be abstracted behind the `TtsEngine` interface. Don't bundle on-device TTS model files in the
APK.

### Database changes

When changing a Room entity, you **must** bump the version, add a `Migration`, update indices, and
add a migration test. See
[docs/ARCHITECTURE.md#database-migrations](ARCHITECTURE.md#database-migrations) for the full rules.

## Testing

Run the unit tests with:

```bash
./gradlew test
```

Screenshot tests use [Paparazzi](https://github.com/cashapp/paparazzi); verify with
`./gradlew verifyPaparazziDebug` (or record with `recordPaparazziDebug`). See the
[Testing Guide](TESTING_GUIDE.md) for the full strategy and CI checks.

## Commits and Pull Requests

- Write clear, imperative commit messages (for example, `fix(tts): reset state on release`).
- Keep pull requests scoped to a single concern.
- Fill in the pull request template, including how you tested the change.
- Link related issues with `Fixes #123` where applicable.

## Dependency Updates

Dependabot opens grouped pull requests weekly for the Gradle version catalog and GitHub Actions.
Do not merge a dependency pull request until CI has passed against the current `main`; the merge
queue re-tests each queued pull request against the latest `main` before it lands.

Several Android toolchain pieces are compatibility-coupled and should be updated as a set:

| Component | Coupled to | Failure mode if mismatched |
| --- | --- | --- |
| Kotlin | KSP, Hilt (Dagger), Compose compiler | Kotlin metadata `2.4.0` is unreadable by older `kotlin-metadata-jvm`, so Hilt/KSP fail |
| androidx libraries | `compileSdk` | Newer AARs require a higher `compileSdk` (for example API 37) |
| AGP / Android Lint | Kotlin, Hilt, KSP | Lint's bundled Kotlin can conflict with the project's Kotlin version |
| Compose BOM | Compose compiler, `compileSdk` | Compiler/runtime mismatch or new lint checks |

Rules of thumb:

- Merge one Dependabot group at a time and wait for a green run on `main` before merging the next.
- Run `./scripts/local-ci.sh` before merging anything non-trivial.
- Toolchain major versions (Kotlin, AGP, Hilt, KSP) are ignored by Dependabot and must be handled
  manually as a coordinated change.

## Licensing

Narra is licensed under the [Apache License 2.0](../LICENSE). By contributing, you agree that your
contributions are licensed under the same terms. New dependencies must be license-compatible
(Apache 2.0, MIT, LGPL, and similar) — flag anything copyleft before adding it. Sherpa-ONNX is
Apache 2.0; verify that any Sherpa-ONNX models used are redistributable or clearly documented as
user-sourced.

## Secrets and Accounts

- Never commit API keys or secrets. Use `local.properties` or environment variables.
- Keep the project buildable from a clean checkout with no manual setup beyond providing API keys
  and downloading TTS models at runtime.
- This project will never require a first-party user account. Do not design features that depend on
  users registering with or authenticating against a server operated by this project.

## Reporting Security Issues

Please do not open a public issue for security vulnerabilities. See [SECURITY.md](../SECURITY.md).
