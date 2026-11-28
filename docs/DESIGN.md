# Narra Design Language

Narra is a listening app. The interface should feel calm, readable, and out of the way: the text
and the playback controls are the product, and everything else is supporting chrome. The UI is
built with Jetpack Compose and Material 3, with a small custom theme in
`app/src/main/java/com/mienaiknife/narra/ui/theme/`.

> **Read this before changing the GUI.** Every Compose screen, reusable component, theme token,
> Glance widget, `@Preview`, or Paparazzi screenshot test must follow this document. The design
> language is implemented in code; this document explains the intent behind it and the conventions
> that keep screens consistent. If a change needs to depart from these rules, update this document
> in the same pull request.

## Design Principles

1. **Content first.** Titles, article text, and playback state lead. Avoid decorative panels,
   gradients, and ornament that compete with reading.
2. **One accent.** Narra cyan is reserved for primary actions, selection, progress, links, and
   section labels. It is not a general-purpose decoration color.
3. **Quiet, tonal surfaces.** Grouping comes from `surfaceContainer` against `background`, not
   from shadows. The app is intentionally flat.
4. **One structural rhythm.** Top-level screens share the same header, side padding, and section
   spacing. Settings screens share the same rows. Novel layouts should reuse those patterns.
5. **Legible at every size.** Type scales with the system font scale, long titles shrink or
   truncate instead of clipping, and an OpenDyslexic font option is available.
6. **Touch first.** Rows are full-width targets, icon buttons keep the 48 dp minimum, and the
   primary playback control is large and obvious.
7. **Accessible by construction.** Every user-visible string comes from resources, icons have
   meaningful descriptions, RTL icons are mirrored, and color is never the only signal.
8. **Verify visually.** A UI change is not done until its previews and screenshot tests render in
   both light and dark themes.

## Where the Design Lives

| Area | Source of truth |
| --- | --- |
| Color palette and Material color schemes | `ui/theme/Color.kt` |
| Type scale and bundled fonts | `ui/theme/Type.kt` |
| Theme composable, spacing local, edge-to-edge | `ui/theme/Theme.kt` |
| User theme and reader preferences | `ui/theme/ThemeManager.kt`, `ThemeViewModel.kt` |
| Shared UI components | `ui/components/` |
| Screens and screen-level layout | `ui/screens/` |
| Reader rendering | `ui/screens/ReaderScreen.kt`, `ui/screens/reader/` |
| Home screen widget | `ui/widget/` |
| Compose previews | `@Preview` functions beside each screen/component |
| Committed screenshots | `app/src/test/snapshots/images/` |
| Screenshot test sources | `app/src/test/java/com/mienaiknife/narra/ui/screenshots/` |
| Product screenshots used in the README | `docs/images/` |

## Color

### Palette

| Token | Value | Meaning |
| --- | --- | --- |
| `NarraBlack` | `#191919` | Dark page background; inverse surface in light mode; splash background in dark mode |
| `NarraDarkGrey` | `#282828` | Dark surface and grouped container; light secondary text |
| `NarraCyan` | `#1D8A9C` | Brand accent and `primary` |
| `NarraCyanTrack` | `NarraCyan` at 50% | `primaryContainer`: progress tracks, switch tracks, selected nav indicator |
| `NarraWhite` | `#EDEDED` | Light page background; dark primary text and `onPrimary`; splash background in light mode |
| `NarraGrey` | `#949494` | Light `surface`; dark secondary text (`onSurfaceVariant`) |
| `NarraLightGrey` | `#E0E0E0` | Light `surfaceContainer` for cards and grouped rows |

### Schemes

Only the roles below are overridden. Every other Material 3 token keeps its library default, so
prefer the listed tokens in new UI and check contrast before pairing an unlisted role.

| Role | Light | Dark | Typical use |
| --- | --- | --- | --- |
| `primary` | `NarraCyan` | `NarraCyan` | Filled buttons, links, progress, section labels, word highlight |
| `onPrimary` | `NarraWhite` | `NarraWhite` | Content on primary fills |
| `background` | `NarraWhite` | `NarraBlack` | Page background on every screen |
| `onBackground` | M3 default (dark) | `NarraWhite` | Headline and body text on the page |
| `surface` | `NarraGrey` | `NarraDarkGrey` | Component surface (for example the pull-to-refresh container) |
| `onSurface` | `NarraBlack` | `NarraWhite` | Primary text and icons on surfaces |
| `surfaceContainer` | `NarraLightGrey` | `NarraDarkGrey` | Cards, mini player, grouped rows, dialogs |
| `onSurfaceVariant` | `NarraDarkGrey` | `NarraGrey` | Secondary text, metadata, inactive icons |
| `primaryContainer` | `NarraCyanTrack` | `NarraCyanTrack` | Progress tracks, switch tracks, nav selection indicator |
| `inverseSurface` / `inverseOnSurface` | `NarraBlack` / `NarraWhite` | `NarraWhite` / `NarraBlack` | Inverse content, such as snackbars |
| `error` / `onError` | `#BA1A1A` / white | `#FFB4AB` / `#690005` | Destructive actions and error text |
| `errorContainer` / `onErrorContainer` | `#FFDAD6` / `#410002` | `#93000A` / `#FFDAD6` | Error surfaces |

Color rules:

- **Never hardcode brand colors in screens or components.** Use `MaterialTheme.colorScheme.*` so
  dynamic color, light mode, and dark mode continue to work.
- **Cyan means interactive or active.** Use `primary` for primary buttons, links, progress, and
  section labels. Selected navigation destinations keep `onBackground` icon/text over a
  `primaryContainer` pill. Do not fill large decorative areas with cyan.
- **Use `primaryContainer` for tracks and quiet states.** Progress tracks, switch tracks, and the
  selected navigation pill use the 50% cyan wash.
- **Use `surfaceContainer` to group.** Cards, mini player, grouped settings, and dialogs sit one
  step above `background`. Do not add elevation to create separation.
- **Dim completed content, do not recolor it.** Finished queue rows and article cards use
  `onSurface.copy(alpha = 0.6f)` and `onSurfaceVariant.copy(alpha = 0.6f)`.
- **Errors use the error roles.** Destructive menu items and delete confirmations use `error`;
  never use cyan for a destructive action.

### Brand

The launcher icon (`docs/images/app-icon.svg`) is a tree reading a book: growth plus listening, in
the same cyan-on-neutral palette. The splash screen is a plain `NarraWhite`/`NarraBlack` background
with that icon. Do not introduce a second logo, mascot, or decorative illustration style inside the
app; the reader screenshots in `docs/images/` show the intended visual tone.

### Theme Resolution

`MainActivity` reads `ThemeUiState` and passes the resolved values into `NarraTheme` at the app
root. The app follows the system light/dark setting by default; the user can override it, opt into
Material You dynamic color (Android 12+), and choose the reader font family. When dynamic color is
enabled, the wallpaper-derived scheme replaces the brand palette; previews and screenshot tests
must still pin `dynamicColor = false` so they stay deterministic.

`NarraTheme` also owns edge-to-edge setup: transparent status bar, navigation bar matched to
`surfaceContainer`, and light/dark system bar icon appearance. Screens must not call
`enableEdgeToEdge()` themselves. Instead:

- apply `statusBarsPadding()` to the top-level screen column or box;
- apply `navigationBarsPadding()` to bottom overlays such as snackbars and the reader FAB;
- rely on the root `Scaffold` for bottom navigation insets and never double-pad inside it.

## Typography

The full Material 3 scale is defined in `Type.kt` with `includeFontPadding = false` and
`LineHeightStyle` centered and trimmed on both sides. The roles actually used in the app are:

| Role | Size / line height | Weight | Use |
| --- | --- | --- | --- |
| `headlineSmall` | 24 / 32 sp | Normal | Top-level screen titles, sub-screen titles in `ScreenHeader`, empty-state titles |
| `titleLarge` | 22 / 28 sp | Normal | Dialog and bottom-sheet titles |
| `titleMedium` | 16 / 24 sp | Medium | Section labels (with `primary`), carousel headings |
| `bodyLarge` | 16 / 24 sp | Normal | Settings row titles and primary list content |
| `bodyMedium` | 14 / 20 sp | Normal | Row subtitles, article and queue titles, descriptions |
| `bodySmall` | 12 / 16 sp | Normal | Source, date, and other compact metadata |
| `labelSmall` | 11 / 16 sp | Medium | Timestamps and progress values |
| `labelLarge` | 14 / 20 sp | Medium | Button labels (Material default) |

`display*` and `titleSmall` exist in the scale for completeness but are not used by current
screens. Reader headings are the one intentional exception: they derive from
`headlineLarge`/`headlineMedium`/`headlineSmall` and scale with the user's reader font size.

Typography rules:

- **Screen title:** `headlineSmall` with `onBackground`.
- **Section heading:** `titleMedium` with `primary`, usually followed by 8 dp of space.
- **Home carousel heading:** `titleMedium` with `onBackground`. Carousel headings sit on the page
  rather than opening a settings group, so they stay neutral; this is the one section-heading
  exception.
- **Settings row:** `bodyLarge` title plus `bodyMedium` subtitle in `onSurfaceVariant`.
- **List row:** `bodyMedium` title, `bodySmall` metadata, `labelSmall` timestamps.
- **No ad-hoc font sizes in normal screens.** Size belongs to the type role. The reader (user
  controlled), rich-text super/subscripts, and the Glance widget (which cannot read Compose's
  `MaterialTheme.typography`) are the intentional exceptions.
- **Use `AdaptiveText` for titles in constrained containers.** Article cards, queue rows, the mini
  player, and similar rows pass a `maxLines` and ellipsis overflow. It shrinks text only when the
  system font scale makes the nominal size overflow, so normal layouts keep their intended size.
  Do not use it for long prose.
- **Do not override `platformStyle`, line height, or letter spacing** outside the theme or the
  reader's deliberate line-spacing control.

### Font Family

`FontFamily.Default` (the system font, labelled "Roboto" in settings) is the default. The bundled
`OpenDyslexic3` family is an accessibility option. `getFontFamily(name)` maps unknown names back
to the default.

`MainActivity` resolves the selected family and passes it to `NarraTheme`, so the choice changes
app typography, not only the reader. OpenDyslexic3 also changes `LocalNarraSpacing.itemVertical`
from 4 dp to 0 dp because its taller glyphs already create enough separation. Use
`LocalNarraSpacing.current.itemVertical` between a title and its subtitle rather than a literal
`Arrangement.spacedBy(4.dp)`.

The reader additionally exposes font size (12–36 sp, default 18 sp) and line spacing (1.0–2.0,
default 1.0). Reader body text uses a base line height of `fontSize * 1.6 * lineSpacing`; keep that
formula when changing reader typography.

## Spacing, Shape, and Elevation

### Spacing

The base unit is 4 dp. These values recur across the app:

| Space | Use |
| --- | --- |
| 4 dp | Title-to-subtitle gap via `LocalNarraSpacing.itemVertical`; compact internal gaps |
| 8 dp | Vertical padding on settings rows; gap between a control and its label |
| 12 dp | Gap between thumbnail and text; gap between cards in a horizontal carousel |
| 16 dp | Standard horizontal screen padding; top spacer above the header; gap below the header; **gap between list rows**; section heading bottom padding; leading-icon side spacing in list rows |
| 24 dp | Gap between major sections and settings groups; reader horizontal padding; bottom-sheet content padding |
| 32 dp | Final scroll spacer; back-arrow icon size |
| 48 dp | Minimum height for top-level header rows and touch targets |
| 56 dp | Reader scroll-to-current FAB |
| 64 dp | List and mini-player thumbnails; empty-state icon |
| 140 dp | Article card width and image height |
| 260 dp | Maximum height of the voice list |
| 400 dp | Maximum bottom-navigation content width on tablets |
| 600 dp | `smallestScreenWidthDp` breakpoint for tablet layout |

Never hand-roll the tab skeleton. Top-level tabs ("Home", "Queue", "Add", "Inbox", "Settings") use
`TopLevelScreen`, which applies the status bar inset, the 16 dp spacer, a 48 dp header without a
back arrow, an optional one-line `meta` summary (for example the queue count and remaining time),
and the 16 dp gap below the header:

```kotlin
TopLevelScreen(
    title = stringResource(R.string.nav_queue),
    headerActions = {
        OverflowMenu(
            expanded = showMenu,
            onExpandedChange = { showMenu = it },
            contentDescription = stringResource(R.string.action_menu),
        ) {
            // Menu items
        }
    },
    meta = {
        Text(
            text = queueSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    },
) {
    NarraPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.weight(1f),
    ) {
        // List or EmptyState
    }
}
```

Sub-screens use `ScreenHeader` instead. Settings sub-screens use `SettingsScaffold`, which supplies
`ScreenHeader`, a 16 dp horizontal gutter, the scroll container, and the 32 dp final spacer. A
sub-screen made only of full-width `NarraListItem` rows (About, Licenses) passes
`horizontalPadding = 0.dp` so its rows align with the top-level Settings list. Do not introduce a
`TopAppBar`; the app deliberately uses the flat in-content header. Header actions are
`OverflowMenu` (a 32 dp `MoreVert` icon), not the navigation `Menu` icon.

### Shapes

| Radius | Use |
| --- | --- |
| 2 dp | Progress bar ends after clipping the 5 dp bar |
| 5 dp | `ArticleCard` container |
| 8 dp | Filled action buttons, including empty-state and add-pronunciation buttons |
| 12 dp | Queue and feed thumbnails; pronunciation cards; home-screen widget corners |
| 16 dp | Add-screen action cards |
| 24 dp | Outlined text fields everywhere (pill-like) |
| `MaterialTheme.shapes.small` | Reader block quotes, via the theme's small shape |
| `MaterialTheme.shapes.medium` | Reader images |
| `CircleShape` | Reader FAB, queue play/pause affordance, scrollbar thumb |

Use the values above rather than inventing a new radius. Dialogs, menus, and modal bottom sheets
keep their Material 3 defaults.

### Elevation and Motion

The app does not set elevation or shadows on its own surfaces. Navigation, mini-player, cards, and
settings groups are flat; contrast and spacing do the work. Material components that need a
default elevation (menus, dialogs, bottom sheets, the navigation bar) keep it.

Motion is restrained:

- root navigation transitions are intentionally disabled, so tabs switch instantly;
- the reader is the one exception: it opens with a 300 ms upward slide and dismisses with the
  matching downward slide, which also follows the predictive-back gesture, and the top-bar chevron
  uses the same motion. The reader stays full-bleed for the whole slide; the mini-player and
  navigation bar start returning just before the dismiss slide ends and come back over 300 ms.
  Every other push or pop stays instant;
- every route root paints `MaterialTheme.colorScheme.background` (through `TopLevelScreen`,
  `SettingsScaffold`, or the screen's own root) so a screen is always opaque; the status bar strip
  then matches the page instead of the window background;
- the reader follows playback with a low-stiffness spring scroll;
- the custom scrollbar fades in and out over 300 ms and disappears after 1.5 s of inactivity;
- settings deep links use `flashHighlight`, which waits 300 ms for navigation to settle, flashes
  `primary` at 20% for one second, then fades out over 500 ms.

Avoid adding decorative animation. Animation should explain a state change, not compete with
playback.

## Shared Components

Reuse these before building a one-off version. They live in `ui/components/`.

### `TopLevelScreen`

The skeleton for the five tabs. It owns `statusBarsPadding`, the 16 dp top spacer, the 48 dp header
row (`TopLevelScreenHeader`), the optional `meta` line, and the 16 dp gap below the header. Use it
instead of assembling a `Column` in each tab.

### `ScreenHeader`

The sub-screen header: 32 dp back arrow, `headlineSmall` title with ellipsis, optional trailing
actions, and 16 dp horizontal padding. Every screen reached by pushing a route uses it. Use
`Icons.AutoMirrored.Filled.ArrowBack` so RTL is handled.

### `SettingsScaffold`

The skeleton for settings sub-screens. It supplies `statusBarsPadding`, an optional engine-init
`LinearProgressIndicator`, `ScreenHeader`, a horizontal gutter (16 dp by default, overridable via
`horizontalPadding`), a vertical scroll container, and the 32 dp final spacer. Pass
`horizontalPadding = 0.dp` for screens that are only full-width list rows so those rows stay
full-bleed like the top-level Settings list. Screens do not add their own scroll container or
trailing spacer.

### `OverflowMenu`

The standard header action: a 32 dp `MoreVert` icon that opens a dropdown with default menu colors.
Callers own the expanded state so the menu can close when an item is chosen. Do not use
`Icons.Default.Menu` for an overflow menu.

### `BottomNavBar`

Five destinations: Home, Queue, Add, Inbox, Settings. Queue also covers History, Inbox covers
Feeds and feed articles, and Settings covers all settings sub-screens. Selected items use
`onBackground` for icon and text with a `primaryContainer` indicator; unselected items use
Material defaults. On tablets (`smallestScreenWidthDp >= 600`) the bar content is capped at 400 dp
and centered while the container background still spans the window.

### `MiniPlayer`

A flat `surfaceContainer` strip above the bottom navigation, hidden when no article is playing. It
shows a 64 dp thumbnail, a two-line `bodyMedium` title, a one-line `labelSmall` source, a 32 dp
play/pause icon, and a 5 dp progress bar clipped to 2 dp using `primary` over
`primaryContainer`. The thumbnail falls back to a source-specific icon (EPUB, RSS, or web) while
the image loads. Tapping the strip opens the reader.

### `ArticleCard` and `QueueItem`

Both use a 64/140 dp image area with the same RSS/EPUB/web fallback icons and dim finished content
to 60% opacity. `ArticleCard` is a 140 dp-wide `surfaceContainer` card with a 5 dp radius;
`QueueItem` is a flat full-width row with a 12 dp rounded thumbnail, drag handle, metadata,
progress, and a 42 dp circular play/pause affordance with a 1.5 dp `onSurface` border. Long-press or
tap opens the item menu with played, queue, and visit-site actions.

### Settings Rows

`SettingsSwitchRow`, `SettingsActionRow`, `SettingsSliderRow`, `SettingDropDownItem`, and
`SettingsSectionLabel` implement the standard settings pattern. Reuse them instead of hand-rolling a
row; a hand-rolled switch loses the whole-row `Role.Switch` target and drifts from the shared colors.

- group headings use `SettingsSectionLabel` (`titleMedium` in `primary`, 24 dp above and 8 dp below;
  the first group on a screen passes `first = true`);
- a heading that carries a trailing control (the Voices language filter, its Reset button) centres
  that control on the heading's line: the control is a 48 dp touch target, so the heading and the
  control share a centred 48 dp row and the group gap is applied to the row. Padding the heading
  text instead leaves the control sitting above the heading;
- full-width clickable/toggleable row with 8 dp vertical padding — do not override the padding;
- `bodyLarge` title and `bodyMedium` subtitle in `onSurfaceVariant`;
- title-to-subtitle gap from `LocalNarraSpacing.current.itemVertical`;
- switch thumb/track colors come from `NarraSwitch` (`primary`/`primaryContainer` when checked,
  `onSurfaceVariant`/`surfaceContainer` when unchecked). Never copy the colors into a screen;
- sliders use `SettingsSliderRow`, which owns the label/description rhythm and the primary over
  `primaryContainer` track;
- dropdown rows use `SettingDropDownItem` (current value in `primary`, radio buttons in the menu) and
  expose `enabled` for engine-initializing states. There is no second dropdown idiom;
- rows that opt into search highlighting pass `bringIntoViewRequester(...)` and
  `flashHighlight(...)` through the row `modifier`.

Voice model rows (`Settings > Voices > Voice data`) show one metadata line rendered from the
structured `VoiceProfile`: quality, accent, and gender or voice count, for example
`Low quality · American English · Female`. Render it through `voiceProfileDescription()`; do not
hand-write a description or place free-form copy in the catalog. The line uses `bodyMedium` in
`onSurfaceVariant`, and its ` · ` separator is a `translatable="false"` string resource.

Settings screens label each group with `titleMedium` in `primary`, separate groups with 24 dp, and
end the scroll with a 32 dp spacer. When adding a setting, add it to `SettingsSearchData` and use a
`BringIntoViewRequester` plus `flashHighlight` when the search result navigates to it.

### `NarraListItem`

The standard icon + title + subtitle row used by Settings, About, and Licenses. It applies
`bodyLarge`/`bodyMedium`, a 24 dp leading icon, a transparent container, and `Role.Button` for
clickable rows. The leading icon gets its side spacing from Material 3 `ListItem`'s own
`LeadingContentEndPadding` (16 dp), so the 16 dp screen inset and the icon-to-text gap match; do not
add padding to the icon. Rows with richer content (progress bars, multiple supporting lines, inline
icon buttons) use a Material 3 `ListItem` with `narraListItemColors()` and the same text styles.

### `EmptyState`

The shared empty state: a 64 dp icon in `primary` at 50%, a `headlineSmall` title, an optional
`bodyMedium` description in `onSurfaceVariant`, and an optional filled primary action with an 8 dp
corner radius. Every list empty state uses it; do not leave a bare centered `Text`. When the empty
state sits inside a pull-to-refresh container, pass it
`Modifier.fillMaxSize().verticalScroll(rememberScrollState())` so the gesture still works.

### `NarraProgressBar`

The app's only linear progress bar: 5 dp tall, clipped to a 2 dp radius, `primary` over
`primaryContainer`, no stop indicator. Use it for the mini player, cards, queue rows, download rows,
and the reader instead of configuring `LinearProgressIndicator` locally.

### `NarraPullToRefreshBox`

Pull-to-refresh with the app's indicator styling (`surface` container, `primary` spinner). Use it
instead of constructing `PullToRefreshBox` and its indicator in each list.

### Text fields

Every `OutlinedTextField` uses `NarraTextFieldShape` (24 dp pill) and `narraTextFieldColors()`
(`outlineVariant` border, `primary` when focused). That includes the settings search field, the Add
screen, the pronunciation editor, and the reader search sheet.

### `NarraScrollbar`

An overlay scrollbar for `LazyListState` or `ScrollState`. It has a 16 dp-wide interaction strip and
a 4 dp circular thumb at `onSurface` 30% opacity. It appears while scrolling or dragging and fades
after 1.5 s. Place it inside a `Box` aligned to `Alignment.CenterEnd`, as the queue, inbox, feed, and
history lists do.

### Empty, Loading, and Error States

- **Empty:** use `EmptyState` — centered 64 dp icon tinted `primary` at 50%, `headlineSmall` title,
  optional `bodyMedium` description in `onSurfaceVariant`, then an optional filled primary button.
- **Loading:** a centered `CircularProgressIndicator`; long-running engine setup may also show a
  full-width `LinearProgressIndicator` above the header (`SettingsScaffold` provides this).
- **Error:** `ErrorView` (`headlineSmall` title, the message, an outlined back action, and a filled
  8 dp retry action).
- **Transient feedback:** snackbars at the bottom, or a toast for import confirmations. There is one
  snackbar host, owned by the root `Scaffold` in `Navigation.kt`; screens receive its
  `SnackbarHostState` instead of creating their own host.

### Dialogs and Sheets

Use `AlertDialog` for destructive confirmations with `containerColor =
surfaceContainer`; the confirm action uses `error` text. Use `ModalBottomSheet` for sort, sleep
timer, and search. Keep actions in sentence case and put the destructive action last.

## Screen Patterns

- **Top-level tabs:** use `TopLevelScreen` — flat header with no back arrow, `statusBarsPadding`,
  content starts 16 dp below the header, and the optional `meta` summary sits directly under the
  title. The root `Scaffold` supplies `MiniPlayer` and `BottomNavBar`.
- **Sub-screens:** `ScreenHeader` with back arrow, then a scrollable column. Card- or control-based
  screens use `SettingsScaffold` with its 16 dp side gutter; screens that are only full-width list
  rows (About, Licenses) pass `horizontalPadding = 0.dp` so their rows match the top-level lists.
  The bottom navigation remains visible.
- **Lists:** rows are separated by 16 dp, sections by 24 dp, and the list ends with 32 dp of bottom
  padding. The same rhythm applies to queue, history, inbox, feeds, and feed articles.
- **Reader:** full-screen and outside the bottom navigation. It uses `background`, 24 dp side
  padding, and its own overlay controls. The current paragraph has a `primary` 15–20% background;
  the current word is drawn with a `primary` 40% highlight; links are `primary` and switch to
  `onSurface` when they fall under the word highlight. Keep those alphas consistent when refining
  the reader.
- **Widget:** the Glance widget is dark-only. It uses `NarraWidget.DarkThemeColors` (the app's
  `DarkColorScheme`), 12 dp corners, and the same background/surface pairing as the app. Widget
  playback actions must use direct `Intent` signals to `PlaybackService`; see
  [PLAYBACK_LIFECYCLE.md](PLAYBACK_LIFECYCLE.md) and
  [samsung-media-session-fixes.md](samsung-media-session-fixes.md).

## Accessibility and Localization

- Put every user-facing string in `strings.xml`; use `<plurals>` for counts. Keep layout literals
  in code, never display text.
- Decorative icons use `contentDescription = null`. Interactive icons describe the action and,
  where useful, the article title (for example "Play <title>").
- Use `Icons.AutoMirrored.*` for directional icons (back, sort, open-in-new, playlist) and avoid
  absolute left/right padding. Test the layout in RTL.
- Give custom clickable rows a semantics `Role`; use `toggleable(role = Role.Switch)` for a switch
  row and merge semantics for a card whose whole surface is one action.
- Never use color alone. Played state also changes semantics and opacity; playback progress also
  has elapsed and remaining labels.
- Respect font scale. Use `heightIn` rather than fixed heights around text and `AdaptiveText` for
  fixed-width titles. Check at least 1.5× font scale before shipping.
- Long-press actions should give haptic feedback, as queue rows do.
- The reader font, size, and line spacing are accessibility features. Do not bypass
  `ThemeUiState` when rendering reader content.

## Previews and Screenshots

Narra uses two visual verification layers. They serve different purposes and neither replaces the
other.

### Compose Previews

In-file `@Preview` functions render in Android Studio's layout editor. They are part of the source
and must keep compiling.

Conventions:

- **Preview the stateless `*Content` composable, not the Hilt-backed screen.** Screens that take a
  ViewModel expose a `...Content` function with an immutable UI state and callbacks; preview that
  function with fake data.
- **Use the canonical pair annotation:** `@PreviewLightDark` renders the same body in light and
  dark. Full screens, empty/error states, and anything color-sensitive should use it.
- **Let the theme follow the preview:** call `NarraTheme(dynamicColor = false)` and do not hardcode
  `darkTheme`, so the light and dark previews render correctly.
- **Always disable dynamic color.** Dynamic color makes previews depend on the host wallpaper and
  breaks visual comparison.
- **Wrap full screens in the app chrome.** Use `Scaffold(bottomBar = { BottomNavBar(...) })` and
  apply its inner padding, matching the real root scaffold. Include `MiniPlayerContent` with fake
  state when the screen state includes a playing article, or provide a dedicated mini-player
  preview.
- **Give bare components an opaque background.** Either set `backgroundColor = 0xFF191919` on the
  preview or wrap the component in `Surface(color = MaterialTheme.colorScheme.background)`. A bare
  component can otherwise render against an unpredictable canvas.
- **Use real sample content.** `SampleArticles` and hand-built UI states live in the data models;
  prefer them over empty placeholders when the visual result matters.
- **Keep previews free of real I/O.** Do not let a preview touch DataStore, the network, Hilt,
  playback, or the file system. Screens that own a `ViewModel` expose a stateless `*Content`
  composable; preview that instead of subclassing the ViewModel.
- **Pass the font explicitly when layout depends on it.** Reader and UI-settings previews call
  `getFontFamily("Roboto")` so the rendered type matches the default theme.
- `@PreviewLightDark` cannot carry `showSystemUi`; when system bars or insets are part of what you
  are checking (as in `ReaderScreenPreview`), use two explicit `@Preview` annotations
  (`UI_MODE_NIGHT_NO` and `UI_MODE_NIGHT_YES`) on the same body.
- **Add at least one preview for a new screen or component.** A new full screen must be checked in
  both themes through the screenshot suite.

Full-screen preview template:

```kotlin
@PreviewLightDark
@Composable
fun QueueScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                QueueScreenContent(
                    articles = SampleArticles.all.filter { it.isInQueue },
                    onHistoryClick = {},
                )
            }
        }
    }
}
```

Component preview template:

```kotlin
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
)
@Composable
fun MiniPlayerPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            MiniPlayerContent(
                article = SampleArticles.sampleArticle1,
                isPlaying = false,
                isBuffering = false,
                currentPosition = 45_000L,
                duration = 180_000L,
                onExpand = {},
                onTogglePlayPause = {},
            )
        }
    }
}
```

### Paparazzi Screenshot Tests

Paparazzi renders Compose on the JVM, so the checked-in PNGs are the visual regression suite. CI
runs `verifyPaparazziDebug`; a changed or missing image fails the build.

- **Location:** `app/src/test/java/com/mienaiknife/narra/ui/screenshots/`.
- **Device:** `Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)` for every suite.
- **Snapshots:** commit them under `app/src/test/snapshots/images/`. The file name is generated
  from the fully qualified test class and method, so renaming either orphans an image.
- **Coverage:** add a test for each new screen and each meaningful state (empty, populated, error,
  loading, dialog, disabled). Add a dark variant where color or contrast changes; settings and
  list-heavy screens should have both themes.
- **Use immutable state and callbacks.** Snapshot the `*Content` composable with fake data. Do not
  snapshot a Hilt-backed screen.
- **Wrap the content in a Surface.** Use the shared `SnapshotSurface` helper in the screenshot test
  source set, which applies `NarraTheme(darkTheme = ..., dynamicColor = false)`, fills the size, and
  paints `MaterialTheme.colorScheme.background`. Without it, light-theme content can render against
  Paparazzi's raw canvas instead of the app background. Do not define per-file copies.
- **Keep snapshots deterministic.** No clocks, network, random values, animations, or real
  `ViewModel`s. Use fixed sample articles and fixed timestamps.
- **Review the PNG diff before recording.** A screenshot update is a visual change and should be
  inspected like code, not recorded blindly.

Commands:

```bash
# Render/update snapshots after an intentional visual change
./gradlew recordPaparazziDebug

# Verify committed snapshots exactly as CI does
./gradlew verifyPaparazziDebug

# Run the unit suite without the memory-heavy screenshot tests
./gradlew testDebugUnitTest -PskipPaparazzi

# Full local CI, or skip screenshots on a low-memory machine
./scripts/local-ci.sh
./scripts/local-ci.sh --no-paparazzi
```

Screenshot test template:

```kotlin
class NewScreenScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun newScreen_Default() {
        paparazzi.snapshot {
            SnapshotSurface {
                NewScreenContent(/* fake state */)
            }
        }
    }

    @Test
    fun newScreen_Dark() {
        paparazzi.snapshot {
            SnapshotSurface(darkTheme = true) {
                NewScreenContent(/* fake state */)
            }
        }
    }
}
```

`SnapshotSurface` is defined once in
`app/src/test/java/com/mienaiknife/narra/ui/screenshots/SnapshotSurface.kt`; do not add a private
copy to a suite.

Previews and screenshots are complementary: a preview is the fast iteration loop inside Android
Studio; a Paparazzi test is the committed, CI-enforced record. Adding a `@Preview` does not
automatically add a screenshot test, and adding a screenshot test does not replace checking the
preview in the IDE.

## UI Change Checklist

Before opening a GUI pull request:

- [ ] Read this document and reused the existing components before adding a new one.
- [ ] Used `TopLevelScreen`, `SettingsScaffold`, `EmptyState`, `NarraListItem`, `NarraProgressBar`,
      and the settings rows instead of hand-rolling their layouts.
- [ ] Used theme roles for color, the type scale for text, and the standard spacing and shapes.
- [ ] Kept the standard screen structure (top-level header or `ScreenHeader`; flat surfaces) and the
      16/24/32 list rhythm.
- [ ] Added or updated a `@PreviewLightDark` with `NarraTheme(dynamicColor = false)`.
- [ ] Checked the layout in light and dark themes, at large font scale, and with long titles.
- [ ] Put new strings in `strings.xml`; added content descriptions and RTL-safe icons.
- [ ] Added or updated a Paparazzi test for each meaningful visual state.
- [ ] Recorded snapshots only after reviewing the image diff.
- [ ] Ran `./gradlew spotlessApply`, `./gradlew spotlessCheck lintDebug`,
      `./gradlew testDebugUnitTest -PskipPaparazzi`, and `./gradlew verifyPaparazziDebug`.
- [ ] Confirmed the change adds no new lint warnings in `app/build/reports/lint-results-debug.txt`.

## Related Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — layer boundaries and project structure.
- [PLAYBACK_LIFECYCLE.md](PLAYBACK_LIFECYCLE.md) — playback UI state and the media session contract.
- [TESTING_GUIDE.md](TESTING_GUIDE.md) — full test strategy, including screenshot tests.
- [CONTRIBUTING.md](CONTRIBUTING.md) — code style, commits, dependency updates, and CI checks.
- [LOCALIZATION.md](../LOCALIZATION.md) — translating the app.
- [README.md](../README.md) — product overview and reader screenshots.
