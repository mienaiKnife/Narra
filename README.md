# Narra

Narra is a mobile app (currently Android exclusive) that allows users to listen to webpages and ebooks read aloud by TTS in a podcast-like experience. Choose from a wide variety of TTS voices, subscribe to the RSS feeds of your favourite blogs, and queue up several texts to listen to without ads.

## Getting Started

Follow these steps to set up the development environment and build Narra.

### Prerequisites
- Android Studio Ladybug (or newer).
- JDK 17 or higher.

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/narra.git
   ```

2. **Open the project**:
   Open Android Studio and select "Open" to choose the project directory.

3. **Gradle Sync**:
   Wait for the project to finish syncing. If there are any issues, go to `File > Sync Project with Gradle Files`.

4. **API Keys (Optional)**:
   For certain cloud TTS providers, you may need to add API keys to your `local.properties` file. Do not commit these keys to the repository.

### Building and Running
- Select the `app` configuration and your target device (emulator or physical device).
- Click the **Run** button or use the shortcut `Shift + F10`.

### Testing
Run unit tests using the following command:
```bash
./gradlew test
```

## Documentation

- [User Guide](docs/USAGE.md) - Learn how to use Narra's features.
- [Architecture](docs/ARCHITECTURE.md) - Learn about the project's technical design.
- [TTS Engines](docs/TTS_ENGINES.md) - Guide for implementing and extending TTS providers.
- [Content Parsing](docs/CONTENT_PARSING.md) - How we extract text from RSS, ebooks, and Web.
- [Playback Lifecycle](docs/PLAYBACK_LIFECYCLE.md) - Understanding the media service and audio flow.
- [Testing Guide](docs/TESTING_GUIDE.md) - How to run and write tests for Narra.
- [Privacy Policy](docs/PRIVACY.md) - Our commitment to your privacy.
- [Roadmap](docs/ROADMAP.md) - Current status and future plans.
