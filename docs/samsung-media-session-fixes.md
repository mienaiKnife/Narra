# Samsung-Specific Media Session & Widget Fixes

This document outlines critical implementation details required to maintain responsive Play/Pause controls and hardware button priority on Samsung (One UI) devices. 

> [!CAUTION]
> Avoid refactoring these specific areas without thorough testing on a physical Samsung device. Samsung's background process management is significantly stricter than standard Android.

## 1. Widget Unresponsiveness Fix
**Problem:** Using `MediaController` inside `GlanceAppWidget` or its callbacks leads to unresponsiveness. The `MediaController` connection is asynchronous and the Glance background scope often expires before the connection is established.

**Solution:** Use direct `Intent` signals to the `PlaybackService`.
- **File:** `PlaybackActionCallback.kt`
- **Mechanism:** Instead of `MediaController.play()`, use `context.startForegroundService(Intent(context, PlaybackService::class.java).apply { action = ACTION_TOGGLE })`.
- **Benefit:** Direct intents are delivered immediately regardless of the controller's connection state.

## 2. Hardware / Bluetooth Button Priority
**Problem:** Samsung devices often prioritize established media apps (like YouTube or Spotify) over new sessions. Standard Media3 automatic claim logic can fail, leaving Narra's `mediaButtonReceiver` with a `null` PendingIntent (`pi=null` in `dumpsys`).

**Solution:** Force session relevance, explicit user intent, and use a reflection-based workaround to manually set the `MediaButtonReceiver`.
- **File:** `PlaybackService.kt`, `MediaSessionUtils.kt`
- **Key Extras:** 
  - `android.media.IS_EXPLICIT = true`: Signals to the system that this session was explicitly started by the user.
  - `android.media.session.extra.EXTRA_SLOT_RESERVATION = true`: Helps the system UI reserve space and routing for this session.
- **Reflection Hack:** Use `MediaSessionUtils.forceActivationAndMbr()` to access the underlying `MediaSessionCompat` (hidden in Media3 1.1+) and explicitly call `setMediaButtonReceiver` and `setActive(true)`.
- **Reinforcement:** Re-trigger the activation cycle whenever playback starts to ensure Narra keeps priority even if other apps were recently played.
- **Unique Session ID:** Always set a static, unique ID in `MediaLibrarySession.Builder.setId("NarraPlaybackSession")`.
- **Advertising Commands:** Explicitly grant `Player.COMMAND_PLAY_PAUSE` and other standard commands in `onConnect`, even if the player already advertises them. This increases the session's "weight" in the system's priority stack.

## 3. Silence Player (Audio Priority Claim)
**Problem:** Because Narra's audio is delegated to the system TTS process (`com.google.android.tts` or similar), the Android system does not recognize Narra as the primary audio producer. Consequently, Narra "loses" media button priority to other background apps that are seen as active producers (e.g., Spotify, AntennaPod).

**Solution:** Run a silent `AudioTrack` loop within Narra's own process.
- **File:** `PlaybackService.kt`
- **Implementation:**
    - `startSilence()`: Starts an `AudioTrack` playing Mono, 16-bit, 44.1kHz silence.
    - `stopSilence()`: Stops the track.
- **Trigger:** Silence must be active whenever `playWhenReady` is true. This forces the system to list Narra in the "Audio playback" stack (`dumpsys media_session`), which is a prerequisite for claiming the "Media button session" slot.

## 4. Custom MediaButtonReceiver & Legacy Priority
**Problem:** Modern Media3 `MediaButtonReceiver` is often ignored by Samsung's legacy routing logic on older firmware or when multiple sessions exist.

**Solution:** Use a custom subclass and explicit `AudioManager` registration.
- **File:** `NarraMediaButtonReceiver.kt`, `PlaybackService.kt`
- **Mechanism:** 
    - Subclass `BroadcastReceiver` and delegate to `MediaButtonReceiver.onReceive`. This provides a hook for logging and ensures the broadcast intent is delivered.
    - Call `audioManager.registerMediaButtonEventReceiver(componentName)` whenever playback starts. This uses the legacy API to reinforce the app's desire to capture hardware buttons.
- **Active Pulse:** In `MediaSessionUtils.forceActivationAndMbr()`, the session is "kicked" by pulsing its active state (`setActive(false)` then `setActive(true)`). This forces the system to re-evaluate which app should hold the hardware button priority.

## 5. Playback Stability (SimpleBasePlayer Contract)
**Problem:** Violating the `SimpleBasePlayer` state contract causes internal Media3 crashes that are difficult to debug.
**Solution:** If a `PlaybackException` is reported in `getState()`, the playback state **MUST** be `Player.STATE_IDLE`. Never report an error while in `STATE_READY` or `STATE_BUFFERING`.

## 6. Playback Resumption
**Problem:** Bluetooth "Play" presses after the app has been killed won't wake Narra unless correctly declared.

**Solution:**
- **Manifest:** Ensure `com.mienaiknife.narra.service.NarraMediaButtonReceiver` is declared and `PlaybackService` handles `android.intent.action.MEDIA_BUTTON`.
- **Service:** Implement `onPlaybackResumption` in the session callback to provide the `MediaItem` the system should restart with.

## 7. Verification Commands
To verify if Narra has correctly claimed priority, run:
```bash
adb shell dumpsys media_session
```
**Check for:**
1. `Media button session is com.mienaiknife.narra/...` (Narra should be the session).
2. `Audio playback (lastly played comes first)`: `com.mienaiknife.narra` should be at the top.
3. `Last MediaButtonReceiver: MBR {pi=PendingIntent{...}, ...}` (Ensure `pi` points to `NarraMediaButtonReceiver`).
4. `android.media.IS_EXPLICIT=true` in the session extras.
5. Bitmask `actions=3967` in the `PlaybackState` section (shows Narra supports standard and prep actions).
