# Testing Guide

Quality is critical for an open-source project. This guide explains the testing strategy for Narra and how to run and write tests.

## Test Levels

### Unit Tests
- **Location**: `app/src/test/java/`
- **Focus**: Business logic in ViewModels, Use Cases, and Repository logic that doesn't depend on the Android framework.
- **Run command**: `./gradlew test`

### Instrumented Tests
- **Location**: `app/src/androidTest/java/`
- **Focus**: UI components (Compose), Room database migrations, and logic that requires an Android environment.
- **Run command**: `./gradlew connectedAndroidTest`

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
- **Avoid Flakiness**: Ensure tests are deterministic. Use `TestCoroutineDispatcher` for coroutines.
- **Coverage**: Aim for high coverage in the `domain` and `data` layers.

## CI/CD
Every Pull Request triggers an automated build and runs the unit test suite via GitHub Actions. Ensure your tests pass locally before submitting.
