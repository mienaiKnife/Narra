# Narra Improvement Todo

Audit date: 2026-09-10
Reference: full audit report in session. Paths relative to repo root.
Check off items as they land. Prefer one isolated commit + regression test per item.

## P0 — Correctness bugs

- [x] **Android TTS never fires end-of-article.** `tts/android/AndroidTtsEngine.kt:100-105`
      `onDone` sets `Finished` then `Ready` synchronously; `StateFlow` conflates, so
      `TtsPlayer.kt:205-216` (`STATE_ENDED`/queue advance) is never reached.
      Fix: emit `Finished` only; set `Ready` on next `onStart`/`stop`, or use a one-shot event.
- [x] **`isPreparing` not reset on empty article.** `playback/TtsPlayer.kt:522-535`
      Early return leaves state stuck `BUFFERING` and suppresses both error handlers.
      Fix: reset `isPreparing = false` (try/finally) before `return`.
- [x] **`handleSetMediaItems` throws on empty list.** `playback/TtsPlayer.kt:433`
      `coerceIn(0, items.size - 1)` → `coerceIn(0,-1)` before the `takeIf`.
      Fix: branch on `items.isEmpty()` first.
- [x] **Sherpa synthesis exception wedges playback.** `tts/ondevice/SherpaTtsEngine.kt:348-386,555-632`
      Channel never closed/finished on throw; `playAudioStream` blocks forever.
      Fix: finish/close channel in `finally`; emit `TtsState.Error`.
- [x] **Missing `ACCESS_NETWORK_STATE`.** `AndroidManifest.xml:22-27` vs
      `ui/utils/NetworkMonitorImpl.kt:35,43`. Add the permission.
- [x] **OkHttp response leak.** `data/remote/WebDataSourceImpl.kt:59-73`
      Wrap `execute()` in `.use { }` (cf. `ImageDataSourceImpl.kt:46`).
- [x] **`byline` used as image URL.** `data/remote/WebDataSourceImpl.kt:251`
      Remove the author-name fallback in `extractImageUrl`.
- [x] **Release not minified; ProGuard dead.** `app/build.gradle.kts:82-87`
      Enable `isMinifyEnabled`/`isShrinkResources` + keep rules; verify JNI/Room/Hilt/serialization.
- [x] **No release signing / static version.** `app/build.gradle.kts:60-61,80-88`
      Add `signingConfigs`, drive `versionCode`/`versionName` from tag; add release workflow.
- [x] **Migration chain incomplete.** `data/local/AppDatabase.kt:29`, `di/DatabaseModule.kt:39,144`
      Only `16→17`, no migrations <16, no destructive fallback. Add chain + schemas (or document).
- [x] **Migration test assertions are no-ops.** `androidTest/.../DatabaseMigrationTest.kt:62,76`
      Replace Kotlin `assert(...)` with JUnit assertions; run in CI.

## P1 — High impact

### Persistence & data
- [x] **SQLCipher key verified only by `isOpen`.** `di/DatabaseModule.kt:83-96`
      Run a real query (`SELECT count(*) FROM sqlite_schema`) as the test already does.
- [x] **Main-thread DB I/O at startup.** `di/DatabaseModule.kt:70-146`, `service/SyncManager.kt:117-145`,
      called from `MainActivity.kt:71`. Remove `runBlocking`; move to IO/WorkManager.
- [x] **Data layer imports UI DataStore.** `data/settings/DownloadSettingsManager.kt:22`,
      `data/settings/SyncSettingsManager.kt:23` → `ui/theme/ThemeManager.kt:34`. Relocate delegate.
- [x] **Non-atomic progress writes.** `data/repositories/ArticleRepositoryImpl.kt:143-165`,
      `data/local/dao/ArticleDao.kt:77-78`. Add targeted `UPDATE` queries.
- [x] **`addToQueue`/`reorderQueue` read-modify-write + O(n²).** `ArticleRepositoryImpl.kt:95-99,167-196`.
- [x] **Feed refresh N+1 + serial downloads + swallows failures.**
      `data/repositories/FeedRepositoryImpl.kt:76-151`. Batch URLs, propagate failures.
- [x] **Backup/restore is non-portable & closes singleton DB.**
      `data/repositories/ImportExportRepositoryImpl.kt:94-128`, `res/xml/backup_rules.xml:24-27`,
      `res/xml/data_extraction_rules.xml`. Export portable format; don't back up ciphertext.
- [x] **Deprecated `security-crypto` + fragile hex parsing.** `utils/SecurityManager.kt:21-22,108-114`.
- [x] **`TtsModelEntity.valueOf` can throw.** `data/local/entities/TtsModelEntity.kt:48`. Use `runCatching`.
- [x] **Duplicate DAO methods.** `data/local/dao/ArticleDao.kt:94-108` (`markAsFinished`/`markAsPlayed`).
- [x] **`deleteFeed` matches by mutable title.** `FeedRepositoryImpl.kt:61-67` → match by `feedUrl`.
- [x] **Worker retry/backoff semantics.** `data/workers/*`; `ModelRepositoryImpl.kt:149-153` `REPLACE` restarts downloads.
- [x] **`encryptDatabase` non-atomic swap + unescaped path.** `di/DatabaseModule.kt:156-188`.
- [x] **Model progress clamps to 100% early.** `data/repositories/ModelRepositoryImpl.kt:210,219,231`.
- [x] **Orphaned local images / misleading extensions.** `data/local/ImageDataSourceImpl.kt:40-75`,
      `data/local/EpubDataSourceImpl.kt:68`; add pruning.

### Playback / TTS / service
- [x] **Engine switch breaks Sherpa.** `tts/common/DelegatingTtsEngine.kt:89-115`,
      `tts/ondevice/SherpaTtsEngine.kt:128-145,635`. Don't `release()` on switch-away; fail loudly on null `tts`.
- [x] **Settings that do nothing.** `_pauseForInterruptions` `TtsPlayer.kt:117,250`;
      `pauseOnDisconnect` `playback/PlaybackSettingsManager.kt:98`. Wire up or remove.
- [x] **Audio focus never abandoned on stop.** `playback/TtsPlayer.kt:407-421`.
- [x] **Hardware-button mapping mismatch.** `playback/PlaybackManager.kt:598-618` vs
      `ui/screens/PlaybackSettingsScreen.kt:282-287`. Share constants; handle `restart_article`.
- [x] **Wake lock 10-min timeout never renewed.** `playback/PowerLockManager.kt:30-43`.
- [x] **Sherpa thread-safety.** `samplesPerCharAverage` (`SherpaTtsEngine.kt:80,616`),
      `activeStreams` locking (`:418,553,662`), `tts?.release()` outside lock (`:181-196`).
- [x] **`release()` doesn't cancel scope/reset state.** `SherpaTtsEngine.kt:805-813`.
- [x] **Service reflection loop runs forever every 10s.** `service/PlaybackService.kt:196-204`.
- [x] **`onPlaybackResumption` out-of-range index.** `service/PlaybackService.kt:275-292`.
- [x] **`startSilence`/`stopSilence` AudioTrack race.** `service/PlaybackService.kt:440-471`.
- [x] **Main-thread bitmap compression.** `TtsPlayer.kt:596-629` (`bitmap.compress`).
- [x] **Empty catch blocks in Samsung helper.** `utils/MediaSessionUtils.kt:104-199`.

### UI
- [x] **Composition-phase Toast.** `ui/screens/VoicesSettingsScreen.kt:101-104` → `LaunchedEffect`.
- [ ] **Widget deep link ignored.** `MainActivity.kt:90-96` `onNewIntent`; `ui/widget/NarraWidget.kt:156-161`.
- [x] **Reorder gesture cancels itself.** `ui/screens/QueueScreen.kt:417` → stable key.
- [x] **ViewModel work on `PlaybackManager.scope`.** `ui/viewmodels/HistoryViewModel.kt:86,113`.
- [x] **HTML parse on Main.** `ui/viewmodels/QueueViewModel.kt:164`, `HistoryViewModel.kt:93,106`.
- [ ] **Raw resource IDs as UI labels.** `ui/screens/PlaybackSettingsScreen.kt:402`.
- [ ] **Wrong auto-fullscreen highlight requester + missing search entries.**
      `ui/screens/UserInterfaceSettingsScreen.kt:286-290`, `ui/screens/SettingsSearchData.kt`.
- [ ] **Dead "Search" overflow actions.** `QueueScreen.kt:269-279`, `FeedsScreen.kt:221-231`,
      `InboxScreen.kt:167-177`, `HistoryScreen.kt:175-185`, `FeedArticlesScreen.kt:172-182`.
- [ ] **Stale instrumentation assertions.** `androidTest/.../ui/NavigationTest.kt:49,69` (`"Add Content"`).
- [ ] **Unused `sherpaSpeed` UI state/setter.** `ui/viewmodels/VoicesSettingsViewModel.kt:56,118-122`.
- [ ] **16-flow unchecked combine (and 5 others).** `ui/viewmodels/ReaderViewModel.kt:98-145`,
      `InboxViewModel.kt:67-96`, `HistoryViewModel.kt:62-77`, etc.
- [ ] **Reader parses article twice.** `ui/viewmodels/ReaderViewModel.kt:67-96,151-162,191-194`.
- [ ] **Dead error states.** `ui/viewmodels/QueueUiState.kt:36-38`, `HomeUiState.kt:30-32`.
- [ ] **Theme manager scope/9 collectors.** `ui/theme/ThemeManager.kt:39,70-84`, `ThemeViewModel.kt:50-96`.
- [ ] **`UiText` value equality.** `ui/UiText.kt:32-41`.
- [ ] **Sort-toggle duplication (4×).** `QueueViewModel.kt:114-128`, `FeedsViewModel.kt:74-90`,
      `FeedArticlesViewModel.kt:113-128`, `InboxViewModel.kt:107-122`.
- [ ] **Widget updates only first instance.** `ui/widget/WidgetManager.kt:63`, `WidgetImageWorker.kt:73`.
- [ ] **Widget image cache file collisions/eviction.** `ui/widget/WidgetImageWorker.kt:56,66-68`.

## P2 — Medium

### Compose / perf / a11y
- [ ] Edge-to-edge in `SideEffect` every recomposition. `ui/theme/Theme.kt:63-88`.
- [ ] `lastInteractionTrigger` mutated on every pointer event. `ui/screens/ReaderScreen.kt:265-294`.
- [ ] `LaunchedEffect` keyed on measured value. `ui/screens/reader/ReaderContentList.kt:127`.
- [ ] Migrate `Modifier.composed` flash highlight. `ui/components/ModifierExtensions.kt:32-56`.
- [ ] Missing list keys. `HomeScreen.kt:297`, `FeedsScreen.kt:334`, `FeedArticlesScreen.kt:284`,
      `SettingsScreen.kt:229`, `LicensesScreen.kt:103`, `ReaderScreen.kt:498-515`.
- [ ] Merged semantics swallow child controls. `ui/components/QueueItem.kt:215-224,361-362`,
      `ui/components/MiniPlayer.kt:107-113`.
- [ ] Clickable rows missing `Role` (many files, see audit).
- [ ] Notification toggle has no state semantics. `ui/screens/FeedsScreen.kt:429-436`.
- [ ] Sort state conveyed by color only. `ui/components/SortBottomSheet.kt:111-149`.
- [ ] `IntrinsicSize` in long lists. `VoicesSettingsScreen.kt:472-474`, `QueueItem.kt:388`,
      `ReaderContentList.kt:385`.
- [ ] `NarraScrollbar` constant thumb fraction; hardcoded `contentDescription`.
      `ui/components/NarraScrollbar.kt:234-244,308-310`.
- [ ] Favicon URL logic duplicated 4×. `HomeScreen.kt:337`, `QueueItem.kt:235`,
      `MiniPlayer.kt:130`, `FeedsScreen.kt:385`.
- [ ] Reader error uses `Icons.Default.Refresh` placeholder. `ReaderScreen.kt:553-558`.
- [ ] Wrong dropdown anchor type. `ui/screens/SettingsScreen.kt:123`.
- [ ] Inconsistent reader line-spacing default. `ReaderScreen.kt:158` vs `ThemeManager.kt:54`.
- [ ] `UiText.fromError` wrong message for `WifiRequired`. `ui/UiText.kt:49`.
- [ ] Manual `ThemeManager`/VM in preview can crash. `UserInterfaceSettingsScreen.kt:451-453`.

### Localization
- [ ] Hardcoded nav labels (resources exist). `ui/components/BottomNavBar.kt:43-47`.
- [ ] Hardcoded widget strings. `ui/widget/NarraWidget.kt:131,219`.
- [ ] Hardcoded interval/limit lists. `DownloadsSettingsScreen.kt:326,337`.
- [ ] Hardcoded skip-time labels. `PlaybackSettingsScreen.kt:264,275`.
- [ ] English-only search keywords. `ui/screens/SettingsSearchData.kt:38-258`.
- [ ] Hardcoded separator `" • "` (4 files); About version string. `AboutScreen.kt:67`.
- [ ] Hardcoded notification strings/icon. `utils/NotificationHelper.kt:44-45,55,92-93`.
- [ ] Hardcoded `"Loading..."` / Sherpa error strings compared in code.
      `TtsPlayer.kt:270`, `SherpaTtsEngine.kt:636,645`, `PlaybackManager.kt:139`.

### Duplication / structure
- [ ] Extract `ScreenHeader` + `SettingsSwitchRow`/`SettingsActionRow` (~9 screens).
- [ ] Extract `SortOption.toggled()`.
- [ ] Extract `NetworkConstants`/User-Agent; model catalog constants.
- [ ] Move HTML/TTS parsing out of `ui/` per AGENTS.md. `ui/utils/HtmlToAnnotatedString.kt`,
      `ui/models/ContentBlock.kt`.
- [ ] Convention plugin for Spotless (both modules + root).
- [ ] Remove dead code: `registerNoisyReceiver`, `acquireLocks`, manual wake-lock API,
      `reloadLastArticle`, `deleteStaleKokoroModels`, Opml reflection fallback, `KEY_ARTICLE_SOURCE`.

### Build / CI / deps / licensing
- [ ] **Bump Media3 (1.4.0 → 1.11.0).** Deferred pending a device playback smoke test.
      Unused `media3-exoplayer` already removed.
- [x] Remove unused catalog entries (`androidx-ui`, `paparazzi-gradle-plugin`); drop explicit
      `foundation` version (BOM manages); verify standalone `onnxruntime-android` need.
- [x] Restrict JitPack with `content { includeGroupByRegex("com\\.github\\.k2-fsa.*") }`.
- [x] CI: add `permissions`, `concurrency`, `timeout-minutes`, SHA-pinned actions,
      `assembleDebugAndroidTest`/emulator job, `testReleaseUnitTest`, lint `abortOnError`.
- [x] JDK alignment: CI/Docker/README now JDK 21 to match `gradle-daemon-jvm.properties`.
- [x] Gradle wrapper `distributionSha256Sum`, retries, timeout.
- [x] `gradle.properties`: fix `android.newDsl` comment, remove `org.gradle.tooling.parallel`,
      raise heap, enable parallel/caching (config cache intentionally off for now).
- [x] Add Dependabot config.
- [x] Remove `:benchmark` (was empty; SDK/Java mismatch).
- [x] Licensing: merge (not strip) license notices; fix `LicensesScreen.kt` (Readability4J
      license, add Epublib/Sherpa/ONNX/SQLCipher/OkHttp/etc., document bundled sounds).
- [x] Manifest polish: remove deprecated `package=`, fix `foregroundServiceType` vs
      `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, dedupe `MEDIA_BUTTON`, keep `allowBackup`.

### Docs
- [x] Add `docs/CONTRIBUTING.md` (fixes broken PR-template link) + `SECURITY.md`/`ROADMAP.md`.
- [x] Fix `docs/TTS_ENGINES.md` (sherpa AAR vs JitPack), `docs/ARCHITECTURE.md` stray line,
      `docs/PRIVACY.md` unshipped cloud-provider claims, testing-guide CI accuracy.

### Tests
- [ ] Unit tests: `TtsPlayer`, `PlaybackManager`, `AndroidTtsEngine`, `DelegatingTtsEngine`,
      `ArticleRepositoryImpl`, `FeedRepositoryImpl`, `ImportExportRepositoryImpl`, workers.
- [ ] Migrate `SherpaTtsEngineTest` off its copied re-implementation; expose internal seam.
- [ ] ViewModel tests for Reader, Queue, Feeds, FeedArticles, Inbox, History, PlaybackSettings,
      VoicesSettings, Theme.
- [ ] Screenshot coverage for more screens/states (light/dark, loading/error/empty).
- [ ] Add code-coverage reporting (JaCoCo/Kover) if coverage goals are to be enforced.

## Suggested execution order

1. P0 correctness + migration-test asserts (isolated patches + tests).
2. Persistence/security (migrations, key verification, offline init, portable backup, atomic writes).
3. Playback/TTS correctness and thread-safety.
4. Build/CI/release + dependency/licensing.
5. UI/a11y/localization + test expansion.

## Notes / decisions needed
- Backup strategy: portable logical export vs user-passphrase-derived key (affects Phase 2).
- Whether to keep SDK 36 / toolchain 21 or align everything to 17.
- Whether `:benchmark` should be implemented or removed.
- Priority of localization vs other P2 work.
