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

**Solution:** Force session relevance and explicit user intent.
- **File:** `PlaybackService.kt`
- **Key Extras:** 
  - `android.media.IS_EXPLICIT = true`: Signals to the system that this session was explicitly started by the user.
  - `android.media.session.extra.EXTRA_SLOT_RESERVATION = true`: Helps the system UI reserve space and routing for this session.
- **Unique Session ID:** Always set a static, unique ID in `MediaLibrarySession.Builder.setId("NarraPlaybackSession")`.
- **Advertising Commands:** Explicitly grant `Player.COMMAND_PLAY_PAUSE` and other standard commands in `onConnect`, even if the player already advertises them. This increases the session's "weight" in the system's priority stack.

## 3. Playback Resumption
**Problem:** Bluetooth "Play" presses after the app has been killed won't wake Narra unless correctly declared.

**Solution:**
- **Manifest:** Ensure `androidx.media3.session.MediaButtonReceiver` is declared and `PlaybackService` handles `android.intent.action.MEDIA_BUTTON`.
- **Service:** Implement `onPlaybackResumption` in the session callback to provide the `MediaItem` the system should restart with.

## 4. Verification Commands
To verify if Narra has correctly claimed priority, run:
```bash
adb shell dumpsys media_session
```
**Check for:**
1. `Media button session is com.mienaiknife.narra/...`
2. `mediaButtonReceiver=MBR {pi=PendingIntent{...}, ...}` (Ensure `pi` is NOT `null`).
3. `android.media.IS_EXPLICIT=true` in the session extras.
