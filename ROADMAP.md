# Roadmap

This is a living document, not a commitment. Priorities may change based on user feedback and
contributor availability. It is authoritative for what is in and out of scope; see
[AGENTS.md](AGENTS.md) for how AI agents and contributors should operate within that scope.

## MVP (current target)

- RSS feed subscription and article fetching
- EPUB import and parsing
- Web page import with reader-mode extraction
- Podcast-style listening queue/playlist UI
- Playback via Android's built-in TTS engine
- On-device AI TTS using Sherpa-ONNX (models downloaded at runtime, not bundled)
- OPML import/export
- File-based backup and restore (no account required)
- Foreground media service with system media controls
- Glance-based home screen widget

## Distribution

### F-Droid (next)

The goal is inclusion in the official F-Droid main repository. F-Droid builds every app from
source on its own build server and only accepts FLOSS dependencies and assets, so the work below
comes before any submission. See the
[inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) and the
[submission guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

Repository changes:

- [x] Replace the Google favicon fallback (`UrlUtils.faviconUrl` and its call sites in Home, Feeds,
      Queue, and MiniPlayer). Sending feed and article URLs to a third-party favicon service is a
      privacy leak and a likely `Tracking` anti-feature; use the site's own favicon or a locally
      generated letter avatar instead.
- [x] Send the source origin (or no `Referer`) instead of `https://www.google.com/` in
      `WebDataSourceImpl`.
- [x] Move the version into a committed `version.properties` (`versionCode` and `versionName`) and
      read it from `app/build.gradle.kts`, so F-Droid can extract it from tags with
      `UpdateCheckData`. Validate the file against the tag in `.github/workflows/release.yml` and
      update `docs/RELEASING.md`.
- [ ] Replace the Chequered Ink chime sounds in `res/raw` with CC0/public-domain audio, or confirm
      their license permits redistribution inside the app.
- [ ] Ship the non-code asset licenses: SIL OFL 1.1 for OpenDyslexic 3 and LGPL for
      `epublib-core` (the jar contains no license file), surfaced from the in-app licenses screen.
- [ ] Add store metadata under `fastlane/metadata/android/en-US/`: title, short and full
      descriptions, a 512×512 icon, phone screenshots, and a changelog per version code.
- [ ] Tune the release build for F-Droid's build VMs: lower the Gradle heap, and decide whether
      `lint.checkReleaseBuilds` stays enabled for release assembly.
- [ ] Tag a release containing the changes above.

Submission:

- [ ] Write the `fdroiddata` recipe (`metadata/com.mienaiknife.narra.yml`) and validate it in the
      official buildserver container with `fdroid readmeta`, `fdroid rewritemeta`, `fdroid lint`,
      and `fdroid build`.
- [ ] Open a `New App` merge request against `fdroiddata`.
- [ ] Answer reviewer questions and keep the recipe current: JitPack prebuilt AAR for Sherpa-ONNX,
      Maven Central SQLCipher, LGPL Epublib, and runtime model/lexicon downloads with explicit user
      consent.
- [ ] After inclusion, add the F-Droid link to `README.md` and document the signing-key migration
      for users coming from GitHub release APKs.

Open decisions:

- **Signing:** use F-Droid's key (simple, but existing users must back up, uninstall, and reinstall)
  or pursue reproducible builds with `Binaries`/`AllowedAPKSigningKeys` (keeps the upstream key,
  significantly more work).
- **ABI split:** ship one ~70 MB universal APK or add per-ABI flavors so F-Droid serves smaller
  arm64-v8a and armeabi-v7a builds.

## Planned

- PDF file importing and parsing
- Self-hosted AI TTS server support (for example, Kokoro, Coqui, or Piper via a local API)
- Additional cloud AI TTS providers
- Builds for other platforms (desktop and iOS, via Kotlin Multiplatform or a separate app)
- Optional sync via a self-hosted, user-controlled server (for example, a
  Nextcloud/gpodder-compatible API) — no first-party accounts
- Automatic readability/reader-mode heuristic improvements
- Importing articles by scanning photos
- User-customizable color themes
- Support for more languages

## Non-goals

- Requiring a first-party account or any server operated by this project
- Bundling large AI TTS model files in the APK
- Proprietary or license-incompatible dependencies
