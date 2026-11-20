# Testing Guide

This guide explains the testing strategy for Narra and how to run and write tests.

## Test Levels

### Unit Tests
- **Location**: `app/src/test/java/`
- **Focus**: Business logic in ViewModels, Use Cases, and Repository logic that doesn't depend on the Android framework.
- **Run command**: `./gradlew test`

### Instrumented Tests
- **Location**: `app/src/androidTest/java/`
- **Focus**: UI components (Compose), Room database migrations, and logic that requires an Android environment.
- **Run command**:
  ```bash
  adb devices -l
  ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
  ```

> **These tests delete app data.** Gradle installs both APKs and uninstalls them when the run
> finishes. That removes the app's databases, settings, and downloaded files even when every test
> passes — including subscriptions, queue items, and playback progress.

- **Always target an emulator.** Pin `ANDROID_SERIAL` to the emulator's serial. With more than one
  device attached, Gradle runs on every one, including a connected phone. Start an emulator with
  `android emulator ...` or `emulator -avd <name>` if none is running.
- **Never point the run at a phone with real data.** If a physical device is unavoidable, export a
  backup first (Settings → Downloads & Data → Export database, or enable auto-export), get the
  user's approval, and treat the device data as disposable. Android Auto Backup is not a reliable
  fallback for debug builds.
- **Scope the run while iterating** with
  `-Pandroid.testInstrumentationRunnerArguments.class=com.mienaiknife.narra.data.local.DatabaseMigrationTest`,
  but remember the data wipe still happens after a scoped run.

## Testing Specialized Components

### TTS Engines
Testing TTS engines can be tricky as they often depend on system services or large model files.
- **Mocking**: Use mocks or fakes for the `TtsEngine` interface when testing ViewModels or the `PlaybackService`.
- **Manual Verification**: Since audio quality is subjective, manual verification on a physical device is often required for new TTS providers.

### Content Parsing
- **Sample Data**: Use the assets folder to store sample EPUBs or HTML files to test parsing logic against known outputs.
- **Edge Cases**: Always test with malformed HTML or empty RSS feeds.

## Best Practices
- **Test-Driven Development**: We encourage writing tests before implementation for bug fixes and new features.
- **Avoid Flakiness**: Ensure tests are deterministic. Use `runTest` with a `StandardTestDispatcher` (or `UnconfinedTestDispatcher`) for coroutines, as `TestCoroutineDispatcher` is deprecated.
- **Coverage**: Aim for high coverage in the `domain` and `data` layers.

## CI/CD
Every pull request runs the following checks via GitHub Actions (`.github/workflows/ci.yml`):
- `spotlessCheck` and `lintDebug` — code style and Android Lint
- `testDebugUnitTest` and `testReleaseUnitTest` — JVM unit tests
- `verifyPaparazziDebug` — screenshot verification
- `jacocoTestReport` — code coverage report (HTML/XML; uploaded as a CI artifact)
- `connectedDebugAndroidTest` on an Android emulator — instrumentation tests
- `assembleDebug`, `assembleDebugAndroidTest`, and `assembleRelease` — R8-minified release build

Ensure these pass locally before submitting.

### Low Memory Environments
If running the full suite causes memory issues on your machine, use the provided helper script:
```bash
./scripts/local-ci.sh --low-mem
```
Other useful flags:
- `--fast`: Skips Lint and screenshot tests.
- `--no-paparazzi`: Skips screenshot tests (which are memory intensive).
- `--low-mem`: Disables parallel execution and reduces Gradle heap size.
