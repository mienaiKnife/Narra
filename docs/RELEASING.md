# Releasing Narra

## Versioning

Releases are driven by Git tags of the form `vMAJOR.MINOR.PATCH` (for example `v0.2.0`).

- `versionName` is the tag without the `v` prefix (`0.2.0`).
- `versionCode` is derived from the tag as `MAJOR * 10000 + MINOR * 100 + PATCH` (`200`).

Locally, when no version is provided, the build falls back to `versionCode = 1` and
`versionName = "0.1"`.

## Signing

Release signing is optional. If no keystore is configured, `assembleRelease` still produces a
minified but **unsigned** `app-release-unsigned.apk`, which is what normal CI builds do.

Signing credentials are read from environment variables or `local.properties` (which is
git-ignored), in that order:

| Key | Description |
| --- | --- |
| `RELEASE_KEYSTORE_FILE` | Absolute or relative path to the `.jks`/`.keystore` file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `VERSION_NAME` | Overrides `versionName` |
| `VERSION_CODE` | Overrides `versionCode` |

To sign locally, add the keys to `local.properties` and run `./gradlew assembleRelease`. The
resulting APK is `app/build/outputs/apk/release/app-release.apk`.

## Cutting a release

1. Make sure `main` is green.
2. Create and push a tag: `git tag v0.2.0 && git push origin v0.2.0`.
3. The `Release` workflow (`.github/workflows/release.yml`) decodes the keystore, builds the
   signed APK, and publishes it to the GitHub release for the tag.

The workflow requires these repository secrets:

- `RELEASE_KEYSTORE_BASE64` – the keystore file, base64 encoded
  (`base64 -w0 release.jks`).
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## Minification

Release builds enable R8 (`isMinifyEnabled = true`) and resource shrinking
(`isShrinkResources = true`). Keep rules live in `app/proguard-rules.pro`. Reflection-based code
paths (Sherpa-ONNX JNI and its Kotlin callback, plus the OPML kxml/xmlpull fallback) have explicit
keeps.

Because R8 issues often only surface at runtime, smoke-test a release build on a device before
publishing if you touched reflection, serialization, or JNI code.

## Database migrations

Room schemas below `AppDatabase.MIN_SUPPORTED_VERSION` (16) predate schema export and are recreated
destructively. Any schema at or above that version needs a real `Migration` and a
`DatabaseMigrationTest` case. See [ARCHITECTURE.md](ARCHITECTURE.md#database-migrations).
