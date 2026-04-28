# Getting Started

Follow these steps to set up the development environment and build Narra.

## Prerequisites
- Android Studio Ladybug (or newer).
- JDK 17 or higher.

## Setup Instructions

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

## Building and Running
- Select the `app` configuration and your target device (emulator or physical device).
- Click the **Run** button or use the shortcut `Shift + F10`.

## Testing
Run unit tests using the following command:
```bash
./gradlew test
```
