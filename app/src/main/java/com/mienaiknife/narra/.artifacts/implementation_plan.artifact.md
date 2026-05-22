# Implementation Plan - Open Reader Screen from Widget Icon

This plan describes the changes required to open the app to the reader screen when the user taps on the article icon in the `NarraWidget`.

## Proposed Changes

### Widget State Management

I need to update `WidgetManager` to store the `articleId` in the Glance state so it can be retrieved when the widget is clicked.

#### [WidgetManager.kt](file:///var/home/grey/Documents/My Coding Projects/Android/Narra/app/src/main/java/com/mienaiknife/narra/ui/widget/WidgetManager.kt)

- Add `KEY_ARTICLE_ID` to the `companion object`.
- Update `updateState` function to accept `articleId: String?` and save it to preferences.

### Widget UI

Update `NarraWidget` to retrieve the `articleId` and add a click action to the article icon.

#### [NarraWidget.kt](file:///var/home/grey/Documents/My Coding Projects/Android/Narra/app/src/main/java/com/mienaiknife/narra/ui/widget/NarraWidget.kt)

- Retrieve `articleId` from Glance state in `provideGlance`.
- Pass `articleId` to `WidgetContent`.
- Add `.clickable(actionStartActivity<MainActivity>(/* intent with articleId */))` to the `Box` containing the article icon.

### Playback Service

Update `PlaybackService` to pass the `articleId` when updating the widget state.

#### [PlaybackService.kt](file:///var/home/grey/Documents/My Coding Projects/Android/Narra/app/src/main/java/com/mienaiknife/narra/service/PlaybackService.kt)

- Pass `state.article?.id` to `widgetManager.updateState`.

## Verification Plan

### Automated Tests
- I will run a build to ensure no compilation errors: `./gradlew assembleDebug`

### Manual Verification
- Since I cannot run the app and interact with the widget directly, I will verify the logic by:
    1. Checking that `MainActivity` correctly handles the `article_id` extra (already present in `MainActivity.kt`).
    2. Verifying the Glance `actionStartActivity` usage and intent parameter passing.
    3. Ensuring `WidgetManager` correctly persists the new key.
