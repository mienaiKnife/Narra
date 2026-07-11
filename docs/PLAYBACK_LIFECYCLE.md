# Playback Service & Lifecycle

Audio playback in Narra is handled by a specialized implementation of the Media3 API, allowing for a consistent experience across system controls while utilizing on-device or cloud TTS.

## Architecture

Narra bridges the gap between traditional media players and Text-to-Speech engines by implementing a custom `TtsPlayer`.

### Components

- **`TtsPlayer`**: A custom implementation of Media3's `BasePlayer`. It wraps a `TtsEngine` and translates standard Media3 commands (play, pause, seek) into TTS actions. It also manages audio focus, noisy intents (headphone unplugging), and wake locks.
- **`PlaybackService`**: A `MediaSessionService` that hosts the `TtsPlayer`. It provides the `MediaSession` that enables system-wide media controls, lock screen integration, and foreground service persistence.
- **`PlaybackManager`**: A singleton coordinator that manages the high-level playback state. It handles the transition between articles and manages the sleep timer.
- **`PlaybackSettingsManager`**: Manages user preferences for playback, such as skip times, auto-play settings, and the selected TTS engine/model.
- **`TtsEngine`**: The underlying interface for actual speech synthesis (e.g., `AndroidTtsEngine`, `SherpaTtsEngine`).

## Samsung-Specific Optimizations

Narra includes critical workarounds for Samsung (One UI) devices to ensure control responsiveness and hardware button priority:
- **Reflection-based MBR**: Manually setting the `MediaButtonReceiver` on the underlying `MediaSessionCompat`.
- **Session Extras**: Using `android.media.IS_EXPLICIT` and `EXTRA_SLOT_RESERVATION` to claim system priority.
- **Widget Intents**: Using direct `Intent` signals to `PlaybackService` instead of `MediaController` for zero-latency widget actions.

See [samsung-media-session-fixes.md](samsung-media-session-fixes.md) for full technical details.

## The Playback Flow

1. **Initialization**: When the app starts, `PlaybackManager` restores the last played article from `PlaybackSettingsManager`.
2. **Loading Content**: When an article is selected, `PlaybackManager` parses the HTML content into `ContentBlock`s and hands the speakable text to `TtsPlayer`.
3. **Synthesis & Playback**:
    - `TtsPlayer` requests audio focus.
    - It instructs the `TtsEngine` to synthesize text paragraph by paragraph.
    - `TtsPlayer` updates its playback state to `STATE_READY`, allowing `PlaybackService` to enter the foreground.
4. **Synchronization**: As the `TtsEngine` speaks, it reports word-level progress. `TtsPlayer` translates these into `PositionInfo` updates, which the UI uses to highlight the current word/paragraph.
5. **Article Transition**: When an article ends, `PlaybackManager` automatically fetches the next item from the `ContentRepository` queue, potentially playing a chime and announcing the new title.

## Audio Focus & Interruptions

`TtsPlayer` manages `AudioManager.OnAudioFocusChangeListener`. 
- **Focus Loss**: The player pauses and abandons locks.
- **Transient Loss**: For notifications or navigation prompts, the player pauses synthesis and resumes once focus is regained.
- **Becoming Noisy**: If headphones are unplugged, `TtsPlayer` receives a broadcast and pauses playback immediately.

## Position Mapping

Because TTS doesn't have a fixed "duration" in the traditional sense, Narra maps playback position as follows:
- **Duration**: Total number of paragraphs * 1000ms.
- **Position**: (Current Paragraph Index * 1000ms) + (Percentage of current paragraph spoken * 1000ms).

This allows standard Media3 controllers and progress bars to function correctly while providing granular seeking capabilities.
